package com.naturalist.plants;

import com.naturalist.data.AbstractEntityRepository;
import com.naturalist.data.Page;
import com.naturalist.data.PageRequest;
import com.naturalist.infrastructure.DomainService;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * RDBMS adapter for the {@link PlantOrder} rank record — a {@code NamedEntity} at the top of the
 * plant Linnaean chain, so it has no upward FK. Reads assemble the parent row with its
 * {@code commonNames} loaded in one batched query keyed on the order-name set, so a page of orders
 * costs two selects, not two-per-order — no N+1. Writes persist the parent then its common-name
 * children; an update replaces the child rows wholesale.
 *
 * <p>Each write spans several tables, so it runs through {@link PlantOrderTransaction} as one
 * transaction.
 */
@DomainService
class PlantOrderEntityRepositoryRdbms
        extends AbstractEntityRepository<PlantOrderName, PlantOrder>
        implements PlantRepository.OrderRepository {

    private final PlantOrderMapper mapper;
    private final PlantOrderTransaction transaction;

    PlantOrderEntityRepositoryRdbms(PlantOrderMapper mapper, PlantOrderTransaction transaction) {
        this.mapper = mapper;
        this.transaction = transaction;
    }

    @Override
    protected Optional<PlantOrder> doGetByName(PlantOrderName name) {
        PlantOrderDbo parent = mapper.selectByName(name.value());
        if (parent == null) return Optional.empty();
        return Optional.of(assemble(List.of(parent), Set.of(name.value())).get(0));
    }

    @Override
    protected List<PlantOrder> doGetByNameSet(Set<PlantOrderName> nameSet) {
        if (nameSet.isEmpty()) return List.of();
        Set<String> slugs = nameSet.stream().map(PlantOrderName::value).collect(Collectors.toSet());
        return assemble(mapper.selectByNameSet(slugs), slugs);
    }

    @Override
    protected Page<PlantOrder> doGetPage(PageRequest request) {
        int pageSize = request.pageSize();
        List<PlantOrderDbo> rows = mapper.selectPage(pageSize + 1, request.offset());
        boolean immediateMore = rows.size() > pageSize;
        List<PlantOrderDbo> parents = rows.stream().limit(pageSize).toList();
        Set<String> names = parents.stream().map(p -> p.name).collect(Collectors.toSet());
        List<PlantOrder> content = assemble(parents, names);

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
    protected void doInsert(PlantOrder entity) {
        transaction.insert(entity);
    }

    @Override
    protected void doUpdate(PlantOrder entity) {
        transaction.update(entity);
    }

    @Override
    protected PlantOrder doSave(PlantOrder entity) {
        transaction.save(entity);
        return entity;
    }

    /** Assemble records from parent rows + one batched common-name load. */
    private List<PlantOrder> assemble(List<PlantOrderDbo> parents, Set<String> names) {
        if (parents.isEmpty()) return List.of();
        var commonNames = mapper.selectCommonNames(names).stream()
                .collect(Collectors.groupingBy(cn -> cn.orderName,
                        Collectors.mapping(PlantOrderCommonNameDbo::toCommonName, Collectors.toSet())));
        return parents.stream()
                .map(p -> p.toEntity(commonNames.getOrDefault(p.name, Set.of())))
                .toList();
    }
}
