package com.naturalist.insects;

import com.naturalist.ddd.Entity;
import com.naturalist.ddd.UniqueValue;
import com.naturalist.observability.Constraints;

import java.util.function.Consumer;

/**
 * A typed, normalised field mark — what an insect <em>looks like</em> at a given rank.
 * <p>
 * Identity is surrogate ({@link InsectFeatureId}, UUIDv7) because there is no external
 * authority guaranteeing feature-name uniqueness the way the ICZN guarantees Linnaean
 * rank names. {@code @UniqueValue} on {@link #value} enforces that each normalised text
 * appears at most once within the data source, but the identity boundary is the
 * surrogate id, not the text.
 * <p>
 * The {@linkplain #InsectFeature(InsectFeatureId, String) compact constructor} trims
 * whitespace and case-folds to lowercase so that {@code "Sucking mouthparts"} and
 * {@code "sucking mouthparts"} normalise to the same value and correctly collide on
 * the uniqueness constraint.
 * <p>
 * The value space is deliberately <b>open</b> — there is no enum, sealed set, or
 * controlled registry. The same feature string may denote non-homologous characters in
 * different lineages (e.g. "reduced wings" is brachyptery in a beetle and aptery in an
 * ant). Ownership-by-rank via {@link InsectFeatureAssignment} carries the disambiguating
 * context; the typed value gives reliable <em>form</em> without asserting universal
 * <em>meaning</em>.
 */
public record InsectFeature(
        InsectFeatureId id,
        @UniqueValue String value
) implements Entity<InsectFeatureId> {

    public InsectFeature {
        if (value != null) {
            value = value.trim().toLowerCase();
        }
    }

    public static InsectFeature of(InsectFeatureId id, String value) {
        return new InsectFeature(id, value);
    }

    @Override
    public Consumer<? extends Constraints> invariants() {
        return i -> i
                .entityId(id, "id")
                .notBlank(value, "value");
    }
}
