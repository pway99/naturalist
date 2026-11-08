package com.naturalist.plants;

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
 * RDBMS adapter for the {@link PlantFamily} rank record. The upward reference to {@link PlantOrder}
 * is stored as {@code order_id}, resolved from the order name by the mapper's nested-select on write
 * and recovered by JOIN on read; a family whose order is absent inserts zero rows, which the adapter
 * turns into a loud {@link EntityNotFoundException}. Reads assemble the parent with its
 * {@code commonNames} loaded in one batched query keyed on the family-name set — no N+1.
 *
 * <p>Each write spans several tables, so it runs through {@link PlantFamilyTransaction} as one
 * transaction.
 */
@DomainService
class PlantFamilyEntityRepositoryRdbms
        extends AbstractEntityRepository<PlantFamilyName, PlantFamily>
        implements PlantRepository.FamilyRepository {

    private final PlantFamilyMapper mapper;
    private final PlantFamilyTransaction transaction;

    PlantFamilyEntityRepositoryRdbms(PlantFamilyMapper mapper, PlantFamilyTransaction transaction) {
        this.mapper = mapper;
        this.transaction = transaction;
    }

    @Override
    protected Optional<PlantFamily> doGetByName(PlantFamilyName name) {
        PlantFamilyDbo parent = mapper.selectByName(name.value());
        if (parent == null) return Optional.empty();
        return Optional.of(assemble(List.of(parent), Set.of(name.value())).get(0));
    }

    @Override
    protected List<PlantFamily> doGetByNameSet(Set<PlantFamilyName> nameSet) {
        if (nameSet.isEmpty()) return List.of();
        Set<String> slugs = nameSet.stream().map(PlantFamilyName::value).collect(Collectors.toSet());
        return assemble(mapper.selectByNameSet(slugs), slugs);
    }

    @Override
    protected Page<PlantFamily> doGetPage(PageRequest request) {
        int pageSize = request.pageSize();
        List<PlantFamilyDbo> rows = mapper.selectPage(pageSize + 1, request.offset());
        boolean immediateMore = rows.size() > pageSize;
        List<PlantFamilyDbo> parents = rows.stream().limit(pageSize).toList();
        Set<String> names = parents.stream().map(p -> p.name).collect(Collectors.toSet());
        List<PlantFamily> content = assemble(parents, names);

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
    protected void doInsert(PlantFamily entity) {
        transaction.insert(entity);
    }

    @Override
    protected void doUpdate(PlantFamily entity) {
        transaction.update(entity);
    }

    @Override
    protected PlantFamily doSave(PlantFamily entity) {
        transaction.save(entity);
        return entity;
    }

    @Override
    public List<PlantFamily> getByOrderName(PlantOrderName orderName) {
        MethodObserver getByOrderName = observer().forMethod("getByOrderName");
        getByOrderName.arguments(i -> i.entityName(orderName, "orderName"))
                .throwWhenInvalid();
        List<PlantFamilyDbo> parents = mapper.selectByOrderName(orderName.value());
        Set<String> names = parents.stream().map(p -> p.name).collect(Collectors.toSet());
        var result = assemble(parents, names);
        getByOrderName.state(s -> s.observableCollection(result, "result")).observe(Level.WARN);
        return result;
    }

    /** Assemble records from parent rows + one batched common-name load. */
    private List<PlantFamily> assemble(List<PlantFamilyDbo> parents, Set<String> names) {
        if (parents.isEmpty()) return List.of();
        var commonNames = mapper.selectCommonNames(names).stream()
                .collect(Collectors.groupingBy(cn -> cn.familyName,
                        Collectors.mapping(PlantFamilyCommonNameDbo::toCommonName, Collectors.toSet())));
        return parents.stream()
                .map(p -> p.toEntity(commonNames.getOrDefault(p.name, Set.of())))
                .toList();
    }
}
