package com.naturalist.plants;

import com.naturalist.ddd.Entity;
import com.naturalist.ddd.UniqueValue;
import com.naturalist.observability.Constraints;
import org.apache.commons.lang3.StringUtils;

import java.util.function.Consumer;

/**
 * A typed, normalised field mark — what a plant <em>looks like</em> at a given rank.
 * <p>
 * Identity is surrogate ({@link PlantFeatureId}, UUIDv7) because there is no external
 * authority guaranteeing feature-name uniqueness the way the ICN guarantees botanical
 * rank names. {@code @UniqueValue} on {@link #value} enforces that each normalised text
 * appears at most once within the data source, but the identity boundary is the
 * surrogate id, not the text.
 * <p>
 * The {@linkplain #PlantFeature(PlantFeatureId, String) compact constructor} trims
 * whitespace and case-folds to lowercase so that {@code "Ray Florets"} and
 * {@code "ray florets"} normalise to the same value and correctly collide on
 * the uniqueness constraint.
 * <p>
 * Mirrors {@code insects.InsectFeature} — see that type's javadoc for the fuller
 * rationale on why the value space is deliberately open (no enum, sealed set, or
 * controlled registry).
 */
public record PlantFeature(
        PlantFeatureId id,
        @UniqueValue String value
) implements Entity<PlantFeatureId> {

    public PlantFeature {
        if (StringUtils.isNotBlank(value)) {
            value = value.trim().toLowerCase();
        }
    }

    public static PlantFeature of(PlantFeatureId id, String value) {
        return new PlantFeature(id, value);
    }

    @Override
    public Consumer<? extends Constraints> invariants() {
        return i -> i
                .entityId(id, "id")
                .notBlank(value, "value");
    }
}
