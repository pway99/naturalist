package com.naturalist.chemistry.product;

import com.naturalist.chemistry.compound.CompoundName;
import com.naturalist.exception.EntityNotFoundException;
import com.naturalist.exception.PrimaryKeyConstraintException;
import com.naturalist.infrastructure.DomainService;
import com.naturalist.persistence.RdbmsExceptions;
import jakarta.transaction.Transactional;

import java.util.Map;

/**
 * The multi-statement writes of {@link ProductEntityRepositoryRdbms}, each run as one transaction:
 * the parent row and its child rows commit or roll back together, so a failed child insert leaves
 * no partial row behind and a failed update does not leave the children it deleted gone.
 *
 * <p>A separate bean, not annotations on the adapter: the adapter's write entry points are the
 * {@code final} {@code insert} / {@code update} / {@code save} of
 * {@link com.naturalist.data.AbstractEntityRepository}, which CGLIB cannot override, and their
 * calls to {@code doInsert} et al. are self-invocations no proxy sees. Nothing here is
 * {@code final}, for the same reason. Inside a caller's transaction these join it. Constructed
 * by hand (the ITs and the seeder) no proxy exists and the caller's own {@code SqlSession} is
 * the transaction.
 */
@DomainService
class ProductTransaction {

    private final ProductMapper mapper;

    ProductTransaction(ProductMapper mapper) {
        this.mapper = mapper;
    }

    /** The parent row, then every child row. */
    @Transactional
    void insert(Product entity) {
        ProductDbo dbo = ProductDbo.from(entity);   // validates + throws
        try {
            mapper.insertProduct(dbo);
        } catch (RuntimeException e) {
            if (RdbmsExceptions.isUniqueViolation(e)) throw new PrimaryKeyConstraintException(entity);
            throw e;
        }
        insertChildren(entity);
    }

    /** The parent row, then its child rows replaced wholesale. */
    @Transactional
    void update(Product entity) {
        ProductDbo dbo = ProductDbo.from(entity);
        if (mapper.updateProduct(dbo) == 0) throw new EntityNotFoundException(entity);
        String name = entity.name().value();
        mapper.deleteProductCompounds(name);
        mapper.deleteProductProperties(name);
        insertChildren(entity);
    }

    /** The existence check and the write it chooses, in one transaction. */
    @Transactional
    void save(Product entity) {
        if (mapper.selectByName(entity.name().value()) != null) update(entity);
        else insert(entity);
    }

    private void insertChildren(Product entity) {
        ProductName name = entity.name();
        for (CompoundName compound : entity.compounds()) {
            if (mapper.insertProductCompound(ProductCompoundDbo.from(name, compound)) == 0) {
                throw new EntityNotFoundException(entity);   // referenced compound absent
            }
        }
        for (Map.Entry<String, String> property : entity.properties().entrySet()) {
            mapper.insertProductProperty(ProductPropertyDbo.from(name, property.getKey(), property.getValue()));
        }
    }
}
