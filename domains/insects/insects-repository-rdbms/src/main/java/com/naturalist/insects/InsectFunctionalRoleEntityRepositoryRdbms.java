package com.naturalist.insects;

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
 * RDBMS adapter for {@link InsectFunctionalRole} — a surrogate-UUID entity keyed by
 * {@link InsectFunctionalRoleId} that owns a non-empty {@code Set<FunctionalGuild>} child table.
 * Reads assemble each parent row with its guilds loaded in one batched query (keyed on the id set),
 * so a page of roles costs two selects, not two-per-role — no N+1. Writes persist the parent then the
 * guild rows (which carry the parent's own id directly); an update replaces the guild rows wholesale.
 * The single-column {@code UNIQUE(parent_name)} (one role per organism) surfaces a duplicate taxon on
 * insert as {@link PrimaryKeyConstraintException}. The set-based {@link #getByParentNames} resolves
 * the whole parent set in a single batched query, so it is never an N+1.
 */
@DomainService
class InsectFunctionalRoleEntityRepositoryRdbms
        extends AbstractEntityRepository<InsectFunctionalRoleId, InsectFunctionalRole>
        implements InsectRepository.FunctionalRoleRepository {

    private final InsectFunctionalRoleMapper mapper;

    InsectFunctionalRoleEntityRepositoryRdbms(InsectFunctionalRoleMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    protected Optional<InsectFunctionalRole> doGetByName(InsectFunctionalRoleId id) {
        InsectFunctionalRoleDbo parent = mapper.selectById(id.value().toString());
        if (parent == null) return Optional.empty();
        return Optional.of(assemble(List.of(parent)).get(0));
    }

    @Override
    protected List<InsectFunctionalRole> doGetByNameSet(Set<InsectFunctionalRoleId> nameSet) {
        if (nameSet.isEmpty()) return List.of();
        List<String> ids = nameSet.stream().map(id -> id.value().toString()).toList();
        return assemble(mapper.selectByIdSet(ids));
    }

    @Override
    protected Page<InsectFunctionalRole> doGetPage(PageRequest request) {
        int pageSize = request.pageSize();
        List<InsectFunctionalRoleDbo> rows = mapper.selectPage(pageSize + 1, request.offset());
        boolean immediateMore = rows.size() > pageSize;
        List<InsectFunctionalRole> content = assemble(rows.stream().limit(pageSize).toList());

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
    protected void doInsert(InsectFunctionalRole entity) {
        InsectFunctionalRoleDbo dbo = InsectFunctionalRoleDbo.from(entity);   // validates + throws
        try {
            mapper.insert(dbo);
        } catch (RuntimeException e) {
            if (RdbmsExceptions.isUniqueViolation(e)) throw new PrimaryKeyConstraintException(entity);
            throw e;
        }
        insertGuilds(entity);
    }

    @Override
    protected void doUpdate(InsectFunctionalRole entity) {
        InsectFunctionalRoleDbo dbo = InsectFunctionalRoleDbo.from(entity);
        if (mapper.updateById(dbo) == 0) throw new EntityNotFoundException(entity);
        mapper.deleteGuilds(entity.id().value().toString());
        insertGuilds(entity);
    }

    @Override
    protected InsectFunctionalRole doSave(InsectFunctionalRole entity) {
        if (mapper.selectById(entity.id().value().toString()) != null) doUpdate(entity);
        else doInsert(entity);
        return entity;
    }

    @Override
    public List<InsectFunctionalRole> getByGuild(FunctionalGuild guild) {
        observer().arguments("getByGuild", i -> i.notNull(guild, "guild"))
                .throwWhenInvalid();
        return assemble(mapper.selectByGuild(guild.name()));
    }

    @Override
    public Optional<InsectFunctionalRole> getByParentName(InsectRankName parentName) {
        observer().arguments("getByParentName", i -> i.identifier(parentName, "parentName"))
                .throwWhenInvalid();
        InsectFunctionalRoleDbo parent = mapper.selectByParentName(parentName.rank().name(), parentName.value());
        if (parent == null) return Optional.empty();
        return Optional.of(assemble(List.of(parent)).get(0));
    }

    @Override
    public List<InsectFunctionalRole> getByParentNames(Set<InsectRankName> parentNames) {
        observer().arguments("getByParentNames", i -> i.observableCollection(parentNames, "parentNames"))
                .throwWhenInvalid();
        if (parentNames.isEmpty()) return List.of();
        List<InsectRankKey> keys = parentNames.stream()
                .map(r -> new InsectRankKey(r.rank().name(), r.value()))
                .toList();
        return assemble(mapper.selectByParentNames(keys));
    }

    private void insertGuilds(InsectFunctionalRole entity) {
        String id = entity.id().value().toString();
        for (FunctionalGuild guild : entity.guilds()) {
            mapper.insertGuild(InsectFunctionalRoleGuildDbo.from(id, guild));
        }
    }

    /** Assemble entities from parent rows + one batched load of their guild rows (grouped by id). */
    private List<InsectFunctionalRole> assemble(List<InsectFunctionalRoleDbo> parents) {
        if (parents.isEmpty()) return List.of();
        List<String> ids = parents.stream().map(p -> p.id).toList();
        Map<String, Set<FunctionalGuild>> guilds = mapper.selectGuildsByIds(ids).stream()
                .collect(Collectors.groupingBy(g -> g.roleId,
                        Collectors.mapping(InsectFunctionalRoleGuildDbo::toGuild, Collectors.toSet())));
        return parents.stream()
                .map(p -> p.toEntity(guilds.getOrDefault(p.id, Set.of())))
                .toList();
    }
}
