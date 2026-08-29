package com.naturalist.plants;

import com.naturalist.naturalist.NaturalistName;
import com.naturalist.observability.Constraints;
import com.naturalist.observability.Observer;
import com.naturalist.observation.Identification;
import com.naturalist.observation.OrganismObservation;
import com.naturalist.persistence.Dbo;
import com.naturalist.persistence.DboSchema;
import com.naturalist.taxonomy.LinealRank;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Persistence view of a plant {@link OrganismObservation} — a surrogate-UUID {@code Entity} keyed by
 * {@link PlantObservationId}. Its {@code subject} is a polymorphic {@link PlantRankName} stored as a
 * {@code subject_rank} discriminator + {@code subject} slug (no FK — it spans four rank tables) and
 * rebuilt in-module via {@link PlantRankName#of(String, LinealRank)}. {@code observedBy} is a
 * cross-domain naturalist slug (no FK). The nullable owned {@link Identification} flattens to the
 * {@code identification_confidence}/{@code identification_evidence} column group (both null when it is
 * absent); its {@code alternatives} are a child table ({@link PlantObservationCandidateDbo}), so the
 * adapter passes them to {@link #toEntity(List)}.
 */
@DboSchema(table = "plant_observation", primaryKey = "id", entity = OrganismObservation.class)
final class PlantObservationDbo implements Dbo {
    String id;             // the PlantObservationId's UUID as text; mapper casts it (::uuid)
    String observedBy;     // naturalist slug (cross-domain, no FK)
    String subjectRank;    // ORDER|FAMILY|GENUS|SPECIES
    String subject;        // rank-name slug (polymorphic, no FK)
    Instant observedOn;
    String notes;          // nullable
    String location;       // nullable
    Double identificationConfidence;  // nullable group: present iff the observation has an Identification
    String identificationEvidence;    // nullable group

    static PlantObservationDbo from(OrganismObservation<PlantObservationId, PlantRankName> o) {
        PlantObservationDbo d = new PlantObservationDbo();
        d.id = o.id().value().toString();
        d.observedBy = o.observedBy().value();
        d.subjectRank = o.subject().rank().name();
        d.subject = o.subject().value();
        d.observedOn = o.observedOn();
        d.notes = o.notes();
        d.location = o.location();
        Identification ident = o.identification();
        if (ident != null) {
            d.identificationConfidence = ident.confidence();
            d.identificationEvidence = ident.evidence();
        }
        Observer.forClass(PlantObservationDbo.class)
                .arguments("from", i -> i.observable(d, "dbo"))
                .throwWhenInvalid();
        return d;
    }

    OrganismObservation<PlantObservationId, PlantRankName> toEntity(List<Identification.Candidate> alternatives) {
        Identification identification = identificationConfidence == null ? null
                : new Identification(identificationConfidence, identificationEvidence, alternatives);
        return new OrganismObservation<>(
                PlantObservationId.of(UUID.fromString(id)),
                NaturalistName.of(observedBy),
                PlantRankName.of(subject, LinealRank.valueOf(subjectRank)),
                observedOn,
                notes,
                location,
                identification);
    }

    @Override
    public Consumer<? extends Constraints> invariants() {
        return c -> c
                .notBlank(id, "id")
                .notNull(observedBy, "observedBy").kebabFormat(observedBy, "observedBy")
                    .maxLength(observedBy, 64, "observedBy")
                .notBlank(subjectRank, "subjectRank").maxLength(subjectRank, 16, "subjectRank")
                .notNull(subject, "subject").kebabFormat(subject, "subject").maxLength(subject, 64, "subject")
                .notNull(observedOn, "observedOn");
    }
}
