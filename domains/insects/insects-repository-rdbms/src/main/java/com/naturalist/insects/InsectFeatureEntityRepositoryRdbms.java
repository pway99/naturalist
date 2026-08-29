package com.naturalist.insects;

import com.naturalist.data.AbstractEntityRepository;
import com.naturalist.data.Page;
import com.naturalist.data.PageRequest;
import com.naturalist.exception.EntityNotFoundException;
import com.naturalist.exception.PrimaryKeyConstraintException;
import com.naturalist.infrastructure.DomainService;
import com.naturalist.persistence.RdbmsExceptions;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * RDBMS adapter for {@link InsectFeature} — a surrogate-UUID entity keyed by {@link InsectFeatureId}.
 * The uuid PK stores as text and is cast in SQL; the {@code value} column carries a single-column
 * {@code UNIQUE}, so a duplicate normalised feature on insert surfaces as
 * {@link PrimaryKeyConstraintException}.
 */
@DomainService
class InsectFeatureEntityRepositoryRdbms
        extends AbstractEntityRepository<InsectFeatureId, InsectFeature>
        implements InsectRepository.FeatureRepository {

    private final InsectFeatureMapper mapper;

    InsectFeatureEntityRepositoryRdbms(InsectFeatureMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    protected Optional<InsectFeature> doGetByName(InsectFeatureId id) {
        return Optional.ofNullable(mapper.selectById(id.value().toString())).map(InsectFeatureDbo::toEntity);
    }

    @Override
    protected List<InsectFeature> doGetByNameSet(Set<InsectFeatureId> nameSet) {
        if (nameSet.isEmpty()) return List.of();
        List<String> ids = nameSet.stream().map(id -> id.value().toString()).toList();
        return mapper.selectByIdSet(ids).stream().map(InsectFeatureDbo::toEntity).toList();
    }

    @Override
    protected Page<InsectFeature> doGetPage(PageRequest request) {
        int pageSize = request.pageSize();
        List<InsectFeatureDbo> rows = mapper.selectPage(pageSize + 1, request.offset());
        boolean immediateMore = rows.size() > pageSize;
        List<InsectFeature> content = rows.stream().limit(pageSize).map(InsectFeatureDbo::toEntity).toList();

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
    protected void doInsert(InsectFeature entity) {
        InsectFeatureDbo dbo = InsectFeatureDbo.from(entity);   // validates + throws
        try {
            mapper.insert(dbo);
        } catch (RuntimeException e) {
            if (RdbmsExceptions.isUniqueViolation(e)) throw new PrimaryKeyConstraintException(entity);
            throw e;
        }
    }

    @Override
    protected void doUpdate(InsectFeature entity) {
        InsectFeatureDbo dbo = InsectFeatureDbo.from(entity);
        if (mapper.updateById(dbo) == 0) throw new EntityNotFoundException(entity);
    }

    @Override
    protected InsectFeature doSave(InsectFeature entity) {
        if (mapper.selectById(entity.id().value().toString()) != null) doUpdate(entity);
        else doInsert(entity);
        return entity;
    }
}
