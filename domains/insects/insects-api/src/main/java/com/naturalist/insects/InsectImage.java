package com.naturalist.insects;

import com.naturalist.data.FileName;
import com.naturalist.ddd.FactEntity;
import com.naturalist.observability.Constraints;
import org.jspecify.annotations.Nullable;

import java.time.Instant;
import java.util.function.Consumer;

/**
 * A photographic record of an insect species observation at Oak Vista.
 * <p>
 * {@code InsectImage} is a {@link FactEntity} — an immutable observation tied to a
 * specific point in time. It does not describe a species; it records that a species was
 * seen and photographed. Its identity is a UUID-based {@link InsectImageName}, never
 * referenced cross-domain by slug.
 * <p>
 * {@code insectSpeciesName} is the stable slug of the parent {@link InsectSpecies}.
 * {@code insectSpeciesId} is the resolved persistence key — null in JSON catalog fixtures
 * (same pattern as {@link InsectImage#id}) and populated by the repository once the
 * parent species is inserted and assigned an ID. The slug is the authoritative reference;
 * the ID is a denormalized convenience for the RDBMS join column.
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
        InsectImageName name,
        @Nullable InsectSpeciesId insectSpeciesId,
        InsectSpeciesName insectSpeciesName,
        Instant dateAdded,
        FileName resourceName
) implements FactEntity<InsectImageId, InsectImageName> {

    @Override
    public InsectImage withId(InsectImageId id) {
        return new InsectImage(id, name, insectSpeciesId, insectSpeciesName, dateAdded, resourceName);
    }

    public InsectImage withInsectSpeciesId(InsectSpeciesId insectSpeciesId) {
        return new InsectImage(id, name, insectSpeciesId, insectSpeciesName, dateAdded, resourceName);
    }

    @Override
    public Consumer<? extends Constraints> invariants() {
        return i -> i
                .entityId(this, InsectImage::id, "id")
                .entityName(name(), "name")
                .entityName(insectSpeciesName(), "insectSpeciesName")
                .notNull(this, InsectImage::dateAdded, "dateAdded")
                .namedValue(this, InsectImage::resourceName, "resourceName");
    }
}
