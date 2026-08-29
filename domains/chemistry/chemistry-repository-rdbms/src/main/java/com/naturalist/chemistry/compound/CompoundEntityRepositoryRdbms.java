package com.naturalist.chemistry.compound;

import com.naturalist.chemistry.compound.role.FunctionalRole;
import com.naturalist.chemistry.element.PeriodicElement;
import com.naturalist.data.AbstractEntityRepository;
import com.naturalist.data.Page;
import com.naturalist.data.PageRequest;
import com.naturalist.exception.EntityNotFoundException;
import com.naturalist.exception.PrimaryKeyConstraintException;
import com.naturalist.infrastructure.DomainService;
import com.naturalist.persistence.RdbmsExceptions;

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
 */
@DomainService
class CompoundEntityRepositoryRdbms
        extends AbstractEntityRepository<CompoundName, Compound>
        implements CompoundRepository.CompoundEntityRepository {

    private final CompoundMapper mapper;

    CompoundEntityRepositoryRdbms(CompoundMapper mapper) {
        this.mapper = mapper;
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
        CompoundDbo dbo = CompoundDbo.from(entity);   // validates the parent scalars + throws
        try {
            mapper.insertCompound(dbo);
        } catch (RuntimeException e) {
            if (RdbmsExceptions.isUniqueViolation(e)) throw new PrimaryKeyConstraintException(entity);
            throw e;
        }
        insertChildren(entity);
    }

    @Override
    protected void doUpdate(Compound entity) {
        CompoundDbo dbo = CompoundDbo.from(entity);
        if (mapper.updateCompound(dbo) == 0) throw new EntityNotFoundException(entity);
        String name = entity.name().value();
        mapper.deleteFunctionalRoles(name);
        mapper.deleteConstituentElements(name);
        mapper.deleteProperties(name);
        insertChildren(entity);
    }

    @Override
    protected Compound doSave(Compound entity) {
        if (mapper.selectByName(entity.name().value()) != null) doUpdate(entity);
        else doInsert(entity);
        return entity;
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
