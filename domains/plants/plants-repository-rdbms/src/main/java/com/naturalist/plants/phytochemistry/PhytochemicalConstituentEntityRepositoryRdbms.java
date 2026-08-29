package com.naturalist.plants.phytochemistry;

import com.naturalist.chemistry.compound.CompoundName;
import com.naturalist.data.AbstractEntityRepository;
import com.naturalist.data.Page;
import com.naturalist.data.PageRequest;
import com.naturalist.exception.EntityNotFoundException;
import com.naturalist.exception.PrimaryKeyConstraintException;
import com.naturalist.infrastructure.DomainService;
import com.naturalist.persistence.RdbmsExceptions;
import com.naturalist.plants.PlantRankName;
import com.naturalist.plants.phytochemistry.role.PhytochemicalRole;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * RDBMS adapter for the {@link PhytochemicalConstituent} link entity. Reads assemble the parent
 * row with its two child tables (roles, tissues) loaded in one batched query each (keyed on the
 * constituent-name set), so a page of constituents costs three selects, not three-per-constituent
 * — no N+1. Writes persist the parent then the children (child inserts nested-select the parent id
 * from the name); an update replaces the child rows wholesale. The reverse {@link #getByPlantName}
 * / {@link #getByCompoundName} resolve the matching constituent names, then assemble them.
 */
@DomainService
class PhytochemicalConstituentEntityRepositoryRdbms
        extends AbstractEntityRepository<PhytochemicalConstituentName, PhytochemicalConstituent>
        implements PhytochemicalConstituentRepository {

    private final PhytochemicalConstituentMapper mapper;

    PhytochemicalConstituentEntityRepositoryRdbms(PhytochemicalConstituentMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    protected Optional<PhytochemicalConstituent> doGetByName(PhytochemicalConstituentName name) {
        PhytochemicalConstituentDbo parent = mapper.selectByName(name.value());
        if (parent == null) return Optional.empty();
        return Optional.of(assemble(List.of(parent), Set.of(name.value())).get(0));
    }

    @Override
    protected List<PhytochemicalConstituent> doGetByNameSet(Set<PhytochemicalConstituentName> nameSet) {
        if (nameSet.isEmpty()) return List.of();
        Set<String> slugs = nameSet.stream()
                .map(PhytochemicalConstituentName::value).collect(Collectors.toSet());
        return assemble(mapper.selectByNameSet(slugs), slugs);
    }

    @Override
    protected Page<PhytochemicalConstituent> doGetPage(PageRequest request) {
        int pageSize = request.pageSize();
        List<PhytochemicalConstituentDbo> rows = mapper.selectPage(pageSize + 1, request.offset());
        boolean immediateMore = rows.size() > pageSize;
        List<PhytochemicalConstituentDbo> parents = rows.stream().limit(pageSize).toList();
        Set<String> names = parents.stream().map(p -> p.name).collect(Collectors.toSet());
        List<PhytochemicalConstituent> content = assemble(parents, names);

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
    protected void doInsert(PhytochemicalConstituent entity) {
        PhytochemicalConstituentDbo dbo = PhytochemicalConstituentDbo.from(entity);   // validates + throws
        try {
            mapper.insertConstituent(dbo);
        } catch (RuntimeException e) {
            if (RdbmsExceptions.isUniqueViolation(e)) throw new PrimaryKeyConstraintException(entity);
            throw e;
        }
        insertChildren(entity);
    }

    @Override
    protected void doUpdate(PhytochemicalConstituent entity) {
        PhytochemicalConstituentDbo dbo = PhytochemicalConstituentDbo.from(entity);
        if (mapper.updateConstituent(dbo) == 0) throw new EntityNotFoundException(entity);
        String name = entity.name().value();
        mapper.deleteRoles(name);
        mapper.deleteTissues(name);
        insertChildren(entity);
    }

    @Override
    protected PhytochemicalConstituent doSave(PhytochemicalConstituent entity) {
        if (mapper.selectByName(entity.name().value()) != null) doUpdate(entity);
        else doInsert(entity);
        return entity;
    }

    @Override
    public List<PhytochemicalConstituent> getByPlantName(PlantRankName plantName) {
        observer().arguments("getByPlantName", i -> i.identifier(plantName, "plantName")).throwWhenInvalid();
        List<String> names = mapper.selectNamesByPlant(plantName.rank().name(), plantName.value());
        if (names.isEmpty()) return List.of();
        return assemble(mapper.selectByNameSet(names), Set.copyOf(names));
    }

    @Override
    public List<PhytochemicalConstituent> getByCompoundName(CompoundName compoundName) {
        observer().arguments("getByCompoundName", i -> i.entityName(compoundName, "compoundName")).throwWhenInvalid();
        List<String> names = mapper.selectNamesByCompound(compoundName.value());
        if (names.isEmpty()) return List.of();
        return assemble(mapper.selectByNameSet(names), Set.copyOf(names));
    }

    private void insertChildren(PhytochemicalConstituent c) {
        PhytochemicalConstituentName name = c.name();
        for (PhytochemicalRole role : c.roles()) {
            mapper.insertRole(PhytochemicalConstituentRoleDbo.from(name, role));
        }
        for (PlantTissue tissue : c.tissues()) {
            mapper.insertTissue(PhytochemicalConstituentTissueDbo.from(name, tissue));
        }
    }

    /** Assemble entities from parent rows + one batched load per child table. */
    private List<PhytochemicalConstituent> assemble(List<PhytochemicalConstituentDbo> parents, Set<String> names) {
        if (parents.isEmpty()) return List.of();

        Map<String, Set<PhytochemicalRole>> roles = mapper.selectRoles(names).stream()
                .collect(Collectors.groupingBy(r -> r.constituentName,
                        Collectors.mapping(PhytochemicalConstituentRoleDbo::toRole, Collectors.toSet())));
        Map<String, Set<PlantTissue>> tissues = mapper.selectTissues(names).stream()
                .collect(Collectors.groupingBy(t -> t.constituentName,
                        Collectors.mapping(PhytochemicalConstituentTissueDbo::toTissue, Collectors.toSet())));

        return parents.stream()
                .map(p -> p.toEntity(
                        roles.getOrDefault(p.name, Set.of()),
                        tissues.getOrDefault(p.name, Set.of())))
                .toList();
    }
}
