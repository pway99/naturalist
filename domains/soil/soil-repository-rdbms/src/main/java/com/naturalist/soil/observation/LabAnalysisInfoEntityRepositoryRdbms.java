package com.naturalist.soil.observation;

import com.naturalist.data.AbstractEntityRepository;
import com.naturalist.data.Page;
import com.naturalist.data.PageRequest;
import com.naturalist.exception.EntityNotFoundException;
import com.naturalist.exception.PrimaryKeyConstraintException;
import com.naturalist.infrastructure.DomainService;
import com.naturalist.observability.Observer;
import com.naturalist.persistence.RdbmsExceptions;
import com.naturalist.soil.SoilProfileName;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * RDBMS adapter for {@link LabAnalysisInfo} — a surrogate-UUID entity with a nullable owned
 * {@code SamplingProtocol} (flattened onto three columns) and a soft {@code soilProfileName} slug
 * (no FK). {@code getBySoilProfileName} is a plain equality lookup on that slug column.
 */
@DomainService
class LabAnalysisInfoEntityRepositoryRdbms
        extends AbstractEntityRepository<LabAnalysisId, LabAnalysisInfo>
        implements LabAnalysisInfoRepository {

    private final LabAnalysisInfoMapper mapper;

    LabAnalysisInfoEntityRepositoryRdbms(LabAnalysisInfoMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    protected Optional<LabAnalysisInfo> doGetByName(LabAnalysisId id) {
        return Optional.ofNullable(mapper.selectById(id.value().toString())).map(LabAnalysisInfoDbo::toEntity);
    }

    @Override
    protected List<LabAnalysisInfo> doGetByNameSet(Set<LabAnalysisId> nameSet) {
        if (nameSet.isEmpty()) return List.of();
        List<String> ids = nameSet.stream().map(id -> id.value().toString()).toList();
        return mapper.selectByIdSet(ids).stream().map(LabAnalysisInfoDbo::toEntity).toList();
    }

    @Override
    protected Page<LabAnalysisInfo> doGetPage(PageRequest request) {
        int pageSize = request.pageSize();
        List<LabAnalysisInfoDbo> rows = mapper.selectPage(pageSize + 1, request.offset());
        boolean immediateMore = rows.size() > pageSize;
        List<LabAnalysisInfo> content = rows.stream().limit(pageSize).map(LabAnalysisInfoDbo::toEntity).toList();

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
    protected void doInsert(LabAnalysisInfo entity) {
        LabAnalysisInfoDbo dbo = LabAnalysisInfoDbo.from(entity);
        try {
            mapper.insert(dbo);
        } catch (RuntimeException e) {
            if (RdbmsExceptions.isUniqueViolation(e)) throw new PrimaryKeyConstraintException(entity);
            throw e;
        }
    }

    @Override
    protected void doUpdate(LabAnalysisInfo entity) {
        LabAnalysisInfoDbo dbo = LabAnalysisInfoDbo.from(entity);
        if (mapper.updateById(dbo) == 0) throw new EntityNotFoundException(entity);
    }

    @Override
    protected LabAnalysisInfo doSave(LabAnalysisInfo entity) {
        if (mapper.selectById(entity.id().value().toString()) != null) doUpdate(entity);
        else doInsert(entity);
        return entity;
    }

    @Override
    public List<LabAnalysisInfo> getBySoilProfileName(SoilProfileName soilProfileName) {
        Observer.forClass(LabAnalysisInfoEntityRepositoryRdbms.class)
                .arguments("getBySoilProfileName", i -> i.entityName(soilProfileName, "soilProfileName"))
                .throwWhenInvalid();
        return mapper.selectBySoilProfileName(soilProfileName.value()).stream()
                .map(LabAnalysisInfoDbo::toEntity).toList();
    }
}
