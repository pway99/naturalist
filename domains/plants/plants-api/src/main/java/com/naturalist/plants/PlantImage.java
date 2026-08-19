package com.naturalist.plants;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.As;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import com.naturalist.data.FileName;
import com.naturalist.ddd.Entity;
import com.naturalist.observability.Constraints;
import org.jspecify.annotations.Nullable;

import java.time.Instant;
import java.util.function.Consumer;

/**
 * A photographic record of a plant observed at Oak Vista — the plant analogue of the
 * insects {@code InsectImage}.
 * <p>
 * {@code PlantImage} is an immutable observation tied to a specific point in time. It does
 * not describe a taxonomic record; it records that a plant was seen and photographed. Its
 * identity is its UUID-based {@link PlantImageId}.
 * <p>
 * {@code parentName} is the typed slug of the parent taxonomic record — whichever rank
 * ({@link PlantOrder}, {@link PlantFamily}, {@link PlantGenus}, or {@link PlantSpecies})
 * the naturalist's confidence allows. The sealed {@link PlantRankName} marker statically
 * constrains the slot to the four plant-side rank names; cross-domain names cannot compile
 * in. Rank transitions ("we now know this is <i>Salvia apiana</i>, not just a Salvia")
 * become a single-field update.
 * <p>
 * Jackson dispatch lives on the {@code parentName} component, not on the
 * {@link PlantRankName} interface — placing the polymorphic envelope on the interface would
 * leak it into every direct leaf-class serialization site (e.g. {@code PlantSpecies.name}).
 * The {@link As#EXTERNAL_PROPERTY} form flattens the discriminator into a sibling
 * {@code "parentRank"} field, keeping {@code parentName} itself a plain slug string via
 * {@code EntityName}'s {@code @JsonValue}.
 * <p>
 * {@code observationId} is a nullable {@link PlantObservationId} link to the
 * {@link com.naturalist.observation.OrganismObservation} the photo was captured under.
 * {@code null} means a shared catalog image with no owning naturalist (the pre-collection default).
 * <p>
 * {@code resourceName} is a {@link FileName} wrapping the image filename as stored under
 * {@code plants/images/} in the classpath resources (e.g. {@code "IMG_9313.HEIC"}). The
 * directory path is not stored — it is a stable convention of the plants bounded context.
 * Use {@code resourceName.path("plants/images/")} to compose the full classpath resource
 * path at the point of use, and {@code resourceName.nameType()} to branch on format (e.g.
 * {@code "HEIC"} vs {@code "JPG"}) when conversion is required.
 */
public record PlantImage(
        PlantImageId id,
        @JsonTypeInfo(use = Id.NAME, property = "parentRank", include = As.EXTERNAL_PROPERTY)
        @JsonSubTypes({
                @Type(value = PlantOrderName.class, name = "ORDER"),
                @Type(value = PlantFamilyName.class, name = "FAMILY"),
                @Type(value = PlantGenusName.class, name = "GENUS"),
                @Type(value = PlantSpeciesName.class, name = "SPECIES")
        })
        PlantRankName parentName,
        Instant dateAdded,
        FileName resourceName,
        @Nullable PlantObservationId observationId
) implements Entity<PlantImageId> {

    @Override
    public Consumer<? extends Constraints> invariants() {
        return i -> i
                .entityId(id, "id")
                .identifier(parentName, "parentName")
                .notNull(dateAdded, "dateAdded")
                .namedValue(resourceName, "resourceName")
                .whenNotNull(observationId, c -> c.entityId(observationId, "observationId"));
    }
}
