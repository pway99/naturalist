package com.naturalist.insects;

import com.naturalist.insects.InsectEntityCollections.ImageCollection;
import com.naturalist.observability.Constraints;

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
    public InsectRankName name() {
        return genus.name();
    }

    @Override
    public Consumer<? extends Constraints> invariants() {
        return i -> i
                .namedEntity(genus, "genus")
                .behavioralCollection(images, "images");
    }
}
