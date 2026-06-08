package com.naturalist.insects;

import com.naturalist.insects.InsectEntityCollections.ImageCollection;
import com.naturalist.observability.Constraints;
import org.jspecify.annotations.Nullable;

import java.util.function.Consumer;

/**
 * Genus-rank {@link InsectTaxonView} — used when identification firmed up to genus
 * but not to species (e.g. <i>Empoasca</i> sp., <i>Halictus</i> sp.). Composes the
 * genus record with the photographic field record. Root identity is the genus's
 * {@link InsectGenusName}.
 */
public record InsectGenusView(
        InsectGenus genus,
        ImageCollection images
) implements InsectTaxonView {

    public static InsectGenusView of(InsectGenus genus, ImageCollection images) {
        return new InsectGenusView(genus, images);
    }

    public static InsectGenusView of(InsectGenus genus) {
        return new InsectGenusView(genus, ImageCollection.empty());
    }

    @Override
    public InsectGenusName name() {
        return genus.name();
    }

    /** The family this genus belongs to, exposed as a typed FK delegate. */
    public InsectFamilyName familyName() {
        return genus.familyName();
    }

    /**
     * True iff this genus's family FK equals the given family's name, OR the
     * given family is null. Null tolerance lets a caller compose this check
     * inside a {@code whenNotNull(genus, ...)} block without firing a
     * redundant {@code isTrue} violation when the family is missing
     * (monotonic-fill catches missing-ancestor via its own
     * {@code .genus:family} violation).
     */
    public boolean belongsToFamily(@Nullable InsectFamilyView family) {
        return family == null || genus.belongsToFamily(family.name());
    }

    @Override
    public Consumer<? extends Constraints> invariants() {
        return i -> i
                .namedEntity(genus, "genus")
                .behavioralCollection(images, "images");
    }
}
