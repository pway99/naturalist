package com.naturalist.plants;

import com.naturalist.observability.Constraints;
import com.naturalist.observability.Observer;
import com.naturalist.observation.Identification;
import com.naturalist.observation.OrganismObservation;
import com.naturalist.persistence.Dbo;
import com.naturalist.persistence.DboSchema;
import com.naturalist.persistence.Fk;

import java.util.function.Consumer;

/**
 * Child row for one runner-up {@link Identification.Candidate} of a plant observation's vision
 * identification. Ordered within its parent by {@code ordinal} so the reconstructed
 * {@code List<Candidate>} preserves the model's ranking. The parent is a surrogate-UUID entity, so the
 * row carries the observation's UUID directly (FK-enforced) — no name indirection.
 */
@DboSchema(table = "plant_observation_candidate", primaryKey = "observation_id,ordinal",
           foreignKeys = @Fk(columns = "observation_id", references = "plant_observation(id)"),
           entity = OrganismObservation.class)
final class PlantObservationCandidateDbo implements Dbo {
    String observationId;  // parent UUID as text; mapper casts it (::uuid)
    int ordinal;
    String scientificName;
    String commonName;     // nullable
    double confidence;

    static PlantObservationCandidateDbo from(String observationId, int ordinal, Identification.Candidate c) {
        PlantObservationCandidateDbo d = new PlantObservationCandidateDbo();
        d.observationId = observationId;
        d.ordinal = ordinal;
        d.scientificName = c.scientificName();
        d.commonName = c.commonName();
        d.confidence = c.confidence();
        Observer.forClass(PlantObservationCandidateDbo.class)
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
