package com.naturalist.insects;

import com.naturalist.insects.InsectEntityCollections.ImageCollection;
import com.naturalist.observability.Constraints;
import org.jspecify.annotations.Nullable;

import java.util.function.Consumer;

/**
 * Genus-rank permit of {@link InsectTaxon} — used when identification firmed up to genus but not to species
 * (e.g. <i>Empoasca</i> sp., <i>Halictus</i> sp.). Composes the genus record with the
 * photographic field record. Root identity is the genus's {@link InsectGenusName}.
 */
public record InsectGenus(
        InsectGenusInfo info,
        ImageCollection images
) implements InsectTaxon {

    public static InsectGenus of(InsectGenusInfo genus, ImageCollection images) {
        return new InsectGenus(genus, images);
    }

    public static InsectGenus of(InsectGenusInfo genus) {
        return new InsectGenus(genus, ImageCollection.empty());
    }

    @Override
    public InsectGenusName name() {
        return info.name();
    }

    /** The family this genus belongs to, exposed as a typed FK delegate. */
    public InsectFamilyName familyName() {
        return info.familyName();
    }

    /**
     * True iff this genus's family FK equals the given family's name, OR the
     * given family is null. Null tolerance lets a caller compose this check
     * inside a {@code whenNotNull(genus, ...)} block without firing a
     * redundant {@code isTrue} violation when the family is missing
     * (monotonic-fill catches missing-ancestor via its own
     * {@code .genus:family} violation).
     */
    public boolean belongsToFamily(@Nullable InsectFamily family) {
        return family == null || info.belongsToFamily(family.name());
    }

    @Override
    public Consumer<? extends Constraints> invariants() {
        return i -> i
                .namedEntity(info, "info")
                .behavioralCollection(images, "images");
    }
}
