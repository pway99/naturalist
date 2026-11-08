package com.naturalist.insects;

import com.naturalist.data.AbstractEntityRepository;
import com.naturalist.data.Page;
import com.naturalist.data.PageRequest;
import com.naturalist.exception.EntityNotFoundException;
import com.naturalist.infrastructure.DomainService;
import com.naturalist.observability.Level;
import com.naturalist.observability.MethodObserver;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * RDBMS adapter for the {@link InsectFamilyInfo} rank record. The upward reference to {@link InsectOrderInfo}
 * is stored as {@code order_id}, resolved from the order name by the mapper's nested-select on write
 * and recovered by JOIN on read; a family whose order is absent inserts zero rows, which the adapter
 * turns into a loud {@link EntityNotFoundException}. Reads assemble the parent with its
 * {@code commonNames} loaded in one batched query keyed on the family-name set — no N+1. The
 * {@link #getByOrderName} / {@link #getByOrderNames} queries each cost two selects regardless of the
 * order-set size.
 *
 * <p>Each write spans several tables, so it runs through {@link InsectFamilyTransaction} as one
 * transaction.
 */
@DomainService
class InsectFamilyEntityRepositoryRdbms
        extends AbstractEntityRepository<InsectFamilyName, InsectFamilyInfo>
        implements InsectRepository.FamilyRepository {

    private final InsectFamilyMapper mapper;
    private final InsectFamilyTransaction transaction;

    InsectFamilyEntityRepositoryRdbms(InsectFamilyMapper mapper,
            InsectFamilyTransaction transaction) {
        this.mapper = mapper;
        this.transaction = transaction;
    }

    @Override
    protected Optional<InsectFamilyInfo> doGetByName(InsectFamilyName name) {
        InsectFamilyDbo parent = mapper.selectByName(name.value());
        if (parent == null) return Optional.empty();
        return Optional.of(assemble(List.of(parent), Set.of(name.value())).get(0));
    }

    @Override
    protected List<InsectFamilyInfo> doGetByNameSet(Set<InsectFamilyName> nameSet) {
        if (nameSet.isEmpty()) return List.of();
        Set<String> slugs = nameSet.stream().map(InsectFamilyName::value).collect(Collectors.toSet());
        return assemble(mapper.selectByNameSet(slugs), slugs);
    }

    @Override
    protected Page<InsectFamilyInfo> doGetPage(PageRequest request) {
        int pageSize = request.pageSize();
        List<InsectFamilyDbo> rows = mapper.selectPage(pageSize + 1, request.offset());
        boolean immediateMore = rows.size() > pageSize;
        List<InsectFamilyDbo> parents = rows.stream().limit(pageSize).toList();
        Set<String> names = parents.stream().map(p -> p.name).collect(Collectors.toSet());
        List<InsectFamilyInfo> content = assemble(parents, names);

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
    protected void doInsert(InsectFamilyInfo entity) {
        transaction.insert(entity);
    }

    @Override
    protected void doUpdate(InsectFamilyInfo entity) {
        transaction.update(entity);
    }

    @Override
    protected InsectFamilyInfo doSave(InsectFamilyInfo entity) {
        transaction.save(entity);
        return entity;
    }

    @Override
    public List<InsectFamilyInfo> getByOrderName(InsectOrderName orderName) {
        MethodObserver getByOrderName = observer().forMethod("getByOrderName");
        getByOrderName.arguments(i -> i.entityName(orderName, "orderName"))
                .throwWhenInvalid();
        List<InsectFamilyDbo> parents = mapper.selectByOrderName(orderName.value());
        Set<String> names = parents.stream().map(p -> p.name).collect(Collectors.toSet());
        var result = assemble(parents, names);
        getByOrderName.state(s -> s.observableCollection(result, "result")).observe(Level.WARN);
        return result;
    }

    @Override
    public List<InsectFamilyInfo> getByOrderNames(Set<InsectOrderName> orderNames) {
        MethodObserver getByOrderNames = observer().forMethod("getByOrderNames");
        getByOrderNames.arguments(i -> i.entityNameCollection(orderNames, "orderNames"))
                .throwWhenInvalid();
        if (orderNames.isEmpty()) return List.of();
        Set<String> orderSlugs = orderNames.stream().map(InsectOrderName::value).collect(Collectors.toSet());
        List<InsectFamilyDbo> parents = mapper.selectByOrderNameSet(orderSlugs);
        Set<String> names = parents.stream().map(p -> p.name).collect(Collectors.toSet());
        var result = assemble(parents, names);
        getByOrderNames.state(s -> s.observableCollection(result, "result")).observe(Level.WARN);
        return result;
    }

    /** Assemble records from parent rows + one batched common-name load. */
    private List<InsectFamilyInfo> assemble(List<InsectFamilyDbo> parents, Set<String> names) {
        if (parents.isEmpty()) return List.of();
        var commonNames = mapper.selectCommonNames(names).stream()
                .collect(Collectors.groupingBy(cn -> cn.familyName,
                        Collectors.mapping(InsectFamilyCommonNameDbo::toCommonName, Collectors.toSet())));
        return parents.stream()
                .map(p -> p.toEntity(commonNames.getOrDefault(p.name, Set.of())))
                .toList();
    }
}
