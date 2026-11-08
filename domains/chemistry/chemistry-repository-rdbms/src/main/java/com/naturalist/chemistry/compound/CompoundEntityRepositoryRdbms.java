package com.naturalist.chemistry.compound;

import com.naturalist.chemistry.compound.role.FunctionalRole;
import com.naturalist.chemistry.element.PeriodicElement;
import com.naturalist.data.AbstractEntityRepository;
import com.naturalist.data.Page;
import com.naturalist.data.PageRequest;
import com.naturalist.infrastructure.DomainService;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * RDBMS adapter for the {@link Compound} aggregate. Reads assemble the parent row with its three
 * child tables loaded in one batched query each (keyed on the compound-name set), so a page of
 * compounds costs four selects, not four-per-compound — no N+1. Writes persist the parent then the
 * children (child inserts nested-select the parent id from the name); an update replaces the
 * child rows wholesale.
 *
 * <p>Each write spans several tables, so it runs through {@link CompoundTransaction} as one
 * transaction.
 */
@DomainService
class CompoundEntityRepositoryRdbms
        extends AbstractEntityRepository<CompoundName, Compound>
        implements CompoundRepository.CompoundEntityRepository {

    private final CompoundMapper mapper;
    private final CompoundTransaction transaction;

    CompoundEntityRepositoryRdbms(CompoundMapper mapper, CompoundTransaction transaction) {
        this.mapper = mapper;
        this.transaction = transaction;
    }

    @Override
    protected Optional<Compound> doGetByName(CompoundName name) {
        CompoundDbo parent = mapper.selectByName(name.value());
        if (parent == null) return Optional.empty();
        return Optional.of(assemble(List.of(parent), Set.of(name.value())).get(0));
    }

    @Override
    protected List<Compound> doGetByNameSet(Set<CompoundName> nameSet) {
        if (nameSet.isEmpty()) return List.of();
        Set<String> slugs = nameSet.stream().map(CompoundName::value).collect(Collectors.toSet());
        return assemble(mapper.selectByNameSet(slugs), slugs);
    }

    @Override
    protected Page<Compound> doGetPage(PageRequest request) {
        int pageSize = request.pageSize();
        List<CompoundDbo> rows = mapper.selectPage(pageSize + 1, request.offset());
        boolean immediateMore = rows.size() > pageSize;
        List<CompoundDbo> parents = rows.stream().limit(pageSize).toList();
        Set<String> names = parents.stream().map(p -> p.name).collect(Collectors.toSet());
        List<Compound> content = assemble(parents, names);

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
    protected void doInsert(Compound entity) {
        transaction.insert(entity);
    }

    @Override
    protected void doUpdate(Compound entity) {
        transaction.update(entity);
    }

    @Override
    protected Compound doSave(Compound entity) {
        transaction.save(entity);
        return entity;
    }

    /** Assemble aggregates from parent rows + one batched load per child table. */
    private List<Compound> assemble(List<CompoundDbo> parents, Set<String> names) {
        if (parents.isEmpty()) return List.of();

        Map<String, Set<FunctionalRole>> roles = mapper.selectFunctionalRoles(names).stream()
                .collect(Collectors.groupingBy(r -> r.compoundName,
                        Collectors.mapping(CompoundFunctionalRoleDbo::toRole, Collectors.toSet())));
        Map<String, Set<PeriodicElement>> elements = mapper.selectConstituentElements(names).stream()
                .collect(Collectors.groupingBy(e -> e.compoundName,
                        Collectors.mapping(CompoundConstituentElementDbo::toElement, Collectors.toSet())));
        Map<String, Map<String, String>> properties = mapper.selectProperties(names).stream()
                .collect(Collectors.groupingBy(p -> p.compoundName,
                        Collectors.toMap(p -> p.propertyKey, p -> p.propertyValue)));

        return parents.stream()
                .map(p -> p.toEntity(
                        roles.getOrDefault(p.name, Set.of()),
                        elements.getOrDefault(p.name, Set.of()),
                        properties.getOrDefault(p.name, Map.of())))
                .toList();
    }
}
