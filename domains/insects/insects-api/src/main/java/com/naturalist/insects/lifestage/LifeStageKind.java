package com.naturalist.insects.lifestage;

/**
 * The stages of an insect's life cycle. Component of {@link LifeStageName}.
 */
public enum LifeStageKind {
    EGG("egg"),
    LARVA("larva"),
    PUPA("pupa"),
    ADULT("adult");

    private final String slug;

    LifeStageKind(String slug) {
        this.slug = slug;
    }

    public String slug() {
        return slug;
    }

    public static LifeStageKind fromSlug(String slug) {
        for (LifeStageKind k : values()) {
            if (k.slug.equals(slug)) return k;
        }
        throw new IllegalArgumentException("Unknown life stage kind slug: " + slug);
    }
}
