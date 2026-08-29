package com.naturalist.plants.cultivar;

import com.naturalist.data.AbstractEntityRepository;
import com.naturalist.data.Page;
import com.naturalist.data.PageRequest;
import com.naturalist.exception.EntityNotFoundException;
import com.naturalist.exception.PrimaryKeyConstraintException;
import com.naturalist.infrastructure.DomainService;
import com.naturalist.observability.Observer;
import com.naturalist.persistence.RdbmsExceptions;
import com.naturalist.plants.PlantSpeciesName;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * RDBMS adapter for {@link Cultivar} — a flat {@code NamedEntity} carrying one within-domain
 * reference (its {@code PlantSpecies}). {@code id} is DB-generated; identity at the port is the
 * slug. Writes nested-select the {@code plant_species_id} from the species slug, so a 0-row insert
 * (species absent) surfaces as {@code EntityNotFoundException}; a unique-name collision surfaces as
 * {@code PrimaryKeyConstraintException}.
 */
@DomainService
class CultivarEntityRepositoryRdbms
        extends AbstractEntityRepository<CultivarName, Cultivar>
        implements CultivarRepository {

    private final CultivarMapper mapper;

    CultivarEntityRepositoryRdbms(CultivarMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    protected Optional<Cultivar> doGetByName(CultivarName name) {
        return Optional.ofNullable(mapper.selectByName(name.value())).map(CultivarDbo::toEntity);
    }

    @Override
    protected List<Cultivar> doGetByNameSet(Set<CultivarName> nameSet) {
        if (nameSet.isEmpty()) return List.of();
        List<String> slugs = nameSet.stream().map(CultivarName::value).toList();
        return mapper.selectByNameSet(slugs).stream().map(CultivarDbo::toEntity).toList();
    }

    @Override
    protected Page<Cultivar> doGetPage(PageRequest request) {
        int pageSize = request.pageSize();
        List<CultivarDbo> rows = mapper.selectPage(pageSize + 1, request.offset());
        boolean immediateMore = rows.size() > pageSize;
        List<Cultivar> content = rows.stream().limit(pageSize).map(CultivarDbo::toEntity).toList();

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
    protected void doInsert(Cultivar entity) {
        CultivarDbo dbo = CultivarDbo.from(entity);   // validates + throws before the DB
        int rows;
        try {
            rows = mapper.insert(dbo);
        } catch (RuntimeException e) {
            if (RdbmsExceptions.isUniqueViolation(e)) throw new PrimaryKeyConstraintException(entity);
            throw e;
        }
        if (rows == 0) throw new EntityNotFoundException(entity);   // referenced species absent
    }

    @Override
    protected void doUpdate(Cultivar entity) {
        CultivarDbo dbo = CultivarDbo.from(entity);
        if (mapper.updateByName(dbo) == 0) throw new EntityNotFoundException(entity);
    }

    @Override
    protected Cultivar doSave(Cultivar entity) {
        if (mapper.selectByName(entity.name().value()) != null) doUpdate(entity);
        else doInsert(entity);
        return entity;
    }

    @Override
    public List<Cultivar> getByPlantName(PlantSpeciesName plantSpeciesName) {
        Observer.forClass(CultivarEntityRepositoryRdbms.class)
                .arguments("getByPlantName", i -> i.entityName(plantSpeciesName, "plantName"))
                .throwWhenInvalid();
        return mapper.selectByPlantName(plantSpeciesName.value()).stream().map(CultivarDbo::toEntity).toList();
    }
}
