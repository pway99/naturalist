package com.naturalist.insects.lifestages;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.naturalist.ddd.EntityName;
import com.naturalist.insects.InsectSpeciesName;

import java.util.Objects;

/**
 * The natural-key identity of a {@link LifeStage}, composing an
 * {@link InsectSpeciesName} with a {@link LifeStageKind}. The wire form is a single
 * kebab-case slug: {@code {species-slug}-{stage-kind-slug}} — for example
 * {@code battus-philenor-larva}, {@code chrysoperla-rufilabris-egg}.
 * <p>
 * The composite structure is recoverable via {@link #speciesName()} and
 * {@link #stageKind()}, both of which parse the wrapped slug on demand. The slug
 * is the canonical identity form — parsed accessors are a domain convenience, not
 * a separate source of truth.
 * <p>
 * Composition is asymmetric: the species slug may itself contain hyphens (genus-
 * species form), and the stage kind slug is a fixed single token. Parsing therefore
 * splits on the <i>last</i> hyphen, treating everything before as the species slug
 * and the final token as the stage kind. This works because {@link LifeStageKind}
 * values are single-token slugs by construction — {@code egg}, {@code larva},
 * {@code pupa}, {@code adult}, none of which contain internal hyphens.
 * <p>
 * Per ADR-005 Amendment 3 (carried forward through ADR-022), the wrapped slug must
 * satisfy {@code ^[a-z0-9]+(-[a-z0-9]+)*$}. Because both component slugs satisfy
 * this and the joining hyphen sits between them, the composite satisfies it too.
 */
public final class LifeStageName extends EntityName {

    private static final int MAX_LENGTH = 80;

    private LifeStageName(String value) {
        super(value);
    }

    /**
     * Compose a name from its structural components. This is the primary factory —
     * callers that already hold typed components should never assemble the slug
     * string themselves.
     */
    public static LifeStageName of(InsectSpeciesName species, LifeStageKind kind) {
        Objects.requireNonNull(species, "species");
        Objects.requireNonNull(kind, "kind");
        return new LifeStageName(species.value() + "-" + kind.slug());
    }

    /**
     * Parse a name from its wire form. Used by Jackson and by any persistence
     * adapter reading stage identity from a slug column. The resulting instance
     * may be invalid (parent {@code EntityName} validation surfaces this via
     * {@code isValid()}) — this factory does not throw on malformed input.
     */
    @JsonCreator
    public static LifeStageName of(String value) {
        return new LifeStageName(value);
    }

    /**
     * The species component of this stage's identity.
     */
    public InsectSpeciesName speciesName() {
        return InsectSpeciesName.of(value().substring(0, lastHyphenIndex()));
    }

    /**
     * The stage kind component of this stage's identity.
     */
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
