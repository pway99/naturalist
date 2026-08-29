package com.naturalist.plants;

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
 * RDBMS adapter for {@link PlantEcologicalRole} — a surrogate-UUID entity keyed by
 * {@link PlantEcologicalRoleId} that owns a non-empty {@code Set<PlantRole>} child table. Reads
 * assemble each parent row with its roles loaded in one batched query (keyed on the id set), so a
 * page of roles costs two selects, not two-per-role — no N+1. Writes persist the parent then the
 * role rows (which carry the parent's own id directly); an update replaces the role rows wholesale.
 * The {@code UNIQUE(plant_rank, plant_name)} surfaces a duplicate taxon on insert as
 * {@link PrimaryKeyConstraintException}.
 */
@DomainService
class PlantEcologicalRoleEntityRepositoryRdbms
        extends AbstractEntityRepository<PlantEcologicalRoleId, PlantEcologicalRole>
        implements PlantRepository.EcologicalRoleRepository {

    private final PlantEcologicalRoleMapper mapper;

    PlantEcologicalRoleEntityRepositoryRdbms(PlantEcologicalRoleMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    protected Optional<PlantEcologicalRole> doGetByName(PlantEcologicalRoleId id) {
        PlantEcologicalRoleDbo parent = mapper.selectById(id.value().toString());
        if (parent == null) return Optional.empty();
        return Optional.of(assemble(List.of(parent)).get(0));
    }

    @Override
    protected List<PlantEcologicalRole> doGetByNameSet(Set<PlantEcologicalRoleId> nameSet) {
        if (nameSet.isEmpty()) return List.of();
        List<String> ids = nameSet.stream().map(id -> id.value().toString()).toList();
        return assemble(mapper.selectByIdSet(ids));
    }

    @Override
    protected Page<PlantEcologicalRole> doGetPage(PageRequest request) {
        int pageSize = request.pageSize();
        List<PlantEcologicalRoleDbo> rows = mapper.selectPage(pageSize + 1, request.offset());
        boolean immediateMore = rows.size() > pageSize;
        List<PlantEcologicalRole> content = assemble(rows.stream().limit(pageSize).toList());

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
    protected void doInsert(PlantEcologicalRole entity) {
        PlantEcologicalRoleDbo dbo = PlantEcologicalRoleDbo.from(entity);   // validates + throws
        try {
            mapper.insert(dbo);
        } catch (RuntimeException e) {
            if (RdbmsExceptions.isUniqueViolation(e)) throw new PrimaryKeyConstraintException(entity);
            throw e;
        }
        insertRoles(entity);
    }

    @Override
    protected void doUpdate(PlantEcologicalRole entity) {
        PlantEcologicalRoleDbo dbo = PlantEcologicalRoleDbo.from(entity);
        if (mapper.updateById(dbo) == 0) throw new EntityNotFoundException(entity);
        mapper.deleteRoles(entity.id().value().toString());
        insertRoles(entity);
    }

    @Override
    protected PlantEcologicalRole doSave(PlantEcologicalRole entity) {
        if (mapper.selectById(entity.id().value().toString()) != null) doUpdate(entity);
        else doInsert(entity);
        return entity;
    }

    @Override
    public Optional<PlantEcologicalRole> getByPlantName(PlantRankName plantName) {
        observer().arguments("getByPlantName", i -> i.identifier(plantName, "plantName"))
                .throwWhenInvalid();
        PlantEcologicalRoleDbo parent = mapper.selectByPlantName(plantName.rank().name(), plantName.value());
        if (parent == null) return Optional.empty();
        return Optional.of(assemble(List.of(parent)).get(0));
    }

    private void insertRoles(PlantEcologicalRole entity) {
        String id = entity.id().value().toString();
        for (PlantRole role : entity.roles()) {
            mapper.insertRole(PlantEcologicalRoleRoleDbo.from(id, role));
        }
    }

    /** Assemble entities from parent rows + one batched load of their role rows (grouped by id). */
    private List<PlantEcologicalRole> assemble(List<PlantEcologicalRoleDbo> parents) {
        if (parents.isEmpty()) return List.of();
        List<String> ids = parents.stream().map(p -> p.id).toList();
        Map<String, Set<PlantRole>> roles = mapper.selectRolesByIds(ids).stream()
                .collect(Collectors.groupingBy(r -> r.roleId,
                        Collectors.mapping(PlantEcologicalRoleRoleDbo::toRole, Collectors.toSet())));
        return parents.stream()
                .map(p -> p.toEntity(roles.getOrDefault(p.id, Set.of())))
                .toList();
    }
}
