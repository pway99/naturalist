package com.naturalist.plants;

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
 * RDBMS adapter for plant {@link OrganismObservation}s — a surrogate-UUID entity whose only
 * multi-valued member is the identification's {@code alternatives}, loaded one batched query per page
 * (keyed on the observation-id set), so a page costs two selects, not two-per-observation. The
 * polymorphic {@code subject} and the naturalist {@code observedBy} are stored flat and rebuilt
 * in-module. {@code getByNaturalistAndSubjects} resolves a whole subject set in one query.
 */
@DomainService
class PlantObservationEntityRepositoryRdbms
        extends AbstractEntityRepository<PlantObservationId, OrganismObservation<PlantObservationId, PlantRankName>>
        implements PlantRepository.ObservationRepository {

    private final PlantObservationMapper mapper;

    PlantObservationEntityRepositoryRdbms(PlantObservationMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    protected Optional<OrganismObservation<PlantObservationId, PlantRankName>> doGetByName(PlantObservationId id) {
        PlantObservationDbo dbo = mapper.selectById(id.value().toString());
        if (dbo == null) return Optional.empty();
        return Optional.of(assemble(List.of(dbo)).get(0));
    }

    @Override
    protected List<OrganismObservation<PlantObservationId, PlantRankName>> doGetByNameSet(
            Set<PlantObservationId> nameSet) {
        if (nameSet.isEmpty()) return List.of();
        List<String> ids = nameSet.stream().map(id -> id.value().toString()).toList();
        return assemble(mapper.selectByIdSet(ids));
    }

    @Override
    protected Page<OrganismObservation<PlantObservationId, PlantRankName>> doGetPage(PageRequest request) {
        int pageSize = request.pageSize();
        List<PlantObservationDbo> rows = mapper.selectPage(pageSize + 1, request.offset());
        boolean immediateMore = rows.size() > pageSize;
        List<OrganismObservation<PlantObservationId, PlantRankName>> content =
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
    protected void doInsert(OrganismObservation<PlantObservationId, PlantRankName> entity) {
        PlantObservationDbo dbo = PlantObservationDbo.from(entity);   // validates + throws
        try {
            mapper.insert(dbo);
        } catch (RuntimeException e) {
            if (RdbmsExceptions.isUniqueViolation(e)) throw new PrimaryKeyConstraintException(entity);
            throw e;
        }
        insertCandidates(entity);
    }

    @Override
    protected void doUpdate(OrganismObservation<PlantObservationId, PlantRankName> entity) {
        PlantObservationDbo dbo = PlantObservationDbo.from(entity);
        if (mapper.updateById(dbo) == 0) throw new EntityNotFoundException(entity);
        mapper.deleteCandidates(entity.id().value().toString());
        insertCandidates(entity);
    }

    @Override
    protected OrganismObservation<PlantObservationId, PlantRankName> doSave(
            OrganismObservation<PlantObservationId, PlantRankName> entity) {
        if (mapper.selectById(entity.id().value().toString()) != null) doUpdate(entity);
        else doInsert(entity);
        return entity;
    }

    @Override
    public List<OrganismObservation<PlantObservationId, PlantRankName>> getByNaturalist(NaturalistName observedBy) {
        Observer.forClass(PlantObservationEntityRepositoryRdbms.class)
                .arguments("getByNaturalist", i -> i.identifier(observedBy, "observedBy"))
                .throwWhenInvalid();
        return assemble(mapper.selectByNaturalist(observedBy.value()));
    }

    @Override
    public List<OrganismObservation<PlantObservationId, PlantRankName>> getByNaturalistAndSubjects(
            NaturalistName observedBy, Set<PlantRankName> subjects) {
        Observer.forClass(PlantObservationEntityRepositoryRdbms.class)
                .arguments("getByNaturalistAndSubjects", i -> i.identifier(observedBy, "observedBy"))
                .throwWhenInvalid();
        if (subjects.isEmpty()) return List.of();
        List<ObservationSubjectKey> keys = subjects.stream()
                .map(s -> new ObservationSubjectKey(s.rank().name(), s.value()))
                .toList();
        return assemble(mapper.selectByNaturalistAndSubjects(observedBy.value(), keys));
    }

    private void insertCandidates(OrganismObservation<PlantObservationId, PlantRankName> entity) {
        Identification identification = entity.identification();
        if (identification == null) return;
        String id = entity.id().value().toString();
        List<Identification.Candidate> alternatives = identification.alternatives();
        for (int i = 0; i < alternatives.size(); i++) {
            mapper.insertCandidate(PlantObservationCandidateDbo.from(id, i, alternatives.get(i)));
        }
    }

    /** Assemble observations from parent rows + one batched candidate load, grouped/ordered by ordinal. */
    private List<OrganismObservation<PlantObservationId, PlantRankName>> assemble(List<PlantObservationDbo> parents) {
        if (parents.isEmpty()) return List.of();
        Set<String> ids = parents.stream().map(p -> p.id).collect(Collectors.toSet());
        Map<String, List<Identification.Candidate>> candidates = mapper.selectCandidates(ids).stream()
                .collect(Collectors.groupingBy(c -> c.observationId,
                        Collectors.mapping(PlantObservationCandidateDbo::toCandidate, Collectors.toList())));
        return parents.stream()
                .map(p -> p.toEntity(candidates.getOrDefault(p.id, List.of())))
                .toList();
    }
}
