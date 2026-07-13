package com.naturalist.insects;

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
 * A photographic record of an insect observation at Oak Vista.
 * <p>
 * {@code InsectImage} is an immutable observation tied to a specific point in time. It
 * does not describe a taxonomic record; it records that an organism was seen and
 * photographed. Its identity is its UUID-based {@link InsectImageId}.
 * <p>
 * {@code parentName} is the typed slug of the parent taxonomic record — whichever
 * rank ({@link InsectFamily}, {@link InsectGenus}, {@link InsectSpecies}, or a future
 * subspecies record) the naturalist's confidence allows. The sealed
 * {@link InsectRankName} marker statically constrains the slot to the four insect-side
 * rank names; cross-domain names cannot compile in. Rank transitions ("we now know this
 * is <i>Empoasca fabae</i>, not just an Empoasca") become a single-field update.
 * <p>
 * Jackson dispatch lives on the {@code parentName} component, not on the
 * {@link InsectRankName} interface — placing the polymorphic envelope on the interface
 * would leak it into every direct leaf-class serialization site (e.g.
 * {@code InsectSpecies.name}). The {@link As#EXTERNAL_PROPERTY} form flattens the
 * discriminator into a sibling {@code "parentRank"} field, keeping {@code parentName}
 * itself a plain slug string via {@code EntityName}'s {@code @JsonValue}.
 * <p>
 * {@code resourceName} is a {@link FileName} wrapping the image filename as stored under
 * {@code insects/images/} in the classpath resources (e.g. {@code "IMG_9047.HEIC"}).
 * The directory path is not stored — it is a stable convention of the insects bounded
 * context. Use {@code resourceName.path("insects/images/")} to compose the full
 * classpath resource path at the point of use, and {@code resourceName.nameType()} to
 * branch on format (e.g. {@code "HEIC"} vs {@code "JPG"}) when conversion is required.
 */
public record InsectImage(
        InsectImageId id,
        @JsonTypeInfo(use = Id.NAME, property = "parentRank", include = As.EXTERNAL_PROPERTY)
        @JsonSubTypes({
                @Type(value = InsectOrderName.class, name = "ORDER"),
                @Type(value = InsectFamilyName.class, name = "FAMILY"),
                @Type(value = InsectGenusName.class, name = "GENUS"),
                @Type(value = InsectSpeciesName.class, name = "SPECIES"),
                @Type(value = InsectSubspeciesName.class, name = "SUBSPECIES")
        })
        InsectRankName parentName,
        Instant dateAdded,
        FileName resourceName,
        @Nullable FieldObservationId observationId
) implements Entity<InsectImageId> {

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
