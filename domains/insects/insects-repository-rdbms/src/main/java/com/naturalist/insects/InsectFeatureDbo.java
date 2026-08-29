package com.naturalist.insects;

import com.naturalist.observability.Constraints;
import com.naturalist.observability.Observer;
import com.naturalist.persistence.Dbo;
import com.naturalist.persistence.DboSchema;

import java.util.UUID;
import java.util.function.Consumer;

/**
 * Persistence view of {@link InsectFeature} — a surrogate-UUID {@code Entity} keyed by
 * {@link InsectFeatureId}, whose UUIDv7 identity stores as the native {@code uuid} PK. MyBatis has
 * no UUID type handler and the ACL forbids adding one, so the id travels as text and the mapper
 * casts it in SQL ({@code ::uuid}).
 *
 * <p>{@code value} is the normalised field mark. {@link InsectFeature}'s compact constructor has
 * already trimmed and case-folded it, so it is stored verbatim — the DBO must not re-normalise or
 * a round-trip could diverge from what the uniqueness constraint indexed.
 */
@DboSchema(table = "insect_feature", primaryKey = "id", unique = {"value"}, entity = InsectFeature.class)
final class InsectFeatureDbo implements Dbo {
    String id;     // the InsectFeatureId's UUID as text; the mapper casts it to uuid (::uuid)
    String value;  // normalised diagnostic text — already trimmed+lowercased by InsectFeature's ctor

    static InsectFeatureDbo from(InsectFeature feature) {
        InsectFeatureDbo d = new InsectFeatureDbo();
        d.id = feature.id().value().toString();
        d.value = feature.value();   // store as-is; the record already normalised it
        Observer.forClass(InsectFeatureDbo.class)
                .arguments("from", i -> i.observable(d, "dbo"))
                .throwWhenInvalid();
        return d;
    }

    InsectFeature toEntity() {
        return new InsectFeature(InsectFeatureId.of(UUID.fromString(id)), value);
    }

    @Override
    public Consumer<? extends Constraints> invariants() {
        return c -> c
                .notBlank(id, "id")
                .notBlank(value, "value").maxLength(value, 255, "value");
    }
}
