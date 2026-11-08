package com.naturalist.insects;

import com.naturalist.exception.EntityNotFoundException;
import com.naturalist.exception.PrimaryKeyConstraintException;
import com.naturalist.infrastructure.DomainService;
import com.naturalist.observation.Identification;
import com.naturalist.observation.OrganismObservation;
import com.naturalist.persistence.RdbmsExceptions;
import jakarta.transaction.Transactional;

import java.util.List;

/**
 * The multi-statement writes of {@link InsectObservationEntityRepositoryRdbms}, each run as one
 * transaction: the parent row and its child rows commit or roll back together, so a failed child
 * insert leaves no partial row behind and a failed update does not leave the children it deleted
 * gone.
 *
 * <p>A separate bean, not annotations on the adapter: the adapter's write entry points are the
 * {@code final} {@code insert} / {@code update} / {@code save} of
 * {@link com.naturalist.data.AbstractEntityRepository}, which CGLIB cannot override, and their
 * calls to {@code doInsert} et al. are self-invocations no proxy sees. Nothing here is
 * {@code final}, for the same reason. Inside a caller's transaction these join it. Constructed
 * by hand (the ITs and the seeder) no proxy exists and the caller's own {@code SqlSession} is
 * the transaction.
 */
@DomainService
class InsectObservationTransaction {

    private final InsectObservationMapper mapper;

    InsectObservationTransaction(InsectObservationMapper mapper) {
        this.mapper = mapper;
    }

    /** The parent row, then every child row. */
    @Transactional
    void insert(OrganismObservation<InsectObservationId, InsectRankName> entity) {
        InsectObservationDbo dbo = InsectObservationDbo.from(entity);
        try {
            mapper.insert(dbo);
        } catch (RuntimeException e) {
            if (RdbmsExceptions.isUniqueViolation(e)) throw new PrimaryKeyConstraintException(entity);
            throw e;
        }
        insertCandidates(entity);
    }

    /** The parent row, then its child rows replaced wholesale. */
    @Transactional
    void update(OrganismObservation<InsectObservationId, InsectRankName> entity) {
        InsectObservationDbo dbo = InsectObservationDbo.from(entity);
        if (mapper.updateById(dbo) == 0) throw new EntityNotFoundException(entity);
        mapper.deleteCandidates(entity.id().value().toString());
        insertCandidates(entity);
    }

    /** The existence check and the write it chooses, in one transaction. */
    @Transactional
    void save(OrganismObservation<InsectObservationId, InsectRankName> entity) {
        if (mapper.selectById(entity.id().value().toString()) != null) update(entity);
        else insert(entity);
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
}
