package com.naturalist.plants;

import com.naturalist.data.AbstractEntityRepository;
import com.naturalist.data.Page;
import com.naturalist.data.PageRequest;
import com.naturalist.exception.EntityNotFoundException;
import com.naturalist.exception.PrimaryKeyConstraintException;
import com.naturalist.infrastructure.DomainService;
import com.naturalist.persistence.RdbmsExceptions;
import com.naturalist.taxonomy.OrganismFeatureAssignment;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * RDBMS adapter for the plant feature assignment — the kernel generic
 * {@link OrganismFeatureAssignment} keyed by {@link PlantFeatureAssignmentId}. The {@code feature_id}
 * is a direct within-plants FK to {@code plant_feature(id)} (no name indirection); the polymorphic
 * rank is stored as a {@code (rank, rank_name)} pair. Both the {@code UNIQUE(feature_id, rank_name)}
 * composite and the PK surface a duplicate insert as {@link PrimaryKeyConstraintException}. The
 * set-based {@link #getByRankNames} resolves the whole rank set in a single batched query, so it is
 * never an N+1.
 */
@DomainService
class PlantFeatureAssignmentEntityRepositoryRdbms
        extends AbstractEntityRepository<PlantFeatureAssignmentId,
                OrganismFeatureAssignment<PlantFeatureAssignmentId, PlantFeatureId, PlantRankName>>
        implements PlantRepository.FeatureAssignmentRepository {

    private final PlantFeatureAssignmentMapper mapper;

    PlantFeatureAssignmentEntityRepositoryRdbms(PlantFeatureAssignmentMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    protected Optional<OrganismFeatureAssignment<PlantFeatureAssignmentId, PlantFeatureId, PlantRankName>>
    doGetByName(PlantFeatureAssignmentId id) {
        return Optional.ofNullable(mapper.selectById(id.value().toString()))
                .map(PlantFeatureAssignmentDbo::toEntity);
    }

    @Override
    protected List<OrganismFeatureAssignment<PlantFeatureAssignmentId, PlantFeatureId, PlantRankName>>
    doGetByNameSet(Set<PlantFeatureAssignmentId> nameSet) {
        if (nameSet.isEmpty()) return List.of();
        List<String> ids = nameSet.stream().map(id -> id.value().toString()).toList();
        return mapper.selectByIdSet(ids).stream().map(PlantFeatureAssignmentDbo::toEntity).toList();
    }

    @Override
    protected Page<OrganismFeatureAssignment<PlantFeatureAssignmentId, PlantFeatureId, PlantRankName>>
    doGetPage(PageRequest request) {
        int pageSize = request.pageSize();
        List<PlantFeatureAssignmentDbo> rows = mapper.selectPage(pageSize + 1, request.offset());
        boolean immediateMore = rows.size() > pageSize;
        var content = rows.stream().limit(pageSize).map(PlantFeatureAssignmentDbo::toEntity).toList();

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
    protected void doInsert(OrganismFeatureAssignment<PlantFeatureAssignmentId, PlantFeatureId, PlantRankName> entity) {
        PlantFeatureAssignmentDbo dbo = PlantFeatureAssignmentDbo.from(entity);   // validates + throws
        try {
            mapper.insert(dbo);
        } catch (RuntimeException e) {
            if (RdbmsExceptions.isUniqueViolation(e)) throw new PrimaryKeyConstraintException(entity);
            throw e;   // FK violation (missing feature) propagates — the DB enforces it
        }
    }

    @Override
    protected void doUpdate(OrganismFeatureAssignment<PlantFeatureAssignmentId, PlantFeatureId, PlantRankName> entity) {
        PlantFeatureAssignmentDbo dbo = PlantFeatureAssignmentDbo.from(entity);
        if (mapper.updateById(dbo) == 0) throw new EntityNotFoundException(entity);
    }

    @Override
    protected OrganismFeatureAssignment<PlantFeatureAssignmentId, PlantFeatureId, PlantRankName>
    doSave(OrganismFeatureAssignment<PlantFeatureAssignmentId, PlantFeatureId, PlantRankName> entity) {
        if (mapper.selectById(entity.id().value().toString()) != null) doUpdate(entity);
        else doInsert(entity);
        return entity;
    }

    @Override
    public List<OrganismFeatureAssignment<PlantFeatureAssignmentId, PlantFeatureId, PlantRankName>>
    getByRankName(PlantRankName rankName) {
        observer().arguments("getByRankName", i -> i.identifier(rankName, "rankName"))
                .throwWhenInvalid();
        return mapper.selectByRank(rankName.rank().name(), rankName.value()).stream()
                .map(PlantFeatureAssignmentDbo::toEntity).toList();
    }

    @Override
    public List<OrganismFeatureAssignment<PlantFeatureAssignmentId, PlantFeatureId, PlantRankName>>
    getByRankNames(Set<PlantRankName> rankNames) {
        observer().arguments("getByRankNames", i -> i.observableCollection(rankNames, "rankNames"))
                .throwWhenInvalid();
        if (rankNames.isEmpty()) return List.of();
        List<PlantFeatureAssignmentRankKey> keys = rankNames.stream()
                .map(r -> new PlantFeatureAssignmentRankKey(r.rank().name(), r.value()))
                .toList();
        return mapper.selectByRankKeys(keys).stream()
                .map(PlantFeatureAssignmentDbo::toEntity).toList();
    }

    @Override
    public List<OrganismFeatureAssignment<PlantFeatureAssignmentId, PlantFeatureId, PlantRankName>>
    getByFeatureId(PlantFeatureId featureId) {
        observer().arguments("getByFeatureId", i -> i.entityId(featureId, "featureId"))
                .throwWhenInvalid();
        return mapper.selectByFeatureId(featureId.value().toString()).stream()
                .map(PlantFeatureAssignmentDbo::toEntity).toList();
    }
}
