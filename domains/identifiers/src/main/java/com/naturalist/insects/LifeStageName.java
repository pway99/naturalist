package com.naturalist.insects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.naturalist.ddd.EntityName;
import com.naturalist.observability.Observer;

/**
 * Natural-key identity of a LifeStage. Composite slug:
 * {@code {species-slug}-{stage-kind-slug}} e.g. {@code battus-philenor-larva}.
 * Parsing splits on the last hyphen.
 */
public final class LifeStageName extends EntityName {

    private static final int MAX_LENGTH = 80;

    private LifeStageName(String value) {
        super(value);
    }

    public static LifeStageName of(InsectSpeciesName species, LifeStageKind kind) {
        OBSERVER.arguments("of", i -> i
                .notNull(species, "species")
                .notNull(kind, "kind"))
                .throwWhenInvalid();
        return new LifeStageName(species.value() + "-" + kind.slug());
    }

    public static LifeStageName of(InsectGenusName genus, LifeStageKind kind) {
        OBSERVER.arguments("of", i -> i
                .notNull(genus, "genus")
                .notNull(kind, "kind"))
                .throwWhenInvalid();
        return new LifeStageName(genus.value() + "-" + kind.slug());
    }

    public static LifeStageName of(InsectFamilyName family, LifeStageKind kind) {
        OBSERVER.arguments("of", i -> i
                .notNull(family, "family")
                .notNull(kind, "kind"))
                .throwWhenInvalid();
        return new LifeStageName(family.value() + "-" + kind.slug());
    }

    @JsonCreator
    public static LifeStageName of(String value) {
        return new LifeStageName(value);
    }

    /**
     * The parent-rank slug component of this composite name — everything
     * before the trailing {@code -{stageKind}} segment. The slug carries no
     * intrinsic rank discrimination; the rank is known by the consumer
     * that constructed this name (via the rank-typed {@code of(...)}
     * overloads) or by joining to the parent rank's catalog. For
     * {@code chrysoperla-larva} this returns {@code "chrysoperla"}; for
     * {@code battus-philenor-egg} it returns {@code "battus-philenor"}.
     */
    public String parentSlug() {
        return value().substring(0, lastHyphenIndex());
    }

    public LifeStageKind stageKind() {
        return LifeStageKind.fromSlug(value().substring(lastHyphenIndex() + 1));
    }

    @Override
    protected int maxLength() {
        return MAX_LENGTH;
    }

    private int lastHyphenIndex() {
        int idx = value().lastIndexOf('-');
        OBSERVER.state("lastHyphenIndex", s -> s
                .isTrue(idx >= 0, "stageKindComponent"))
                .throwWhenInvalid();
        return idx;
    }

    private static final Observer OBSERVER = Observer.forClass(LifeStageName.class);
}
