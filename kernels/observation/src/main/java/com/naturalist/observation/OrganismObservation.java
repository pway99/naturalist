package com.naturalist.observation;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.naturalist.ddd.Entity;
import com.naturalist.ddd.EntityId;
import com.naturalist.naturalist.NaturalistName;
import com.naturalist.observability.Constraints;
import com.naturalist.taxonomy.RankName;
import com.naturalist.taxonomy.RankNameDeserializer;
import com.naturalist.taxonomy.RankNameSerializer;
import org.jspecify.annotations.Nullable;

import java.time.Instant;
import java.util.function.Consumer;

/**
 * A naturalist's field observation of an organism — the shared collection unit across
 * organism domains. Records that a naturalist ({@code observedBy}) encountered an organism
 * at a taxonomic rank ({@code subject}) at a point in time. Photographic evidence is optional
 * and lives on each domain's image entity via an observation link. {@code identification}
 * carries an optional machine (vision) result; a manual sighting leaves it null.
 *
 * <p>{@code subject} is a domain permit ({@code InsectSpeciesName}, {@code PlantGenusName}, …)
 * widened to {@link RankName}. It serializes as a self-describing {@code {"rank":…,"value":…}}
 * object; deserialization rebuilds the concrete permit through the {@code RankNameReconstructor}
 * registered on the reading mapper (see the domain's observation test-entity source).
 */
public record OrganismObservation<ID extends EntityId>(
        ID id,
        NaturalistName observedBy,
        @JsonSerialize(using = RankNameSerializer.class)
        @JsonDeserialize(using = RankNameDeserializer.class)
        RankName subject,
        Instant observedOn,
        @Nullable String notes,
        @Nullable String location,
        @Nullable Identification identification
) implements Entity<ID> {

    @Override
    public Consumer<? extends Constraints> invariants() {
        return i -> i
                .entityId(id, "id")
                .identifier(observedBy, "observedBy")
                .identifier(subject, "subject")
                .notNull(observedOn, "observedOn")
                .valueObjectOrNull(identification, "identification");
    }

    public OrganismObservation<ID> withNotes(@Nullable String notes) {
        return new OrganismObservation<>(id, observedBy, subject, observedOn, notes, location, identification);
    }

    public OrganismObservation<ID> withSubject(RankName subject) {
        return new OrganismObservation<>(id, observedBy, subject, observedOn, notes, location, identification);
    }
}
