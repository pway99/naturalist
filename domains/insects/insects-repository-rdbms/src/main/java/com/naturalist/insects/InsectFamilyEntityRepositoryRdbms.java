package com.naturalist.insects;

import com.naturalist.data.AbstractEntityRepository;
import com.naturalist.data.Page;
import com.naturalist.data.PageRequest;
import com.naturalist.exception.EntityNotFoundException;
import com.naturalist.exception.PrimaryKeyConstraintException;
import com.naturalist.fieldnotes.CommonName;
import com.naturalist.infrastructure.DomainService;
import com.naturalist.observability.Observer;
import com.naturalist.persistence.RdbmsExceptions;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * RDBMS adapter for the {@link InsectFamily} rank record. The upward reference to {@link InsectOrder}
 * is stored as {@code order_id}, resolved from the order name by the mapper's nested-select on write
 * and recovered by JOIN on read; a family whose order is absent inserts zero rows, which the adapter
 * turns into a loud {@link EntityNotFoundException}. Reads assemble the parent with its
 * {@code commonNames} loaded in one batched query keyed on the family-name set — no N+1. The
 * {@link #getByOrderName} / {@link #getByOrderNames} queries each cost two selects regardless of the
 * order-set size.
 */
@DomainService
class InsectFamilyEntityRepositoryRdbms
        extends AbstractEntityRepository<InsectFamilyName, InsectFamily>
        implements InsectRepository.FamilyRepository {

    private final InsectFamilyMapper mapper;

    InsectFamilyEntityRepositoryRdbms(InsectFamilyMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    protected Optional<InsectFamily> doGetByName(InsectFamilyName name) {
        InsectFamilyDbo parent = mapper.selectByName(name.value());
        if (parent == null) return Optional.empty();
        return Optional.of(assemble(List.of(parent), Set.of(name.value())).get(0));
    }

    @Override
    protected List<InsectFamily> doGetByNameSet(Set<InsectFamilyName> nameSet) {
        if (nameSet.isEmpty()) return List.of();
        Set<String> slugs = nameSet.stream().map(InsectFamilyName::value).collect(Collectors.toSet());
        return assemble(mapper.selectByNameSet(slugs), slugs);
    }

    @Override
    protected Page<InsectFamily> doGetPage(PageRequest request) {
        int pageSize = request.pageSize();
        List<InsectFamilyDbo> rows = mapper.selectPage(pageSize + 1, request.offset());
        boolean immediateMore = rows.size() > pageSize;
        List<InsectFamilyDbo> parents = rows.stream().limit(pageSize).toList();
        Set<String> names = parents.stream().map(p -> p.name).collect(Collectors.toSet());
        List<InsectFamily> content = assemble(parents, names);

        int pagesAheadKnown = 0;
        boolean moreBeyondLookahead = immediateMore;
        if (request.lookahead() > 0 && immediateMore) {
            int window = request.lookahead() * pageSize + 1;
            int beyond = mapper.countInWindow(request.offset() + pageSize, window);
            pagesAheadKnown = Math.min(request.lookahead(), (beyond + pageSize - 1) / pageSize);
            moreBeyondLookahead = beyond > request.lookahead() * pageSize;
        } else if (request.lookahead() > 0) {
            moreBeyondLookahead = false;
        }
        return new Page<>(content, request.pageNumber(), pageSize, pagesAheadKnown, moreBeyondLookahead);
    }

    @Override
    protected void doInsert(InsectFamily entity) {
        InsectFamilyDbo dbo = InsectFamilyDbo.from(entity);   // validates the parent scalars + throws
        int inserted;
        try {
            inserted = mapper.insert(dbo);
        } catch (RuntimeException e) {
            if (RdbmsExceptions.isUniqueViolation(e)) throw new PrimaryKeyConstraintException(entity);
            throw e;
        }
        if (inserted == 0) throw new EntityNotFoundException(entity);   // referenced order absent
        insertChildren(entity);
    }

    @Override
    protected void doUpdate(InsectFamily entity) {
        InsectFamilyDbo dbo = InsectFamilyDbo.from(entity);
        if (mapper.updateByName(dbo) == 0) throw new EntityNotFoundException(entity);
        mapper.deleteCommonNames(entity.name().value());
        insertChildren(entity);
    }

    @Override
    protected InsectFamily doSave(InsectFamily entity) {
        if (mapper.selectByName(entity.name().value()) != null) doUpdate(entity);
        else doInsert(entity);
        return entity;
    }

    @Override
    public List<InsectFamily> getByOrderName(InsectOrderName orderName) {
        Observer.forClass(InsectFamilyEntityRepositoryRdbms.class)
                .arguments("getByOrderName", i -> i.entityName(orderName, "orderName"))
                .throwWhenInvalid();
        List<InsectFamilyDbo> parents = mapper.selectByOrderName(orderName.value());
        Set<String> names = parents.stream().map(p -> p.name).collect(Collectors.toSet());
        return assemble(parents, names);
    }

    @Override
    public List<InsectFamily> getByOrderNames(Set<InsectOrderName> orderNames) {
        Observer.forClass(InsectFamilyEntityRepositoryRdbms.class)
                .arguments("getByOrderNames", i -> i.entityNameCollection(orderNames, "orderNames"))
                .throwWhenInvalid();
        if (orderNames.isEmpty()) return List.of();
        Set<String> orderSlugs = orderNames.stream().map(InsectOrderName::value).collect(Collectors.toSet());
        List<InsectFamilyDbo> parents = mapper.selectByOrderNameSet(orderSlugs);
        Set<String> names = parents.stream().map(p -> p.name).collect(Collectors.toSet());
        return assemble(parents, names);
    }

    private void insertChildren(InsectFamily f) {
        InsectFamilyName name = f.name();
        for (CommonName commonName : f.commonNames()) {
            mapper.insertCommonName(InsectFamilyCommonNameDbo.from(name, commonName));
        }
    }

    /** Assemble records from parent rows + one batched common-name load. */
    private List<InsectFamily> assemble(List<InsectFamilyDbo> parents, Set<String> names) {
        if (parents.isEmpty()) return List.of();
        var commonNames = mapper.selectCommonNames(names).stream()
                .collect(Collectors.groupingBy(cn -> cn.familyName,
                        Collectors.mapping(InsectFamilyCommonNameDbo::toCommonName, Collectors.toSet())));
        return parents.stream()
                .map(p -> p.toEntity(commonNames.getOrDefault(p.name, Set.of())))
                .toList();
    }
}
