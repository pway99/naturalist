package com.naturalist.insects.lifestages;

/**
 * The stages of an insect's life cycle, as a discrete vocabulary. The slug form
 * is used as the stage-kind component of {@link LifeStageName}.
 * <p>
 * {@link #PUPA} is semantically absent on hemimetabolous species (Blattodea,
 * Orthoptera, Hemiptera), which produce nymphs rather than pupae. A
 * hemimetabolous species carries {@link EggStage} and {@link AdultStage} and nothing
 * between; the pupal slot is not "unknown," it does not exist.
 * <p>
 * Moved from {@code InsectSpecies.LifeStageKind} when {@code LifeStage} became an
 * entity with its own sub-context.
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

    /**
     * The kebab-case slug form used in {@link LifeStageName} composition.
     */
    public String slug() {
        return slug;
    }

    /**
     * Parse a slug form back into the enum. Throws {@code IllegalArgumentException}
     * for unknown slugs — callers that expect possibly-invalid input should validate
     * at the boundary rather than catching here.
     */
    public static LifeStageKind fromSlug(String slug) {
        for (LifeStageKind k : values()) {
            if (k.slug.equals(slug)) return k;
        }
        throw new IllegalArgumentException("Unknown life stage kind slug: " + slug);
    }
}
