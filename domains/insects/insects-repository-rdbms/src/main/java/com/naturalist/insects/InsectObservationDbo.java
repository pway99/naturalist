package com.naturalist.insects;

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
 * Persistence view of an insect {@link OrganismObservation} — a surrogate-UUID {@code Entity} keyed by
 * {@link InsectObservationId}. Polymorphic {@link InsectRankName} subject stored as a {@code subject_rank}
 * discriminator + {@code subject} slug (no FK; up to 96 chars for a subspecies) and rebuilt in-module via
 * {@link InsectRankName#of(String, LinealRank)}. {@code observedBy} is a cross-domain naturalist slug (no
 * FK). The nullable owned {@link Identification} flattens to the confidence/evidence column group; its
 * {@code alternatives} are the {@link InsectObservationCandidateDbo} child table.
 */
@DboSchema(table = "insect_observation", primaryKey = "id", entity = OrganismObservation.class)
final class InsectObservationDbo implements Dbo {
    String id;
    String observedBy;
    String subjectRank;
    String subject;
    Instant observedOn;
    String notes;
    String location;
    Double identificationConfidence;
    String identificationEvidence;

    static InsectObservationDbo from(OrganismObservation<InsectObservationId, InsectRankName> o) {
        InsectObservationDbo d = new InsectObservationDbo();
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
        Observer.forClass(InsectObservationDbo.class)
                .arguments("from", i -> i.observable(d, "dbo"))
                .throwWhenInvalid();
        return d;
    }

    OrganismObservation<InsectObservationId, InsectRankName> toEntity(List<Identification.Candidate> alternatives) {
        Identification identification = identificationConfidence == null ? null
                : new Identification(identificationConfidence, identificationEvidence, alternatives);
        return new OrganismObservation<>(
                InsectObservationId.of(UUID.fromString(id)),
                NaturalistName.of(observedBy),
                InsectRankName.of(subject, LinealRank.valueOf(subjectRank)),
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
                .notNull(subject, "subject").kebabFormat(subject, "subject").maxLength(subject, 96, "subject")
                .notNull(observedOn, "observedOn");
    }
}
