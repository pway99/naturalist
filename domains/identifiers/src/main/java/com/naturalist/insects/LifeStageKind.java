package com.naturalist.insects;

import com.naturalist.observability.Observer;
import com.naturalist.observability.MethodObserver;

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
        MethodObserver m = OBSERVER.forMethod("fromSlug");
        m.arguments(i -> i
                .notBlank(slug, "slug"))
                .throwWhenInvalid();
        LifeStageKind kind = bySlug(slug);
        m.state(s -> s
                .notNull(kind, "kind"))
                .throwWhenInvalid();
        return kind;
    }

    private static final Observer OBSERVER = Observer.forClass(LifeStageKind.class);

    /** The kind whose slug is {@code slug}, or null when no kind has it. */
    private static LifeStageKind bySlug(String slug) {
        for (LifeStageKind k : values()) {
            if (k.slug.equals(slug)) {
                return k;
            }
        }
        return null;
    }
}
