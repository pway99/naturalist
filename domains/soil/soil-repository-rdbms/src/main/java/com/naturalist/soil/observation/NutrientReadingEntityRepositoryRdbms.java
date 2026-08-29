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
 * RDBMS adapter for {@link NutrientReading} — a surrogate-UUID entity keyed by nutrient and (soft,
 * no-FK) analysis. The three reverse lookups are plain equality/{@code IN} queries;
 * {@code getByLabAnalysisIds} resolves the whole analysis set in one batched query.
 */
@DomainService
class NutrientReadingEntityRepositoryRdbms
        extends AbstractEntityRepository<NutrientReadingId, NutrientReading>
        implements NutrientReadingRepository {

    private final NutrientReadingMapper mapper;

    NutrientReadingEntityRepositoryRdbms(NutrientReadingMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    protected Optional<NutrientReading> doGetByName(NutrientReadingId id) {
        return Optional.ofNullable(mapper.selectById(id.value().toString())).map(NutrientReadingDbo::toEntity);
    }

    @Override
    protected List<NutrientReading> doGetByNameSet(Set<NutrientReadingId> nameSet) {
        if (nameSet.isEmpty()) return List.of();
        List<String> ids = nameSet.stream().map(id -> id.value().toString()).toList();
        return mapper.selectByIdSet(ids).stream().map(NutrientReadingDbo::toEntity).toList();
    }

    @Override
    protected Page<NutrientReading> doGetPage(PageRequest request) {
        int pageSize = request.pageSize();
        List<NutrientReadingDbo> rows = mapper.selectPage(pageSize + 1, request.offset());
        boolean immediateMore = rows.size() > pageSize;
        List<NutrientReading> content = rows.stream().limit(pageSize).map(NutrientReadingDbo::toEntity).toList();

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
    protected void doInsert(NutrientReading entity) {
        NutrientReadingDbo dbo = NutrientReadingDbo.from(entity);
        try {
            mapper.insert(dbo);
        } catch (RuntimeException e) {
            if (RdbmsExceptions.isUniqueViolation(e)) throw new PrimaryKeyConstraintException(entity);
            throw e;
        }
    }

    @Override
    protected void doUpdate(NutrientReading entity) {
        NutrientReadingDbo dbo = NutrientReadingDbo.from(entity);
        if (mapper.updateById(dbo) == 0) throw new EntityNotFoundException(entity);
    }

    @Override
    protected NutrientReading doSave(NutrientReading entity) {
        if (mapper.selectById(entity.id().value().toString()) != null) doUpdate(entity);
        else doInsert(entity);
        return entity;
    }

    @Override
    public List<NutrientReading> getByLabAnalysisId(LabAnalysisId labAnalysisId) {
        Observer.forClass(NutrientReadingEntityRepositoryRdbms.class)
                .arguments("getByLabAnalysisId", i -> i.identifier(labAnalysisId, "labAnalysisId"))
                .throwWhenInvalid();
        return mapper.selectByLabAnalysisId(labAnalysisId.value().toString()).stream()
                .map(NutrientReadingDbo::toEntity).toList();
    }

    @Override
    public List<NutrientReading> getByLabAnalysisIds(Set<LabAnalysisId> labAnalysisIds) {
        Observer.forClass(NutrientReadingEntityRepositoryRdbms.class)
                .arguments("getByLabAnalysisIds", i -> i.identifierSet(labAnalysisIds, "labAnalysisIds"))
                .throwWhenInvalid();
        if (labAnalysisIds.isEmpty()) return List.of();
        List<String> ids = labAnalysisIds.stream().map(id -> id.value().toString()).toList();
        return mapper.selectByLabAnalysisIds(ids).stream().map(NutrientReadingDbo::toEntity).toList();
    }

    @Override
    public List<NutrientReading> getByNutrientName(NutrientName nutrientName) {
        Observer.forClass(NutrientReadingEntityRepositoryRdbms.class)
                .arguments("getByNutrientName", i -> i.entityName(nutrientName, "nutrientName"))
                .throwWhenInvalid();
        return mapper.selectByNutrientName(nutrientName.value()).stream()
                .map(NutrientReadingDbo::toEntity).toList();
    }
}
