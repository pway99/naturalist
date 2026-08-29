package com.naturalist.garden;

import com.naturalist.data.AbstractEntityRepository;
import com.naturalist.data.Page;
import com.naturalist.data.PageRequest;
import com.naturalist.exception.EntityNotFoundException;
import com.naturalist.exception.PrimaryKeyConstraintException;
import com.naturalist.infrastructure.DomainService;
import com.naturalist.observability.Observer;
import com.naturalist.persistence.RdbmsExceptions;
import com.naturalist.plants.PlantRankName;
import com.naturalist.zone.ZoneName;
import com.naturalist.zone.subzone.SubZoneName;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * RDBMS adapter for {@link Planting} — a flat surrogate-UUID entity with no owned children and no foreign
 * keys. The polymorphic {@code plantName} and the zone/cultivar slugs are stored flat and rebuilt
 * in-module. {@code getByPlantName} decomposes the rank name to (rank, slug) so ranks never collide.
 */
@DomainService
class PlantingEntityRepositoryRdbms
        extends AbstractEntityRepository<PlantingId, Planting>
        implements PlantingRepository {

    private final PlantingMapper mapper;

    PlantingEntityRepositoryRdbms(PlantingMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    protected Optional<Planting> doGetByName(PlantingId id) {
        return Optional.ofNullable(mapper.selectById(id.value().toString())).map(PlantingDbo::toEntity);
    }

    @Override
    protected List<Planting> doGetByNameSet(Set<PlantingId> nameSet) {
        if (nameSet.isEmpty()) return List.of();
        List<String> ids = nameSet.stream().map(id -> id.value().toString()).toList();
        return mapper.selectByIdSet(ids).stream().map(PlantingDbo::toEntity).toList();
    }

    @Override
    protected Page<Planting> doGetPage(PageRequest request) {
        int pageSize = request.pageSize();
        List<PlantingDbo> rows = mapper.selectPage(pageSize + 1, request.offset());
        boolean immediateMore = rows.size() > pageSize;
        List<Planting> content = rows.stream().limit(pageSize).map(PlantingDbo::toEntity).toList();

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
    protected void doInsert(Planting entity) {
        PlantingDbo dbo = PlantingDbo.from(entity);
        try {
            mapper.insert(dbo);
        } catch (RuntimeException e) {
            if (RdbmsExceptions.isUniqueViolation(e)) throw new PrimaryKeyConstraintException(entity);
            throw e;
        }
    }

    @Override
    protected void doUpdate(Planting entity) {
        PlantingDbo dbo = PlantingDbo.from(entity);
        if (mapper.updateById(dbo) == 0) throw new EntityNotFoundException(entity);
    }

    @Override
    protected Planting doSave(Planting entity) {
        if (mapper.selectById(entity.id().value().toString()) != null) doUpdate(entity);
        else doInsert(entity);
        return entity;
    }

    @Override
    public List<Planting> getByZoneName(ZoneName zoneName) {
        Observer.forClass(PlantingEntityRepositoryRdbms.class)
                .arguments("getByZoneName", i -> i.identifier(zoneName, "zoneName"))
                .throwWhenInvalid();
        return mapper.selectByZoneName(zoneName.value()).stream().map(PlantingDbo::toEntity).toList();
    }

    @Override
    public List<Planting> getBySubZoneName(SubZoneName subZoneName) {
        Observer.forClass(PlantingEntityRepositoryRdbms.class)
                .arguments("getBySubZoneName", i -> i.identifier(subZoneName, "subZoneName"))
                .throwWhenInvalid();
        return mapper.selectBySubZoneName(subZoneName.value()).stream().map(PlantingDbo::toEntity).toList();
    }

    @Override
    public List<Planting> getByPlantName(PlantRankName plantName) {
        Observer.forClass(PlantingEntityRepositoryRdbms.class)
                .arguments("getByPlantName", i -> i.identifier(plantName, "plantName"))
                .throwWhenInvalid();
        return mapper.selectByPlantName(plantName.rank().name(), plantName.value()).stream()
                .map(PlantingDbo::toEntity).toList();
    }
}
