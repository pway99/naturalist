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
 * RDBMS adapter for {@link Concept} — a flat, reference-free {@code NamedEntity}, so it mirrors
 * the chemistry {@code Element} adapter directly. {@code id} is DB-generated; identity at the port
 * is the slug.
 */
@DomainService
class ConceptEntityRepositoryRdbms
        extends AbstractEntityRepository<ConceptName, Concept>
        implements ConceptRepository {

    private final ConceptMapper mapper;

    ConceptEntityRepositoryRdbms(ConceptMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    protected Optional<Concept> doGetByName(ConceptName name) {
        return Optional.ofNullable(mapper.selectByName(name.value())).map(ConceptDbo::toEntity);
    }

    @Override
    protected List<Concept> doGetByNameSet(Set<ConceptName> nameSet) {
        if (nameSet.isEmpty()) return List.of();
        List<String> slugs = nameSet.stream().map(ConceptName::value).toList();
        return mapper.selectByNameSet(slugs).stream().map(ConceptDbo::toEntity).toList();
    }

    @Override
    protected Page<Concept> doGetPage(PageRequest request) {
        int pageSize = request.pageSize();
        List<ConceptDbo> rows = mapper.selectPage(pageSize + 1, request.offset());
        boolean immediateMore = rows.size() > pageSize;
        List<Concept> content = rows.stream().limit(pageSize).map(ConceptDbo::toEntity).toList();

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
    protected void doInsert(Concept entity) {
        ConceptDbo dbo = ConceptDbo.from(entity);   // validates + throws before the DB
        try {
            mapper.insert(dbo);
        } catch (RuntimeException e) {
            if (RdbmsExceptions.isUniqueViolation(e)) throw new PrimaryKeyConstraintException(entity);
            throw e;
        }
    }

    @Override
    protected void doUpdate(Concept entity) {
        ConceptDbo dbo = ConceptDbo.from(entity);
        if (mapper.updateByName(dbo) == 0) throw new EntityNotFoundException(entity);
    }

    @Override
    protected Concept doSave(Concept entity) {
        if (mapper.selectByName(entity.name().value()) != null) doUpdate(entity);
        else doInsert(entity);
        return entity;
    }
}
