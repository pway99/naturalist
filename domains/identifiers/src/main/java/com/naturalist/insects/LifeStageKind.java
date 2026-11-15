package com.naturalist.insects;

import java.util.Optional;
import java.util.Arrays;
import com.naturalist.observability.Observer;

/**
 * The stages of an insect's life cycle. Component of {@link LifeStageName}.
 */
public enum LifeStageKind {
    EGG("egg"),
    LARVA("larva"),
    NYMPH("nymph"),
    PUPA("pupa"),
    JUVENILE("juvenile"),
    ADULT("adult");

    private final String slug;

    LifeStageKind(String slug) {
        this.slug = slug;
    }

    public String slug() {
        return slug;
    }

    public static LifeStageKind fromSlug(String slug) {
        Optional<LifeStageKind> found = Arrays.stream(values())
                .filter(k -> k.slug.equals(slug))
                .findFirst();
        OBSERVER.arguments("fromSlug", i -> i
                .notNull(slug, "slug")
                .isTrue(found.isPresent(), "knownSlug"))
                .throwWhenInvalid();
        return found.orElseThrow();
    }

    private static final Observer OBSERVER = Observer.forClass(LifeStageKind.class);
}
