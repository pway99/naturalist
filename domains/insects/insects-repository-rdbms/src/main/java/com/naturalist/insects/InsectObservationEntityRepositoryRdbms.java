package com.naturalist.insects;

import com.naturalist.data.AbstractEntityRepository;
import com.naturalist.data.Page;
import com.naturalist.data.PageRequest;
import com.naturalist.exception.EntityNotFoundException;
import com.naturalist.exception.PrimaryKeyConstraintException;
import com.naturalist.infrastructure.DomainService;
import com.naturalist.naturalist.NaturalistName;
import com.naturalist.observability.Observer;
import com.naturalist.observation.Identification;
import com.naturalist.observation.OrganismObservation;
import com.naturalist.persistence.RdbmsExceptions;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * RDBMS adapter for insect {@link OrganismObservation}s. The identification's {@code alternatives} load one
 * batched query per page (keyed on the observation-id set); the polymorphic {@code subject} and naturalist
 * {@code observedBy} are stored flat and rebuilt in-module. {@code getByNaturalistAndSubjects} resolves a
 * whole subject set in one query.
 */
@DomainService
class InsectObservationEntityRepositoryRdbms
        extends AbstractEntityRepository<InsectObservationId, OrganismObservation<InsectObservationId, InsectRankName>>
        implements InsectRepository.ObservationRepository {

    private final InsectObservationMapper mapper;

    InsectObservationEntityRepositoryRdbms(InsectObservationMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    protected Optional<OrganismObservation<InsectObservationId, InsectRankName>> doGetByName(InsectObservationId id) {
        InsectObservationDbo dbo = mapper.selectById(id.value().toString());
        if (dbo == null) return Optional.empty();
        return Optional.of(assemble(List.of(dbo)).get(0));
    }

    @Override
    protected List<OrganismObservation<InsectObservationId, InsectRankName>> doGetByNameSet(
            Set<InsectObservationId> nameSet) {
        if (nameSet.isEmpty()) return List.of();
        List<String> ids = nameSet.stream().map(id -> id.value().toString()).toList();
        return assemble(mapper.selectByIdSet(ids));
    }

    @Override
    protected Page<OrganismObservation<InsectObservationId, InsectRankName>> doGetPage(PageRequest request) {
        int pageSize = request.pageSize();
        List<InsectObservationDbo> rows = mapper.selectPage(pageSize + 1, request.offset());
        boolean immediateMore = rows.size() > pageSize;
        List<OrganismObservation<InsectObservationId, InsectRankName>> content =
                assemble(rows.stream().limit(pageSize).toList());

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
    protected void doInsert(OrganismObservation<InsectObservationId, InsectRankName> entity) {
        InsectObservationDbo dbo = InsectObservationDbo.from(entity);
        try {
            mapper.insert(dbo);
        } catch (RuntimeException e) {
            if (RdbmsExceptions.isUniqueViolation(e)) throw new PrimaryKeyConstraintException(entity);
            throw e;
        }
        insertCandidates(entity);
    }

    @Override
    protected void doUpdate(OrganismObservation<InsectObservationId, InsectRankName> entity) {
        InsectObservationDbo dbo = InsectObservationDbo.from(entity);
        if (mapper.updateById(dbo) == 0) throw new EntityNotFoundException(entity);
        mapper.deleteCandidates(entity.id().value().toString());
        insertCandidates(entity);
    }

    @Override
    protected OrganismObservation<InsectObservationId, InsectRankName> doSave(
            OrganismObservation<InsectObservationId, InsectRankName> entity) {
        if (mapper.selectById(entity.id().value().toString()) != null) doUpdate(entity);
        else doInsert(entity);
        return entity;
    }

    @Override
    public List<OrganismObservation<InsectObservationId, InsectRankName>> getByNaturalist(NaturalistName observedBy) {
        Observer.forClass(InsectObservationEntityRepositoryRdbms.class)
                .arguments("getByNaturalist", i -> i.identifier(observedBy, "observedBy"))
                .throwWhenInvalid();
        return assemble(mapper.selectByNaturalist(observedBy.value()));
    }

    @Override
    public List<OrganismObservation<InsectObservationId, InsectRankName>> getByNaturalistAndSubjects(
            NaturalistName observedBy, Set<InsectRankName> subjects) {
        Observer.forClass(InsectObservationEntityRepositoryRdbms.class)
                .arguments("getByNaturalistAndSubjects", i -> i.identifier(observedBy, "observedBy"))
                .throwWhenInvalid();
        if (subjects.isEmpty()) return List.of();
        List<InsectRankKey> keys = subjects.stream()
                .map(s -> new InsectRankKey(s.rank().name(), s.value()))
                .toList();
        return assemble(mapper.selectByNaturalistAndSubjects(observedBy.value(), keys));
    }

    private void insertCandidates(OrganismObservation<InsectObservationId, InsectRankName> entity) {
        Identification identification = entity.identification();
        if (identification == null) return;
        String id = entity.id().value().toString();
        List<Identification.Candidate> alternatives = identification.alternatives();
        for (int i = 0; i < alternatives.size(); i++) {
            mapper.insertCandidate(InsectObservationCandidateDbo.from(id, i, alternatives.get(i)));
        }
    }

    private List<OrganismObservation<InsectObservationId, InsectRankName>> assemble(List<InsectObservationDbo> parents) {
        if (parents.isEmpty()) return List.of();
        Set<String> ids = parents.stream().map(p -> p.id).collect(Collectors.toSet());
        Map<String, List<Identification.Candidate>> candidates = mapper.selectCandidates(ids).stream()
                .collect(Collectors.groupingBy(c -> c.observationId,
                        Collectors.mapping(InsectObservationCandidateDbo::toCandidate, Collectors.toList())));
        return parents.stream()
                .map(p -> p.toEntity(candidates.getOrDefault(p.id, List.of())))
                .toList();
    }
}
