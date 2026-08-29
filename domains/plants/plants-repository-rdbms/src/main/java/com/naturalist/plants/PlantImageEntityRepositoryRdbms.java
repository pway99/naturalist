package com.naturalist.plants;

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
 * RDBMS adapter for plant {@link OrganismImage}s — a surrogate-UUID entity with no owned children. Its
 * polymorphic {@code parentName} is stored flat and rebuilt in-module; its optional {@code observationId}
 * is a direct within-plants FK. Mirrors the chemistry depiction adapter, plus the {@code getByParentName}
 * lookup.
 */
@DomainService
class PlantImageEntityRepositoryRdbms
        extends AbstractEntityRepository<PlantImageId, OrganismImage<PlantImageId, PlantObservationId, PlantRankName>>
        implements PlantRepository.ImageRepository {

    private final PlantImageMapper mapper;

    PlantImageEntityRepositoryRdbms(PlantImageMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    protected Optional<OrganismImage<PlantImageId, PlantObservationId, PlantRankName>> doGetByName(PlantImageId id) {
        return Optional.ofNullable(mapper.selectById(id.value().toString())).map(PlantImageDbo::toEntity);
    }

    @Override
    protected List<OrganismImage<PlantImageId, PlantObservationId, PlantRankName>> doGetByNameSet(
            Set<PlantImageId> nameSet) {
        if (nameSet.isEmpty()) return List.of();
        List<String> ids = nameSet.stream().map(id -> id.value().toString()).toList();
        return mapper.selectByIdSet(ids).stream().map(PlantImageDbo::toEntity).toList();
    }

    @Override
    protected Page<OrganismImage<PlantImageId, PlantObservationId, PlantRankName>> doGetPage(PageRequest request) {
        int pageSize = request.pageSize();
        List<PlantImageDbo> rows = mapper.selectPage(pageSize + 1, request.offset());
        boolean immediateMore = rows.size() > pageSize;
        List<OrganismImage<PlantImageId, PlantObservationId, PlantRankName>> content =
                rows.stream().limit(pageSize).map(PlantImageDbo::toEntity).toList();

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
    protected void doInsert(OrganismImage<PlantImageId, PlantObservationId, PlantRankName> entity) {
        PlantImageDbo dbo = PlantImageDbo.from(entity);   // validates + throws
        try {
            mapper.insert(dbo);
        } catch (RuntimeException e) {
            if (RdbmsExceptions.isUniqueViolation(e)) throw new PrimaryKeyConstraintException(entity);
            throw e;
        }
    }

    @Override
    protected void doUpdate(OrganismImage<PlantImageId, PlantObservationId, PlantRankName> entity) {
        PlantImageDbo dbo = PlantImageDbo.from(entity);
        if (mapper.updateById(dbo) == 0) throw new EntityNotFoundException(entity);
    }

    @Override
    protected OrganismImage<PlantImageId, PlantObservationId, PlantRankName> doSave(
            OrganismImage<PlantImageId, PlantObservationId, PlantRankName> entity) {
        if (mapper.selectById(entity.id().value().toString()) != null) doUpdate(entity);
        else doInsert(entity);
        return entity;
    }

    @Override
    public List<OrganismImage<PlantImageId, PlantObservationId, PlantRankName>> getByParentName(
            PlantRankName parentName) {
        Observer.forClass(PlantImageEntityRepositoryRdbms.class)
                .arguments("getByParentName", i -> i.identifier(parentName, "parentName"))
                .throwWhenInvalid();
        return mapper.selectByParentName(parentName.rank().name(), parentName.value()).stream()
                .map(PlantImageDbo::toEntity).toList();
    }
}
