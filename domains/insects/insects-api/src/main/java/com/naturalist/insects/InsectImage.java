package com.naturalist.insects;

import com.naturalist.data.FileName;
import com.naturalist.ddd.Entity;
import com.naturalist.observability.Constraints;

import java.time.Instant;
import java.util.function.Consumer;

/**
 * A photographic record of an insect species observation at Oak Vista.
 * <p>
 * {@code InsectImage} is an immutable observation tied to a specific point in time. It
 * does not describe a species; it records that a species was seen and photographed. Its
 * identity is its UUID-based {@link InsectImageId}.
 * <p>
 * {@code insectSpeciesName} is the stable slug of the parent {@link InsectSpecies} — the
 * authoritative cross-entity reference per ADR-022 (superseding ADR-021). The RDBMS
 * adapter is free to carry a numeric foreign-key column privately; it never surfaces on
 * the domain record.
 * <p>
 * {@code resourceName} is a {@link FileName} wrapping the image filename as stored under
 * {@code insects/images/} in the classpath resources (e.g. {@code "IMG_9047.HEIC"}).
 * The directory path is not stored — it is a stable convention of the insects bounded
 * context. Use {@code resourceName.path("insects/images/")} to compose the full
 * classpath resource path at the point of use, and {@code resourceName.nameType()} to
 * branch on format (e.g. {@code "HEIC"} vs {@code "JPG"}) when conversion is required.
 */
public record InsectImage(
        InsectImageId name,
        InsectSpeciesName insectSpeciesName,
        Instant dateAdded,
        FileName resourceName
) implements Entity<InsectImageId> {

    @Override
    public Consumer<? extends Constraints> invariants() {
        return i -> i
                .entityId(name, "name")
                .entityName(insectSpeciesName(), "insectSpeciesName")
                .notNull(this, InsectImage::dateAdded, "dateAdded")
                .namedValue(this, InsectImage::resourceName, "resourceName");
    }
}
