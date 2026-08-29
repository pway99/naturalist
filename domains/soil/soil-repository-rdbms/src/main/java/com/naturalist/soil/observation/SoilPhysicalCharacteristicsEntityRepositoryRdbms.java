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
 * RDBMS adapter for {@link SoilPhysicalCharacteristics} — a surrogate-UUID entity, one row per
 * analysis (the soft {@code labAnalysisId} is UNIQUE, so {@code getByLabAnalysisId} returns an
 * {@code Optional}). The measurement value types and the owned {@code CationBaseSaturation} are
 * flattened onto their columns. {@code getByLabAnalysisIds} resolves the whole set in one call.
 */
@DomainService
class SoilPhysicalCharacteristicsEntityRepositoryRdbms
        extends AbstractEntityRepository<SoilPhysicalCharacteristicsId, SoilPhysicalCharacteristics>
        implements SoilPhysicalCharacteristicsRepository {

    private final SoilPhysicalCharacteristicsMapper mapper;

    SoilPhysicalCharacteristicsEntityRepositoryRdbms(SoilPhysicalCharacteristicsMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    protected Optional<SoilPhysicalCharacteristics> doGetByName(SoilPhysicalCharacteristicsId id) {
        return Optional.ofNullable(mapper.selectById(id.value().toString()))
                .map(SoilPhysicalCharacteristicsDbo::toEntity);
    }

    @Override
    protected List<SoilPhysicalCharacteristics> doGetByNameSet(Set<SoilPhysicalCharacteristicsId> nameSet) {
        if (nameSet.isEmpty()) return List.of();
        List<String> ids = nameSet.stream().map(id -> id.value().toString()).toList();
        return mapper.selectByIdSet(ids).stream().map(SoilPhysicalCharacteristicsDbo::toEntity).toList();
    }

    @Override
    protected Page<SoilPhysicalCharacteristics> doGetPage(PageRequest request) {
        int pageSize = request.pageSize();
        List<SoilPhysicalCharacteristicsDbo> rows = mapper.selectPage(pageSize + 1, request.offset());
        boolean immediateMore = rows.size() > pageSize;
        List<SoilPhysicalCharacteristics> content =
                rows.stream().limit(pageSize).map(SoilPhysicalCharacteristicsDbo::toEntity).toList();

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
    protected void doInsert(SoilPhysicalCharacteristics entity) {
        SoilPhysicalCharacteristicsDbo dbo = SoilPhysicalCharacteristicsDbo.from(entity);
        try {
            mapper.insert(dbo);
        } catch (RuntimeException e) {
            if (RdbmsExceptions.isUniqueViolation(e)) throw new PrimaryKeyConstraintException(entity);
            throw e;
        }
    }

    @Override
    protected void doUpdate(SoilPhysicalCharacteristics entity) {
        SoilPhysicalCharacteristicsDbo dbo = SoilPhysicalCharacteristicsDbo.from(entity);
        if (mapper.updateById(dbo) == 0) throw new EntityNotFoundException(entity);
    }

    @Override
    protected SoilPhysicalCharacteristics doSave(SoilPhysicalCharacteristics entity) {
        if (mapper.selectById(entity.id().value().toString()) != null) doUpdate(entity);
        else doInsert(entity);
        return entity;
    }

    @Override
    public Optional<SoilPhysicalCharacteristics> getByLabAnalysisId(LabAnalysisId labAnalysisId) {
        Observer.forClass(SoilPhysicalCharacteristicsEntityRepositoryRdbms.class)
                .arguments("getByLabAnalysisId", i -> i.identifier(labAnalysisId, "labAnalysisId"))
                .throwWhenInvalid();
        return Optional.ofNullable(mapper.selectByLabAnalysisId(labAnalysisId.value().toString()))
                .map(SoilPhysicalCharacteristicsDbo::toEntity);
    }

    @Override
    public List<SoilPhysicalCharacteristics> getByLabAnalysisIds(Set<LabAnalysisId> labAnalysisIds) {
        Observer.forClass(SoilPhysicalCharacteristicsEntityRepositoryRdbms.class)
                .arguments("getByLabAnalysisIds", i -> i.identifierSet(labAnalysisIds, "labAnalysisIds"))
                .throwWhenInvalid();
        if (labAnalysisIds.isEmpty()) return List.of();
        List<String> ids = labAnalysisIds.stream().map(id -> id.value().toString()).toList();
        return mapper.selectByLabAnalysisIds(ids).stream().map(SoilPhysicalCharacteristicsDbo::toEntity).toList();
    }
}
