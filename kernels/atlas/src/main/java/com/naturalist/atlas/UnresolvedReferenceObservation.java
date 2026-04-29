package com.naturalist.atlas;

import com.naturalist.ddd.EntityName;
import com.naturalist.observability.Constraints;
import com.naturalist.observability.Observable;

import java.util.function.Consumer;

/**
 * An observation emitted when the atlas detects a cross-aggregate reference
 * to an entity that does not exist in any contributed catalogue. Soft
 * validation — never thrown, always observed.
 * <p>
 * The producer is whichever atlas call site detects the miss: a startup
 * walker (M10), a runtime forward-resolution attempt against
 * {@code Atlas.resolveAlias}, or a fan-out provider that surfaces a
 * dangling target. The consumer is configured per app — typically a
 * Micrometer counter and a structured log line wired in the console's
 * composition root (M9).
 * <p>
 * The {@code targetType} carries the {@link EntityName} subclass the
 * source aggregate was pointing at; {@code targetSlug} carries the literal
 * slug it was pointing at. Together they identify the missing entity well
 * enough for an operator to decide whether the data file has a typo or the
 * referenced domain has not yet shipped a contribution.
 *
 * @param source     the entity that holds the dangling reference
 * @param targetType the {@link EntityName} subclass of the missing target
 * @param targetSlug the literal slug the source was attempting to resolve
 * @param reason     a short human-readable explanation suitable for log output
 *                   (e.g. {@code "no contribution registered for type"} or
 *                   {@code "slug not present in domain catalogue"})
 */
public record UnresolvedReferenceObservation(
        EntityRef source,
        Class<? extends EntityName> targetType,
        String targetSlug,
        String reason
) implements Observable {

    @Override
    public Consumer<? extends Constraints> invariants() {
        return i -> i
                .valueObject(this, UnresolvedReferenceObservation::source, "source")
                .notNull(this, UnresolvedReferenceObservation::targetType, "targetType")
                .notBlank(this, UnresolvedReferenceObservation::targetSlug, "targetSlug")
                .notBlank(this, UnresolvedReferenceObservation::reason, "reason");
    }
}
