package com.naturalist.insects;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.As;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import com.naturalist.ddd.Entity;
import com.naturalist.naturalist.NaturalistName;
import com.naturalist.observability.Constraints;
import org.jspecify.annotations.Nullable;

import java.time.Instant;
import java.util.function.Consumer;

/**
 * A naturalist's field observation of an insect — the unit of a naturalist's collection.
 * <p>
 * Records that a naturalist ({@code observedBy}) encountered an insect at a taxonomic rank
 * ({@code subject}) at a point in time. Photographic evidence is optional and lives on
 * {@link InsectImage} via its {@code observationId} link — an observation needs no photo.
 * "Insects I've collected" is the distinct set of {@code subject}s across a naturalist's
 * observations. Multiple observations of the same subject are allowed (distinct sightings).
 */
public record FieldObservation(
        FieldObservationId id,
        NaturalistName observedBy,
        @JsonTypeInfo(use = Id.NAME, property = "subjectRank", include = As.EXTERNAL_PROPERTY)
        @JsonSubTypes({
                @Type(value = InsectOrderName.class, name = "ORDER"),
                @Type(value = InsectFamilyName.class, name = "FAMILY"),
                @Type(value = InsectGenusName.class, name = "GENUS"),
                @Type(value = InsectSpeciesName.class, name = "SPECIES"),
                @Type(value = InsectSubspeciesName.class, name = "SUBSPECIES")
        })
        InsectRankName subject,
        Instant observedOn,
        @Nullable String notes
) implements Entity<FieldObservationId> {

    @Override
    public Consumer<? extends Constraints> invariants() {
        return i -> i
                .entityId(id, "id")
                .identifier(observedBy, "observedBy")
                .identifier(subject, "subject")
                .notNull(observedOn, "observedOn");
    }
}
