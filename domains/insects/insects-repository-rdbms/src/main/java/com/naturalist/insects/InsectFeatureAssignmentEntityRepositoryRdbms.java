package com.naturalist.insects;

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
 * RDBMS adapter for the insect feature assignment — the kernel generic
 * {@link OrganismFeatureAssignment} keyed by {@link InsectFeatureAssignmentId}. The {@code feature_id}
 * is a direct within-insects FK to {@code insect_feature(id)} (no name indirection); the polymorphic
 * rank is stored as a {@code (rank, rank_name)} pair. Both the {@code UNIQUE(feature_id, rank_name)}
 * composite and the PK surface a duplicate insert as {@link PrimaryKeyConstraintException}. The
 * set-based {@link #getByRankNames} resolves the whole rank set in a single batched query, so it is
 * never an N+1.
 */
@DomainService
class InsectFeatureAssignmentEntityRepositoryRdbms
        extends AbstractEntityRepository<InsectFeatureAssignmentId,
                OrganismFeatureAssignment<InsectFeatureAssignmentId, InsectFeatureId, InsectRankName>>
        implements InsectRepository.FeatureAssignmentRepository {

    private final InsectFeatureAssignmentMapper mapper;

    InsectFeatureAssignmentEntityRepositoryRdbms(InsectFeatureAssignmentMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    protected Optional<OrganismFeatureAssignment<InsectFeatureAssignmentId, InsectFeatureId, InsectRankName>>
    doGetByName(InsectFeatureAssignmentId id) {
        return Optional.ofNullable(mapper.selectById(id.value().toString()))
                .map(InsectFeatureAssignmentDbo::toEntity);
    }

    @Override
    protected List<OrganismFeatureAssignment<InsectFeatureAssignmentId, InsectFeatureId, InsectRankName>>
    doGetByNameSet(Set<InsectFeatureAssignmentId> nameSet) {
        if (nameSet.isEmpty()) return List.of();
        List<String> ids = nameSet.stream().map(id -> id.value().toString()).toList();
        return mapper.selectByIdSet(ids).stream().map(InsectFeatureAssignmentDbo::toEntity).toList();
    }

    @Override
    protected Page<OrganismFeatureAssignment<InsectFeatureAssignmentId, InsectFeatureId, InsectRankName>>
    doGetPage(PageRequest request) {
        int pageSize = request.pageSize();
        List<InsectFeatureAssignmentDbo> rows = mapper.selectPage(pageSize + 1, request.offset());
        boolean immediateMore = rows.size() > pageSize;
        var content = rows.stream().limit(pageSize).map(InsectFeatureAssignmentDbo::toEntity).toList();

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
    protected void doInsert(OrganismFeatureAssignment<InsectFeatureAssignmentId, InsectFeatureId, InsectRankName> entity) {
        InsectFeatureAssignmentDbo dbo = InsectFeatureAssignmentDbo.from(entity);   // validates + throws
        try {
            mapper.insert(dbo);
        } catch (RuntimeException e) {
            if (RdbmsExceptions.isUniqueViolation(e)) throw new PrimaryKeyConstraintException(entity);
            throw e;   // FK violation (missing feature) propagates — the DB enforces it
        }
    }

    @Override
    protected void doUpdate(OrganismFeatureAssignment<InsectFeatureAssignmentId, InsectFeatureId, InsectRankName> entity) {
        InsectFeatureAssignmentDbo dbo = InsectFeatureAssignmentDbo.from(entity);
        if (mapper.updateById(dbo) == 0) throw new EntityNotFoundException(entity);
    }

    @Override
    protected OrganismFeatureAssignment<InsectFeatureAssignmentId, InsectFeatureId, InsectRankName>
    doSave(OrganismFeatureAssignment<InsectFeatureAssignmentId, InsectFeatureId, InsectRankName> entity) {
        if (mapper.selectById(entity.id().value().toString()) != null) doUpdate(entity);
        else doInsert(entity);
        return entity;
    }

    @Override
    public List<OrganismFeatureAssignment<InsectFeatureAssignmentId, InsectFeatureId, InsectRankName>>
    getByRankName(InsectRankName rankName) {
        observer().arguments("getByRankName", i -> i.identifier(rankName, "rankName"))
                .throwWhenInvalid();
        return mapper.selectByRank(rankName.rank().name(), rankName.value()).stream()
                .map(InsectFeatureAssignmentDbo::toEntity).toList();
    }

    @Override
    public List<OrganismFeatureAssignment<InsectFeatureAssignmentId, InsectFeatureId, InsectRankName>>
    getByRankNames(Set<InsectRankName> rankNames) {
        observer().arguments("getByRankNames", i -> i.observableCollection(rankNames, "rankNames"))
                .throwWhenInvalid();
        if (rankNames.isEmpty()) return List.of();
        List<InsectRankKey> keys = rankNames.stream()
                .map(r -> new InsectRankKey(r.rank().name(), r.value()))
                .toList();
        return mapper.selectByRankKeys(keys).stream()
                .map(InsectFeatureAssignmentDbo::toEntity).toList();
    }

    @Override
    public List<OrganismFeatureAssignment<InsectFeatureAssignmentId, InsectFeatureId, InsectRankName>>
    getByFeatureId(InsectFeatureId featureId) {
        observer().arguments("getByFeatureId", i -> i.entityId(featureId, "featureId"))
                .throwWhenInvalid();
        return mapper.selectByFeatureId(featureId.value().toString()).stream()
                .map(InsectFeatureAssignmentDbo::toEntity).toList();
    }
}
