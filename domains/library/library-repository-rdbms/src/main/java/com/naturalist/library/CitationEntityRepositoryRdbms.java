package com.naturalist.library;

import com.naturalist.authority.Citation;
import com.naturalist.authority.CitationName;
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
 * RDBMS adapter for {@link Citation} — the sealed {@code NamedEntity} from {@code kernels/authority}.
 * Reads/writes go through the single flat {@code citation} table; the sealed permit is restored from
 * the {@code kind} discriminator by {@link CitationDbo#toEntity()}. Reference-free (its authority
 * pointer is a flattened value object), so the shape mirrors the chemistry {@code Element} adapter.
 */
@DomainService
class CitationEntityRepositoryRdbms
        extends AbstractEntityRepository<CitationName, Citation>
        implements CitationRepository {

    private final CitationMapper mapper;

    CitationEntityRepositoryRdbms(CitationMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    protected Optional<Citation> doGetByName(CitationName name) {
        return Optional.ofNullable(mapper.selectByName(name.value())).map(CitationDbo::toEntity);
    }

    @Override
    protected List<Citation> doGetByNameSet(Set<CitationName> nameSet) {
        if (nameSet.isEmpty()) return List.of();
        List<String> slugs = nameSet.stream().map(CitationName::value).toList();
        return mapper.selectByNameSet(slugs).stream().map(CitationDbo::toEntity).toList();
    }

    @Override
    protected Page<Citation> doGetPage(PageRequest request) {
        int pageSize = request.pageSize();
        List<CitationDbo> rows = mapper.selectPage(pageSize + 1, request.offset());
        boolean immediateMore = rows.size() > pageSize;
        List<Citation> content = rows.stream().limit(pageSize).map(CitationDbo::toEntity).toList();

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
    protected void doInsert(Citation entity) {
        CitationDbo dbo = CitationDbo.from(entity);   // validates + throws before the DB
        try {
            mapper.insert(dbo);
        } catch (RuntimeException e) {
            if (RdbmsExceptions.isUniqueViolation(e)) throw new PrimaryKeyConstraintException(entity);
            throw e;
        }
    }

    @Override
    protected void doUpdate(Citation entity) {
        CitationDbo dbo = CitationDbo.from(entity);
        if (mapper.updateByName(dbo) == 0) throw new EntityNotFoundException(entity);
    }

    @Override
    protected Citation doSave(Citation entity) {
        if (mapper.selectByName(entity.name().value()) != null) doUpdate(entity);
        else doInsert(entity);
        return entity;
    }
}
