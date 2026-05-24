package com.naturalist.insects;

import com.naturalist.insects.InsectEntityCollections.ImageCollection;
import com.naturalist.observability.Constraints;

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
    public InsectRankName name() {
        return family.name();
    }

    @Override
    public Consumer<? extends Constraints> invariants() {
        return i -> i
                .namedEntity(family, "family")
                .observable(images, "images");
    }
}
