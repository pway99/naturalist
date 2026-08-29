package com.naturalist.library;

import com.naturalist.authority.CitationName;
import com.naturalist.catalog.EntityRef;
import com.naturalist.data.AbstractEntityRepository;
import com.naturalist.data.Page;
import com.naturalist.data.PageRequest;
import com.naturalist.exception.EntityNotFoundException;
import com.naturalist.exception.PrimaryKeyConstraintException;
import com.naturalist.infrastructure.DomainService;
import com.naturalist.observability.Observer;
import com.naturalist.persistence.RdbmsExceptions;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * RDBMS adapter for {@link CitationAssociation} — a surrogate-UUID entity keyed by
 * {@link CitationAssociationId}. Its citation reference is the numeric {@code citation_id}, resolved
 * from the citation name by the mapper's nested-select on write and recovered by JOIN on read; an
 * association for a missing citation writes zero rows, which becomes an {@link EntityNotFoundException}.
 * Its cross-domain {@code subject} is stored flat and reconstructed through the injected
 * {@link EntityRefResolver}. {@link #getBySubjects} resolves a whole subject set in one query.
 */
@DomainService
class CitationAssociationEntityRepositoryRdbms
        extends AbstractEntityRepository<CitationAssociationId, CitationAssociation>
        implements CitationAssociationRepository {

    private final CitationAssociationMapper mapper;
    private final EntityRefResolver resolver;

    CitationAssociationEntityRepositoryRdbms(CitationAssociationMapper mapper, EntityRefResolver resolver) {
        this.mapper = mapper;
        this.resolver = resolver;
    }

    @Override
    protected Optional<CitationAssociation> doGetByName(CitationAssociationId id) {
        return Optional.ofNullable(mapper.selectById(id.value().toString())).map(d -> d.toEntity(resolver));
    }

    @Override
    protected List<CitationAssociation> doGetByNameSet(Set<CitationAssociationId> nameSet) {
        if (nameSet.isEmpty()) return List.of();
        List<String> ids = nameSet.stream().map(id -> id.value().toString()).toList();
        return mapper.selectByIdSet(ids).stream().map(d -> d.toEntity(resolver)).toList();
    }

    @Override
    protected Page<CitationAssociation> doGetPage(PageRequest request) {
        int pageSize = request.pageSize();
        List<CitationAssociationDbo> rows = mapper.selectPage(pageSize + 1, request.offset());
        boolean immediateMore = rows.size() > pageSize;
        List<CitationAssociation> content =
                rows.stream().limit(pageSize).map(d -> d.toEntity(resolver)).toList();

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
    protected void doInsert(CitationAssociation entity) {
        CitationAssociationDbo dbo = CitationAssociationDbo.from(entity, resolver);   // validates + throws
        int inserted;
        try {
            inserted = mapper.insert(dbo);
        } catch (RuntimeException e) {
            if (RdbmsExceptions.isUniqueViolation(e)) throw new PrimaryKeyConstraintException(entity);
            throw e;
        }
        if (inserted == 0) throw new EntityNotFoundException(entity);   // referenced citation absent
    }

    @Override
    protected void doUpdate(CitationAssociation entity) {
        CitationAssociationDbo dbo = CitationAssociationDbo.from(entity, resolver);
        if (mapper.updateById(dbo) == 0) throw new EntityNotFoundException(entity);
    }

    @Override
    protected CitationAssociation doSave(CitationAssociation entity) {
        if (mapper.selectById(entity.id().value().toString()) != null) doUpdate(entity);
        else doInsert(entity);
        return entity;
    }

    @Override
    public List<CitationAssociation> getByCitationName(CitationName citationName) {
        Observer.forClass(CitationAssociationEntityRepositoryRdbms.class)
                .arguments("getByCitationName", i -> i.entityName(citationName, "citationName"))
                .throwWhenInvalid();
        return mapper.selectByCitationName(citationName.value()).stream()
                .map(d -> d.toEntity(resolver)).toList();
    }

    @Override
    public List<CitationAssociation> getBySubject(EntityRef subject) {
        Observer.forClass(CitationAssociationEntityRepositoryRdbms.class)
                .arguments("getBySubject", i -> i.valueObject(subject, "subject"))
                .throwWhenInvalid();
        return mapper.selectBySubject(
                        subject.domain().value(), resolver.rankOf(subject), subject.name().value()).stream()
                .map(d -> d.toEntity(resolver)).toList();
    }

    @Override
    public List<CitationAssociation> getBySubjects(Set<EntityRef> subjects) {
        Observer.forClass(CitationAssociationEntityRepositoryRdbms.class)
                .arguments("getBySubjects", i -> i.observableCollection(subjects, "subjects"))
                .throwWhenInvalid();
        if (subjects.isEmpty()) return List.of();
        List<CitationSubjectKey> keys = subjects.stream()
                .map(s -> new CitationSubjectKey(s.domain().value(), resolver.rankOf(s), s.name().value()))
                .toList();
        return mapper.selectBySubjects(keys).stream().map(d -> d.toEntity(resolver)).toList();
    }
}
