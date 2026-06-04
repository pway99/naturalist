package com.naturalist.insects;

import com.naturalist.insects.InsectEntityCollections.ImageCollection;
import com.naturalist.observability.Constraints;
import org.jspecify.annotations.Nullable;

import java.util.function.Consumer;

/**
 * Genus-rank {@link InsectAggregate} — used when identification firmed up to genus
 * but not to species (e.g. <i>Empoasca</i> sp., <i>Halictus</i> sp.). Composes the
 * genus record with the photographic field record. Root identity is the genus's
 * {@link InsectGenusName}.
 */
public record InsectGenusAggregate(
        InsectGenus genus,
        ImageCollection images
) implements InsectAggregate {

    public static InsectGenusAggregate of(InsectGenus genus, ImageCollection images) {
        return new InsectGenusAggregate(genus, images);
    }

    public static InsectGenusAggregate of(InsectGenus genus) {
        return new InsectGenusAggregate(genus, ImageCollection.empty());
    }

    @Override
    public InsectGenusName name() {
        return genus.name();
    }

    /** The family this genus belongs to, exposed as a typed FK delegate. */
    public InsectFamilyName familyName() {
        return genus.familyName();
    }

    /** The order this genus belongs to, exposed as a typed FK delegate. */
    public InsectOrderName orderName() {
        return genus.orderName();
    }

    /**
     * True iff this genus's family FK equals the given family's name, OR the
     * given family is null. Null tolerance lets a caller compose this check
     * inside a {@code whenNotNull(genus, ...)} block without firing a
     * redundant {@code isTrue} violation when the family is missing
     * (monotonic-fill catches missing-ancestor via its own
     * {@code .genus:family} violation).
     */
    public boolean belongsToFamily(@Nullable InsectFamilyAggregate family) {
        return family == null || genus.belongsToFamily(family.name());
    }

    /**
     * True iff this genus's order FK equals the given order's name, OR the
     * given order is null. {@code InsectGenus} carries {@code orderName} as a
     * denormalized direct FK (alongside {@code familyName}); this check
     * catches drift between {@code genus.orderName} and the actual order
     * the aggregate composes — a gap left if only the immediate-parent
     * (family) FK were validated. Same null-tolerance rationale as
     * {@link #belongsToFamily}.
     */
    public boolean belongsToOrder(@Nullable InsectOrderAggregate order) {
        return order == null || genus.belongsToOrder(order.name());
    }

    @Override
    public Consumer<? extends Constraints> invariants() {
        return i -> i
                .namedEntity(genus, "genus")
                .behavioralCollection(images, "images");
    }
}
