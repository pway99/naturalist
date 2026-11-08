package com.naturalist.chemistry.compound;

import com.naturalist.chemistry.compound.role.FunctionalRole;
import com.naturalist.chemistry.element.PeriodicElement;
import com.naturalist.exception.EntityNotFoundException;
import com.naturalist.exception.PrimaryKeyConstraintException;
import com.naturalist.infrastructure.DomainService;
import com.naturalist.persistence.RdbmsExceptions;
import jakarta.transaction.Transactional;

import java.util.Map;

/**
 * The multi-statement writes of {@link CompoundEntityRepositoryRdbms}, each run as one transaction:
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
class CompoundTransaction {

    private final CompoundMapper mapper;

    CompoundTransaction(CompoundMapper mapper) {
        this.mapper = mapper;
    }

    /** The parent row, then every child row. */
    @Transactional
    void insert(Compound entity) {
        CompoundDbo dbo = CompoundDbo.from(entity);   // validates the parent scalars + throws
        try {
            mapper.insertCompound(dbo);
        } catch (RuntimeException e) {
            if (RdbmsExceptions.isUniqueViolation(e)) throw new PrimaryKeyConstraintException(entity);
            throw e;
        }
        insertChildren(entity);
    }

    /** The parent row, then its child rows replaced wholesale. */
    @Transactional
    void update(Compound entity) {
        CompoundDbo dbo = CompoundDbo.from(entity);
        if (mapper.updateCompound(dbo) == 0) throw new EntityNotFoundException(entity);
        String name = entity.name().value();
        mapper.deleteFunctionalRoles(name);
        mapper.deleteConstituentElements(name);
        mapper.deleteProperties(name);
        insertChildren(entity);
    }

    /** The existence check and the write it chooses, in one transaction. */
    @Transactional
    void save(Compound entity) {
        if (mapper.selectByName(entity.name().value()) != null) update(entity);
        else insert(entity);
    }

    private void insertChildren(Compound c) {
        CompoundName name = c.name();
        for (FunctionalRole role : c.compoundInfo().functionalRoles()) {
            mapper.insertFunctionalRole(CompoundFunctionalRoleDbo.from(name, role));
        }
        for (PeriodicElement element : c.compoundInfo().constituentElements()) {
            mapper.insertConstituentElement(CompoundConstituentElementDbo.from(name, element));
        }
        for (Map.Entry<String, String> property : c.properties().entrySet()) {
            mapper.insertProperty(CompoundPropertyDbo.from(name, property.getKey(), property.getValue()));
        }
    }
}
