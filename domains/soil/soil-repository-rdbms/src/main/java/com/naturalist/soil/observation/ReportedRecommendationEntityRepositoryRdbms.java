package com.naturalist.soil.observation;

import com.naturalist.data.AbstractEntityRepository;
import com.naturalist.data.Page;
import com.naturalist.data.PageRequest;
import com.naturalist.exception.EntityNotFoundException;
import com.naturalist.exception.PrimaryKeyConstraintException;
import com.naturalist.infrastructure.DomainService;
import com.naturalist.observability.Observer;
import com.naturalist.persistence.RdbmsExceptions;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * RDBMS adapter for {@link ReportedRecommendation} — a surrogate-UUID entity whose sealed
 * {@link RecommendedAmount} is flattened onto a kind discriminator plus a nullable value (a
 * {@code Quantity(0)} stays distinct from {@code None}). The three reverse lookups are plain
 * equality/{@code IN} queries; {@code getByLabAnalysisIds} resolves the whole analysis set in one call.
 */
@DomainService
class ReportedRecommendationEntityRepositoryRdbms
        extends AbstractEntityRepository<ReportedRecommendationId, ReportedRecommendation>
        implements ReportedRecommendationRepository {

    private final ReportedRecommendationMapper mapper;

    ReportedRecommendationEntityRepositoryRdbms(ReportedRecommendationMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    protected Optional<ReportedRecommendation> doGetByName(ReportedRecommendationId id) {
        return Optional.ofNullable(mapper.selectById(id.value().toString()))
                .map(ReportedRecommendationDbo::toEntity);
    }

    @Override
    protected List<ReportedRecommendation> doGetByNameSet(Set<ReportedRecommendationId> nameSet) {
        if (nameSet.isEmpty()) return List.of();
        List<String> ids = nameSet.stream().map(id -> id.value().toString()).toList();
        return mapper.selectByIdSet(ids).stream().map(ReportedRecommendationDbo::toEntity).toList();
    }

    @Override
    protected Page<ReportedRecommendation> doGetPage(PageRequest request) {
        int pageSize = request.pageSize();
        List<ReportedRecommendationDbo> rows = mapper.selectPage(pageSize + 1, request.offset());
        boolean immediateMore = rows.size() > pageSize;
        List<ReportedRecommendation> content =
                rows.stream().limit(pageSize).map(ReportedRecommendationDbo::toEntity).toList();

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
    protected void doInsert(ReportedRecommendation entity) {
        ReportedRecommendationDbo dbo = ReportedRecommendationDbo.from(entity);
        try {
            mapper.insert(dbo);
        } catch (RuntimeException e) {
            if (RdbmsExceptions.isUniqueViolation(e)) throw new PrimaryKeyConstraintException(entity);
            throw e;
        }
    }

    @Override
    protected void doUpdate(ReportedRecommendation entity) {
        ReportedRecommendationDbo dbo = ReportedRecommendationDbo.from(entity);
        if (mapper.updateById(dbo) == 0) throw new EntityNotFoundException(entity);
    }

    @Override
    protected ReportedRecommendation doSave(ReportedRecommendation entity) {
        if (mapper.selectById(entity.id().value().toString()) != null) doUpdate(entity);
        else doInsert(entity);
        return entity;
    }

    @Override
    public List<ReportedRecommendation> getByLabAnalysisId(LabAnalysisId labAnalysisId) {
        Observer.forClass(ReportedRecommendationEntityRepositoryRdbms.class)
                .arguments("getByLabAnalysisId", i -> i.identifier(labAnalysisId, "labAnalysisId"))
                .throwWhenInvalid();
        return mapper.selectByLabAnalysisId(labAnalysisId.value().toString()).stream()
                .map(ReportedRecommendationDbo::toEntity).toList();
    }

    @Override
    public List<ReportedRecommendation> getByLabAnalysisIds(Set<LabAnalysisId> labAnalysisIds) {
        Observer.forClass(ReportedRecommendationEntityRepositoryRdbms.class)
                .arguments("getByLabAnalysisIds", i -> i.identifierSet(labAnalysisIds, "labAnalysisIds"))
                .throwWhenInvalid();
        if (labAnalysisIds.isEmpty()) return List.of();
        List<String> ids = labAnalysisIds.stream().map(id -> id.value().toString()).toList();
        return mapper.selectByLabAnalysisIds(ids).stream().map(ReportedRecommendationDbo::toEntity).toList();
    }

    @Override
    public List<ReportedRecommendation> getByInputName(RecommendedInputName inputName) {
        Observer.forClass(ReportedRecommendationEntityRepositoryRdbms.class)
                .arguments("getByInputName", i -> i.entityName(inputName, "inputName"))
                .throwWhenInvalid();
        return mapper.selectByInputName(inputName.value()).stream()
                .map(ReportedRecommendationDbo::toEntity).toList();
    }
}
