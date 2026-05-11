package com.naturalist.insects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.naturalist.ddd.EntityName;

import java.util.Objects;

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
        Objects.requireNonNull(species, "species");
        Objects.requireNonNull(kind, "kind");
        return new LifeStageName(species.value() + "-" + kind.slug());
    }

    public static LifeStageName of(InsectGenusName genus, LifeStageKind kind) {
        Objects.requireNonNull(genus, "genus");
        Objects.requireNonNull(kind, "kind");
        return new LifeStageName(genus.value() + "-" + kind.slug());
    }

    public static LifeStageName of(InsectFamilyName family, LifeStageKind kind) {
        Objects.requireNonNull(family, "family");
        Objects.requireNonNull(kind, "kind");
        return new LifeStageName(family.value() + "-" + kind.slug());
    }

    @JsonCreator
    public static LifeStageName of(String value) {
        return new LifeStageName(value);
    }

    public InsectSpeciesName speciesName() {
        return InsectSpeciesName.of(value().substring(0, lastHyphenIndex()));
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
        if (idx < 0) {
            throw new IllegalStateException(
                    "LifeStageName has no stage-kind component: " + value());
        }
        return idx;
    }
}
