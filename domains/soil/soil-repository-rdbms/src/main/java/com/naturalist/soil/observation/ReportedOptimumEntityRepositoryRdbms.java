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
 * RDBMS adapter for {@link ReportedOptimum} — a surrogate-UUID entity whose sealed {@link OptimumRange}
 * is flattened onto a shape discriminator plus nullable bounds. The three reverse lookups are plain
 * equality/{@code IN} queries; {@code getByLabAnalysisIds} resolves the whole analysis set in one call.
 */
@DomainService
class ReportedOptimumEntityRepositoryRdbms
        extends AbstractEntityRepository<ReportedOptimumId, ReportedOptimum>
        implements ReportedOptimumRepository {

    private final ReportedOptimumMapper mapper;

    ReportedOptimumEntityRepositoryRdbms(ReportedOptimumMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    protected Optional<ReportedOptimum> doGetByName(ReportedOptimumId id) {
        return Optional.ofNullable(mapper.selectById(id.value().toString())).map(ReportedOptimumDbo::toEntity);
    }

    @Override
    protected List<ReportedOptimum> doGetByNameSet(Set<ReportedOptimumId> nameSet) {
        if (nameSet.isEmpty()) return List.of();
        List<String> ids = nameSet.stream().map(id -> id.value().toString()).toList();
        return mapper.selectByIdSet(ids).stream().map(ReportedOptimumDbo::toEntity).toList();
    }

    @Override
    protected Page<ReportedOptimum> doGetPage(PageRequest request) {
        int pageSize = request.pageSize();
        List<ReportedOptimumDbo> rows = mapper.selectPage(pageSize + 1, request.offset());
        boolean immediateMore = rows.size() > pageSize;
        List<ReportedOptimum> content = rows.stream().limit(pageSize).map(ReportedOptimumDbo::toEntity).toList();

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
    protected void doInsert(ReportedOptimum entity) {
        ReportedOptimumDbo dbo = ReportedOptimumDbo.from(entity);
        try {
            mapper.insert(dbo);
        } catch (RuntimeException e) {
            if (RdbmsExceptions.isUniqueViolation(e)) throw new PrimaryKeyConstraintException(entity);
            throw e;
        }
    }

    @Override
    protected void doUpdate(ReportedOptimum entity) {
        ReportedOptimumDbo dbo = ReportedOptimumDbo.from(entity);
        if (mapper.updateById(dbo) == 0) throw new EntityNotFoundException(entity);
    }

    @Override
    protected ReportedOptimum doSave(ReportedOptimum entity) {
        if (mapper.selectById(entity.id().value().toString()) != null) doUpdate(entity);
        else doInsert(entity);
        return entity;
    }

    @Override
    public List<ReportedOptimum> getByLabAnalysisId(LabAnalysisId labAnalysisId) {
        Observer.forClass(ReportedOptimumEntityRepositoryRdbms.class)
                .arguments("getByLabAnalysisId", i -> i.identifier(labAnalysisId, "labAnalysisId"))
                .throwWhenInvalid();
        return mapper.selectByLabAnalysisId(labAnalysisId.value().toString()).stream()
                .map(ReportedOptimumDbo::toEntity).toList();
    }

    @Override
    public List<ReportedOptimum> getByLabAnalysisIds(Set<LabAnalysisId> labAnalysisIds) {
        Observer.forClass(ReportedOptimumEntityRepositoryRdbms.class)
                .arguments("getByLabAnalysisIds", i -> i.identifierSet(labAnalysisIds, "labAnalysisIds"))
                .throwWhenInvalid();
        if (labAnalysisIds.isEmpty()) return List.of();
        List<String> ids = labAnalysisIds.stream().map(id -> id.value().toString()).toList();
        return mapper.selectByLabAnalysisIds(ids).stream().map(ReportedOptimumDbo::toEntity).toList();
    }

    @Override
    public List<ReportedOptimum> getByNutrientName(NutrientName nutrientName) {
        Observer.forClass(ReportedOptimumEntityRepositoryRdbms.class)
                .arguments("getByNutrientName", i -> i.entityName(nutrientName, "nutrientName"))
                .throwWhenInvalid();
        return mapper.selectByNutrientName(nutrientName.value()).stream()
                .map(ReportedOptimumDbo::toEntity).toList();
    }
}
