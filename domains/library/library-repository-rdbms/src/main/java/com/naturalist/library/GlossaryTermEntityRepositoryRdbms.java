package com.naturalist.library;

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
 * RDBMS adapter for {@link GlossaryTerm} — a flat, reference-free {@code NamedEntity}, mirroring
 * the chemistry {@code Element} adapter. {@code id} is DB-generated; identity at the port is the slug.
 */
@DomainService
class GlossaryTermEntityRepositoryRdbms
        extends AbstractEntityRepository<GlossaryTermName, GlossaryTerm>
        implements GlossaryTermRepository {

    private final GlossaryTermMapper mapper;

    GlossaryTermEntityRepositoryRdbms(GlossaryTermMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    protected Optional<GlossaryTerm> doGetByName(GlossaryTermName name) {
        return Optional.ofNullable(mapper.selectByName(name.value())).map(GlossaryTermDbo::toEntity);
    }

    @Override
    protected List<GlossaryTerm> doGetByNameSet(Set<GlossaryTermName> nameSet) {
        if (nameSet.isEmpty()) return List.of();
        List<String> slugs = nameSet.stream().map(GlossaryTermName::value).toList();
        return mapper.selectByNameSet(slugs).stream().map(GlossaryTermDbo::toEntity).toList();
    }

    @Override
    protected Page<GlossaryTerm> doGetPage(PageRequest request) {
        int pageSize = request.pageSize();
        List<GlossaryTermDbo> rows = mapper.selectPage(pageSize + 1, request.offset());
        boolean immediateMore = rows.size() > pageSize;
        List<GlossaryTerm> content = rows.stream().limit(pageSize).map(GlossaryTermDbo::toEntity).toList();

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
    protected void doInsert(GlossaryTerm entity) {
        GlossaryTermDbo dbo = GlossaryTermDbo.from(entity);   // validates + throws before the DB
        try {
            mapper.insert(dbo);
        } catch (RuntimeException e) {
            if (RdbmsExceptions.isUniqueViolation(e)) throw new PrimaryKeyConstraintException(entity);
            throw e;
        }
    }

    @Override
    protected void doUpdate(GlossaryTerm entity) {
        GlossaryTermDbo dbo = GlossaryTermDbo.from(entity);
        if (mapper.updateByName(dbo) == 0) throw new EntityNotFoundException(entity);
    }

    @Override
    protected GlossaryTerm doSave(GlossaryTerm entity) {
        if (mapper.selectByName(entity.name().value()) != null) doUpdate(entity);
        else doInsert(entity);
        return entity;
    }
}
