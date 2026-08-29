package com.naturalist.plants.management;

import com.naturalist.data.AbstractEntityRepository;
import com.naturalist.data.Page;
import com.naturalist.data.PageRequest;
import com.naturalist.exception.EntityNotFoundException;
import com.naturalist.exception.PrimaryKeyConstraintException;
import com.naturalist.infrastructure.DomainService;
import com.naturalist.observability.Observer;
import com.naturalist.persistence.RdbmsExceptions;
import com.naturalist.plants.PlantRankName;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * RDBMS adapter for {@link PlantProgram} — a flat {@code NamedEntity} whose {@code plantName} is a
 * polymorphic cross-rank {@link PlantRankName}, stored as the {@code (plant_rank, plant_name)} pair
 * with no FK. {@code id} is DB-generated; identity at the port is the slug. Inserts are a plain
 * {@code VALUES} write (no parent to nested-select), so a unique-name collision surfaces as
 * {@code PrimaryKeyConstraintException} and a 0-row update as {@code EntityNotFoundException}.
 */
@DomainService
class PlantProgramEntityRepositoryRdbms
        extends AbstractEntityRepository<PlantProgramName, PlantProgram>
        implements PlantProgramRepository {

    private final PlantProgramMapper mapper;

    PlantProgramEntityRepositoryRdbms(PlantProgramMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    protected Optional<PlantProgram> doGetByName(PlantProgramName name) {
        return Optional.ofNullable(mapper.selectByName(name.value())).map(PlantProgramDbo::toEntity);
    }

    @Override
    protected List<PlantProgram> doGetByNameSet(Set<PlantProgramName> nameSet) {
        if (nameSet.isEmpty()) return List.of();
        List<String> slugs = nameSet.stream().map(PlantProgramName::value).toList();
        return mapper.selectByNameSet(slugs).stream().map(PlantProgramDbo::toEntity).toList();
    }

    @Override
    protected Page<PlantProgram> doGetPage(PageRequest request) {
        int pageSize = request.pageSize();
        List<PlantProgramDbo> rows = mapper.selectPage(pageSize + 1, request.offset());
        boolean immediateMore = rows.size() > pageSize;
        List<PlantProgram> content = rows.stream().limit(pageSize).map(PlantProgramDbo::toEntity).toList();

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
    protected void doInsert(PlantProgram entity) {
        PlantProgramDbo dbo = PlantProgramDbo.from(entity);   // validates + throws before the DB
        try {
            mapper.insert(dbo);
        } catch (RuntimeException e) {
            if (RdbmsExceptions.isUniqueViolation(e)) throw new PrimaryKeyConstraintException(entity);
            throw e;
        }
    }

    @Override
    protected void doUpdate(PlantProgram entity) {
        PlantProgramDbo dbo = PlantProgramDbo.from(entity);
        if (mapper.updateByName(dbo) == 0) throw new EntityNotFoundException(entity);
    }

    @Override
    protected PlantProgram doSave(PlantProgram entity) {
        if (mapper.selectByName(entity.name().value()) != null) doUpdate(entity);
        else doInsert(entity);
        return entity;
    }

    @Override
    public List<PlantProgram> getByPlantName(PlantRankName plantRankName) {
        Observer.forClass(PlantProgramEntityRepositoryRdbms.class)
                .arguments("getByPlantName", i -> i.identifier(plantRankName, "plantName"))
                .throwWhenInvalid();
        return mapper.selectByPlantName(plantRankName.rank().name(), plantRankName.value())
                .stream().map(PlantProgramDbo::toEntity).toList();
    }
}
