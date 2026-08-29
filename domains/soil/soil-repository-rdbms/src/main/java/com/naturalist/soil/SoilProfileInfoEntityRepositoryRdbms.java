package com.naturalist.soil;

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
 * RDBMS adapter for {@link SoilProfileInfo} — a flat, reference-free {@code NamedEntity} (the two
 * zone slugs are stored flat, no FK). {@code id} is DB-generated; identity at the port is the slug.
 * Mirrors the library {@code Concept} adapter directly.
 */
@DomainService
class SoilProfileInfoEntityRepositoryRdbms
        extends AbstractEntityRepository<SoilProfileName, SoilProfileInfo>
        implements SoilProfileInfoRepository {

    private final SoilProfileInfoMapper mapper;

    SoilProfileInfoEntityRepositoryRdbms(SoilProfileInfoMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    protected Optional<SoilProfileInfo> doGetByName(SoilProfileName name) {
        return Optional.ofNullable(mapper.selectByName(name.value())).map(SoilProfileInfoDbo::toEntity);
    }

    @Override
    protected List<SoilProfileInfo> doGetByNameSet(Set<SoilProfileName> nameSet) {
        if (nameSet.isEmpty()) return List.of();
        List<String> slugs = nameSet.stream().map(SoilProfileName::value).toList();
        return mapper.selectByNameSet(slugs).stream().map(SoilProfileInfoDbo::toEntity).toList();
    }

    @Override
    protected Page<SoilProfileInfo> doGetPage(PageRequest request) {
        int pageSize = request.pageSize();
        List<SoilProfileInfoDbo> rows = mapper.selectPage(pageSize + 1, request.offset());
        boolean immediateMore = rows.size() > pageSize;
        List<SoilProfileInfo> content = rows.stream().limit(pageSize).map(SoilProfileInfoDbo::toEntity).toList();

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
    protected void doInsert(SoilProfileInfo entity) {
        SoilProfileInfoDbo dbo = SoilProfileInfoDbo.from(entity);   // validates + throws before the DB
        try {
            mapper.insert(dbo);
        } catch (RuntimeException e) {
            if (RdbmsExceptions.isUniqueViolation(e)) throw new PrimaryKeyConstraintException(entity);
            throw e;
        }
    }

    @Override
    protected void doUpdate(SoilProfileInfo entity) {
        SoilProfileInfoDbo dbo = SoilProfileInfoDbo.from(entity);
        if (mapper.updateByName(dbo) == 0) throw new EntityNotFoundException(entity);
    }

    @Override
    protected SoilProfileInfo doSave(SoilProfileInfo entity) {
        if (mapper.selectByName(entity.name().value()) != null) doUpdate(entity);
        else doInsert(entity);
        return entity;
    }
}
