package com.naturalist.plants;

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
 * A naturalist's field observation of a plant — the plant analogue of the insects
 * {@code FieldObservation}: a naturalist ({@code observedBy}) encountered a plant at a
 * taxonomic rank ({@code subject}) at a point in time. Photographic evidence is optional
 * and lives on {@link PlantImage} via its {@code observationId} link — an observation needs
 * no photo.
 * <p>
 * This is an <em>ephemeral sighting</em>, deliberately distinct from a persistent
 * managed individual (the {@code KnownOrganism} concept). Repeated sightings of the same
 * {@code subject} by the same naturalist are legal — distinct observations.
 * <p>
 * A vision-identification payload ({@code identification}) is deferred; it rides with the
 * write-side / vision work, matching the insects order of construction.
 */
public record FieldObservation(
        FieldObservationId id,
        NaturalistName observedBy,
        @JsonTypeInfo(use = Id.NAME, property = "subjectRank", include = As.EXTERNAL_PROPERTY)
        @JsonSubTypes({
                @Type(value = PlantOrderName.class, name = "ORDER"),
                @Type(value = PlantFamilyName.class, name = "FAMILY"),
                @Type(value = PlantGenusName.class, name = "GENUS"),
                @Type(value = PlantSpeciesName.class, name = "SPECIES")
        })
        PlantRankName subject,
        Instant observedOn,
        @Nullable String notes,
        @Nullable String location
) implements Entity<FieldObservationId> {

    @Override
    public Consumer<? extends Constraints> invariants() {
        return i -> i
                .entityId(id, "id")
                .identifier(observedBy, "observedBy")
                .identifier(subject, "subject")
                .notNull(observedOn, "observedOn");
    }

    public FieldObservation withNotes(@Nullable String notes) {
        return new FieldObservation(id, observedBy, subject, observedOn, notes, location);
    }

    public FieldObservation withSubject(PlantRankName subject) {
        return new FieldObservation(id, observedBy, subject, observedOn, notes, location);
    }
}
