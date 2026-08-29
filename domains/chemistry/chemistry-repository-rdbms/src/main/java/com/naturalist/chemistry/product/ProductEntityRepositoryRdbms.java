package com.naturalist.chemistry.product;

import com.naturalist.chemistry.compound.CompoundName;
import com.naturalist.data.AbstractEntityRepository;
import com.naturalist.data.Page;
import com.naturalist.data.PageRequest;
import com.naturalist.exception.EntityNotFoundException;
import com.naturalist.exception.PrimaryKeyConstraintException;
import com.naturalist.infrastructure.DomainService;
import com.naturalist.observability.Observer;
import com.naturalist.persistence.RdbmsExceptions;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * RDBMS adapter for the {@link Product} aggregate. Reads assemble the parent with its formulation
 * ({@code product_compound}) and properties loaded one batched query each; writes persist the
 * parent then the children (child inserts nested-select the numeric ids from names). The reverse
 * {@link #getByCompoundName} resolves the product names for a compound, then assembles them.
 */
@DomainService
class ProductEntityRepositoryRdbms
        extends AbstractEntityRepository<ProductName, Product>
        implements ProductRepository {

    private final ProductMapper mapper;

    ProductEntityRepositoryRdbms(ProductMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    protected Optional<Product> doGetByName(ProductName name) {
        ProductDbo parent = mapper.selectByName(name.value());
        if (parent == null) return Optional.empty();
        return Optional.of(assemble(List.of(parent), Set.of(name.value())).get(0));
    }

    @Override
    protected List<Product> doGetByNameSet(Set<ProductName> nameSet) {
        if (nameSet.isEmpty()) return List.of();
        Set<String> slugs = nameSet.stream().map(ProductName::value).collect(Collectors.toSet());
        return assemble(mapper.selectByNameSet(slugs), slugs);
    }

    @Override
    protected Page<Product> doGetPage(PageRequest request) {
        int pageSize = request.pageSize();
        List<ProductDbo> rows = mapper.selectPage(pageSize + 1, request.offset());
        boolean immediateMore = rows.size() > pageSize;
        List<ProductDbo> parents = rows.stream().limit(pageSize).toList();
        Set<String> names = parents.stream().map(p -> p.name).collect(Collectors.toSet());
        List<Product> content = assemble(parents, names);

        int pagesAheadKnown = 0;
        boolean moreBeyondLookahead = immediateMore;
        if (request.lookahead() > 0 && immediateMore) {
            int window = request.lookahead() * pageSize + 1;
            int beyond = mapper.countInWindow(request.offset() + pageSize, window);
            pagesAheadKnown = Math.min(request.lookahead(), beyond / pageSize);
            moreBeyondLookahead = beyond > request.lookahead() * pageSize;
        } else if (request.lookahead() > 0) {
            moreBeyondLookahead = false;
        }
        return new Page<>(content, request.pageNumber(), pageSize, pagesAheadKnown, moreBeyondLookahead);
    }

    @Override
    protected void doInsert(Product entity) {
        ProductDbo dbo = ProductDbo.from(entity);   // validates + throws
        try {
            mapper.insertProduct(dbo);
        } catch (RuntimeException e) {
            if (RdbmsExceptions.isUniqueViolation(e)) throw new PrimaryKeyConstraintException(entity);
            throw e;
        }
        insertChildren(entity);
    }

    @Override
    protected void doUpdate(Product entity) {
        ProductDbo dbo = ProductDbo.from(entity);
        if (mapper.updateProduct(dbo) == 0) throw new EntityNotFoundException(entity);
        String name = entity.name().value();
        mapper.deleteProductCompounds(name);
        mapper.deleteProductProperties(name);
        insertChildren(entity);
    }

    @Override
    protected Product doSave(Product entity) {
        if (mapper.selectByName(entity.name().value()) != null) doUpdate(entity);
        else doInsert(entity);
        return entity;
    }

    @Override
    public List<Product> getByCompoundName(CompoundName compoundName) {
        Observer.forClass(ProductEntityRepositoryRdbms.class)
                .arguments("getByCompoundName", i -> i.identifier(compoundName, "compoundName"))
                .throwWhenInvalid();
        List<String> productNames = mapper.selectProductNamesByCompoundName(compoundName.value());
        if (productNames.isEmpty()) return List.of();
        return assemble(mapper.selectByNameSet(productNames), Set.copyOf(productNames));
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

    private List<Product> assemble(List<ProductDbo> parents, Set<String> names) {
        if (parents.isEmpty()) return List.of();
        Map<String, Set<CompoundName>> compounds = mapper.selectProductCompounds(names).stream()
                .collect(Collectors.groupingBy(pc -> pc.productName,
                        Collectors.mapping(ProductCompoundDbo::toCompoundName, Collectors.toSet())));
        Map<String, Map<String, String>> properties = mapper.selectProductProperties(names).stream()
                .collect(Collectors.groupingBy(pp -> pp.productName,
                        Collectors.toMap(pp -> pp.propertyKey, pp -> pp.propertyValue)));
        return parents.stream()
                .map(p -> p.toEntity(
                        compounds.getOrDefault(p.name, Set.of()),
                        properties.getOrDefault(p.name, Map.of())))
                .toList();
    }
}
