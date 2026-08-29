package com.naturalist.plants.heritage;

import com.naturalist.data.AbstractEntityRepository;
import com.naturalist.data.Page;
import com.naturalist.data.PageRequest;
import com.naturalist.exception.EntityNotFoundException;
import com.naturalist.exception.PrimaryKeyConstraintException;
import com.naturalist.infrastructure.DomainService;
import com.naturalist.observability.Observer;
import com.naturalist.persistence.RdbmsExceptions;
import com.naturalist.plants.cultivar.CultivarName;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * RDBMS adapter for {@link SeedLineage} — a flat {@code NamedEntity} carrying one within-domain
 * reference (its {@code Cultivar}) plus a flattened {@code Provenance}. {@code id} is DB-generated;
 * identity at the port is the slug. Writes nested-select the {@code cultivar_id} from the cultivar
 * slug, so a 0-row insert (cultivar absent) surfaces as {@code EntityNotFoundException}; a
 * unique-name collision surfaces as {@code PrimaryKeyConstraintException}.
 */
@DomainService
class SeedLineageEntityRepositoryRdbms
        extends AbstractEntityRepository<SeedLineageName, SeedLineage>
        implements SeedLineageRepository {

    private final SeedLineageMapper mapper;

    SeedLineageEntityRepositoryRdbms(SeedLineageMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    protected Optional<SeedLineage> doGetByName(SeedLineageName name) {
        return Optional.ofNullable(mapper.selectByName(name.value())).map(SeedLineageDbo::toEntity);
    }

    @Override
    protected List<SeedLineage> doGetByNameSet(Set<SeedLineageName> nameSet) {
        if (nameSet.isEmpty()) return List.of();
        List<String> slugs = nameSet.stream().map(SeedLineageName::value).toList();
        return mapper.selectByNameSet(slugs).stream().map(SeedLineageDbo::toEntity).toList();
    }

    @Override
    protected Page<SeedLineage> doGetPage(PageRequest request) {
        int pageSize = request.pageSize();
        List<SeedLineageDbo> rows = mapper.selectPage(pageSize + 1, request.offset());
        boolean immediateMore = rows.size() > pageSize;
        List<SeedLineage> content = rows.stream().limit(pageSize).map(SeedLineageDbo::toEntity).toList();

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
    protected void doInsert(SeedLineage entity) {
        SeedLineageDbo dbo = SeedLineageDbo.from(entity);   // validates + throws before the DB
        int rows;
        try {
            rows = mapper.insert(dbo);
        } catch (RuntimeException e) {
            if (RdbmsExceptions.isUniqueViolation(e)) throw new PrimaryKeyConstraintException(entity);
            throw e;
        }
        if (rows == 0) throw new EntityNotFoundException(entity);   // referenced cultivar absent
    }

    @Override
    protected void doUpdate(SeedLineage entity) {
        SeedLineageDbo dbo = SeedLineageDbo.from(entity);
        if (mapper.updateByName(dbo) == 0) throw new EntityNotFoundException(entity);
    }

    @Override
    protected SeedLineage doSave(SeedLineage entity) {
        if (mapper.selectByName(entity.name().value()) != null) doUpdate(entity);
        else doInsert(entity);
        return entity;
    }

    @Override
    public List<SeedLineage> getByCultivarName(CultivarName cultivarName) {
        Observer.forClass(SeedLineageEntityRepositoryRdbms.class)
                .arguments("getByCultivarName", i -> i.entityName(cultivarName, "cultivarName"))
                .throwWhenInvalid();
        return mapper.selectByCultivarName(cultivarName.value()).stream().map(SeedLineageDbo::toEntity).toList();
    }
}
