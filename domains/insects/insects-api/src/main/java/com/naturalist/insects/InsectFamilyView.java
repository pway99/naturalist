package com.naturalist.insects;

import com.naturalist.insects.InsectEntityCollections.FeatureCollection;
import com.naturalist.insects.InsectEntityCollections.ImageCollection;
import com.naturalist.observability.Constraints;
import org.jspecify.annotations.Nullable;

import java.util.function.Consumer;

/**
 * Family-rank {@link InsectTaxonView} — used when only family is resolved
 * (e.g. <i>Tachinidae</i> sp., <i>Braconidae</i> sp.). Composes the family record
 * with the photographic field record. Root identity is the family's
 * {@link InsectFamilyName}.
 */
public record InsectFamilyView(
        InsectFamily family,
        ImageCollection images,
        FeatureCollection features
) implements InsectTaxonView {

    public static InsectFamilyView of(InsectFamily family, ImageCollection images,
                                      FeatureCollection features) {
        return new InsectFamilyView(family, images, features);
    }

    public static InsectFamilyView of(InsectFamily family) {
        return new InsectFamilyView(family, ImageCollection.empty(), FeatureCollection.empty());
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
    public boolean belongsToOrder(@Nullable InsectOrderView order) {
        return order == null || family.belongsToOrder(order.name());
    }

    @Override
    public Consumer<? extends Constraints> invariants() {
        return i -> i
                .namedEntity(family, "family")
                .behavioralCollection(images, "images")
                .behavioralCollection(features, "features");
    }
}
