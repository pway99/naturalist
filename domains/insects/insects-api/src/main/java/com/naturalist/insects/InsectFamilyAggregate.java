package com.naturalist.insects;

import com.naturalist.insects.InsectEntityCollections.ImageCollection;
import com.naturalist.observability.Constraints;
import org.jspecify.annotations.Nullable;

import java.util.function.Consumer;

/**
 * Family-rank {@link InsectAggregate} — used when only family is resolved
 * (e.g. <i>Tachinidae</i> sp., <i>Braconidae</i> sp.). Composes the family record
 * with the photographic field record. Root identity is the family's
 * {@link InsectFamilyName}.
 */
public record InsectFamilyAggregate(
        InsectFamily family,
        ImageCollection images
) implements InsectAggregate {

    public static InsectFamilyAggregate of(InsectFamily family, ImageCollection images) {
        return new InsectFamilyAggregate(family, images);
    }

    public static InsectFamilyAggregate of(InsectFamily family) {
        return new InsectFamilyAggregate(family, ImageCollection.empty());
    }

    @Override
    public InsectFamilyName name() {
        return family.name();
    }

    /** The order this family belongs to, exposed as a typed FK delegate. */
    public InsectOrderName orderName() {
        return family.orderName();
    }

    /**
     * True iff this family's order FK equals the given order's name, OR the
     * given order is null. Null tolerance lets a caller compose this check
     * inside a {@code whenNotNull(family, ...)} block without firing a
     * redundant {@code isTrue} violation when the order is missing —
     * monotonic-fill already reports the missing-order case via its own
     * violation.
     */
    public boolean belongsToOrder(@Nullable InsectOrderAggregate order) {
        return order == null || family.belongsToOrder(order.name());
    }

    @Override
    public Consumer<? extends Constraints> invariants() {
        return i -> i
                .namedEntity(family, "family")
                .behavioralCollection(images, "images");
    }
}
