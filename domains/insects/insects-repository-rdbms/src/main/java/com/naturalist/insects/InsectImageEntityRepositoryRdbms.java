package com.naturalist.insects;

import com.naturalist.data.AbstractEntityRepository;
import com.naturalist.data.Page;
import com.naturalist.data.PageRequest;
import com.naturalist.exception.EntityNotFoundException;
import com.naturalist.exception.PrimaryKeyConstraintException;
import com.naturalist.infrastructure.DomainService;
import com.naturalist.observability.Observer;
import com.naturalist.observation.OrganismImage;
import com.naturalist.persistence.RdbmsExceptions;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * RDBMS adapter for insect {@link OrganismImage}s — a surrogate-UUID entity with no owned children. The
 * polymorphic {@code parentName} is stored flat and rebuilt in-module; the optional {@code observationId}
 * is a direct within-insects FK. {@code getByParentNames} resolves a whole parent set in one query.
 */
@DomainService
class InsectImageEntityRepositoryRdbms
        extends AbstractEntityRepository<InsectImageId, OrganismImage<InsectImageId, InsectObservationId, InsectRankName>>
        implements InsectRepository.ImageRepository {

    private final InsectImageMapper mapper;

    InsectImageEntityRepositoryRdbms(InsectImageMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    protected Optional<OrganismImage<InsectImageId, InsectObservationId, InsectRankName>> doGetByName(InsectImageId id) {
        return Optional.ofNullable(mapper.selectById(id.value().toString())).map(InsectImageDbo::toEntity);
    }

    @Override
    protected List<OrganismImage<InsectImageId, InsectObservationId, InsectRankName>> doGetByNameSet(
            Set<InsectImageId> nameSet) {
        if (nameSet.isEmpty()) return List.of();
        List<String> ids = nameSet.stream().map(id -> id.value().toString()).toList();
        return mapper.selectByIdSet(ids).stream().map(InsectImageDbo::toEntity).toList();
    }

    @Override
    protected Page<OrganismImage<InsectImageId, InsectObservationId, InsectRankName>> doGetPage(PageRequest request) {
        int pageSize = request.pageSize();
        List<InsectImageDbo> rows = mapper.selectPage(pageSize + 1, request.offset());
        boolean immediateMore = rows.size() > pageSize;
        List<OrganismImage<InsectImageId, InsectObservationId, InsectRankName>> content =
                rows.stream().limit(pageSize).map(InsectImageDbo::toEntity).toList();

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
    protected void doInsert(OrganismImage<InsectImageId, InsectObservationId, InsectRankName> entity) {
        InsectImageDbo dbo = InsectImageDbo.from(entity);
        try {
            mapper.insert(dbo);
        } catch (RuntimeException e) {
            if (RdbmsExceptions.isUniqueViolation(e)) throw new PrimaryKeyConstraintException(entity);
            throw e;
        }
    }

    @Override
    protected void doUpdate(OrganismImage<InsectImageId, InsectObservationId, InsectRankName> entity) {
        InsectImageDbo dbo = InsectImageDbo.from(entity);
        if (mapper.updateById(dbo) == 0) throw new EntityNotFoundException(entity);
    }

    @Override
    protected OrganismImage<InsectImageId, InsectObservationId, InsectRankName> doSave(
            OrganismImage<InsectImageId, InsectObservationId, InsectRankName> entity) {
        if (mapper.selectById(entity.id().value().toString()) != null) doUpdate(entity);
        else doInsert(entity);
        return entity;
    }

    @Override
    public List<OrganismImage<InsectImageId, InsectObservationId, InsectRankName>> getByParentName(
            InsectRankName parentName) {
        Observer.forClass(InsectImageEntityRepositoryRdbms.class)
                .arguments("getByParentName", i -> i.identifier(parentName, "parentName"))
                .throwWhenInvalid();
        return mapper.selectByParentName(parentName.rank().name(), parentName.value()).stream()
                .map(InsectImageDbo::toEntity).toList();
    }

    @Override
    public List<OrganismImage<InsectImageId, InsectObservationId, InsectRankName>> getByParentNames(
            Set<InsectRankName> parentNames) {
        Observer.forClass(InsectImageEntityRepositoryRdbms.class)
                .arguments("getByParentNames", i -> i.observableCollection(parentNames, "parentNames"))
                .throwWhenInvalid();
        if (parentNames.isEmpty()) return List.of();
        List<InsectRankKey> keys = parentNames.stream()
                .map(p -> new InsectRankKey(p.rank().name(), p.value()))
                .toList();
        return mapper.selectByParentNames(keys).stream().map(InsectImageDbo::toEntity).toList();
    }
}
