package com.naturalist.insects;

import com.naturalist.observability.Constraints;
import com.naturalist.observability.Observer;
import com.naturalist.observation.Identification;
import com.naturalist.observation.OrganismObservation;
import com.naturalist.persistence.Dbo;
import com.naturalist.persistence.DboSchema;
import com.naturalist.persistence.Fk;

import java.util.function.Consumer;

/**
 * Child row for one runner-up {@link Identification.Candidate} of an insect observation's vision
 * identification, ordered within its parent by {@code ordinal}. The parent is a surrogate-UUID entity, so
 * the row carries the observation's UUID directly (FK-enforced).
 */
@DboSchema(table = "insect_observation_candidate", primaryKey = "observation_id,ordinal",
           foreignKeys = @Fk(columns = "observation_id", references = "insect_observation(id)"),
           entity = OrganismObservation.class)
final class InsectObservationCandidateDbo implements Dbo {
    String observationId;
    int ordinal;
    String scientificName;
    String commonName;
    double confidence;

    static InsectObservationCandidateDbo from(String observationId, int ordinal, Identification.Candidate c) {
        InsectObservationCandidateDbo d = new InsectObservationCandidateDbo();
        d.observationId = observationId;
        d.ordinal = ordinal;
        d.scientificName = c.scientificName();
        d.commonName = c.commonName();
        d.confidence = c.confidence();
        Observer.forClass(InsectObservationCandidateDbo.class)
                .arguments("from", i -> i.observable(d, "dbo"))
                .throwWhenInvalid();
        return d;
    }

    Identification.Candidate toCandidate() {
        return new Identification.Candidate(scientificName, commonName, confidence);
    }

    @Override
    public Consumer<? extends Constraints> invariants() {
        return c -> c
                .notBlank(observationId, "observationId")
                .notBlank(scientificName, "scientificName");
    }
}
