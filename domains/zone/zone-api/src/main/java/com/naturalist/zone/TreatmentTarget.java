package com.naturalist.zone;

import com.naturalist.zone.subzone.SubZoneName;

import java.util.Objects;

/**
 * The spatial entity that received a treatment event — either a whole Zone or a specific SubZone.
 * <p>
 * {@code TreatmentTarget} is a sealed interface enabling exhaustive pattern matching at the
 * application layer. It lives in the Zones module because it is defined purely in terms of
 * zone identity — it carries no knowledge of treatments themselves.
 * <p>
 * This design emerged from the April 7, 2026 thrips/TSWV outbreak at Oak Vista, where neem oil
 * and dish soap treatment was applied to specific sections of the backyard garden row rather than
 * the whole bed. The system needs to record <em>which section</em> was treated in order to
 * build an accurate pest pressure and treatment history at sub-zone granularity.
 * <p>
 * Usage at the application layer with exhaustive pattern matching:
 * <pre>{@code
 * switch (event.target()) {
 *     case TreatmentTarget.ZoneTreatmentTarget z ->
 *         applyToWholeZone(z.zoneName());
 *     case TreatmentTarget.SubZoneTreatmentTarget s ->
 *         applyToSubZone(s.zoneName(), s.subZoneName());
 * }
 * }</pre>
 * <p>
 * Factory methods are provided for convenience:
 * <pre>{@code
 * TreatmentTarget.zone(zoneName)
 * TreatmentTarget.subZone(zoneName, subZoneName)
 * }</pre>
 */
public sealed interface TreatmentTarget
        permits TreatmentTarget.ZoneTreatmentTarget,
                TreatmentTarget.SubZoneTreatmentTarget {

    /**
     * The Zone that this target belongs to.
     * <p>
     * Always present regardless of whether the target is a whole Zone or a SubZone.
     * SubZone targets carry the parent ZoneName so that callers can resolve zone context
     * without a separate lookup.
     *
     * @return the ZoneName of the targeted Zone or the SubZone's parent Zone
     */
    ZoneName zoneName();

    /**
     * A treatment that targets an entire Zone — all sub-zones receive the treatment uniformly.
     * <p>
     * Used for whole-bed applications: blanket irrigation, broadcast fertiliser, full-bed
     * neem oil when no sub-zone distinction is required. Less granular than a
     * {@link SubZoneTreatmentTarget} and results in a zone-level (not sub-zone-level)
     * entry in the treatment history.
     */
    record ZoneTreatmentTarget(ZoneName zoneName) implements TreatmentTarget {
        public ZoneTreatmentTarget {
            Objects.requireNonNull(zoneName, "zoneName must not be null");
        }
    }

    /**
     * A treatment that targets a specific SubZone within a parent Zone.
     * <p>
     * Enables the system to record treatment history at sub-zone granularity, which
     * is required for accurate crop rotation planning. A sub-zone with documented
     * TSWV history should not receive Solanaceae plantings in the following season —
     * this decision is only possible if treatment and pest pressure records are
     * scoped to the sub-zone, not the whole bed.
     * <p>
     * Both {@code zoneName} and {@code subZoneName} are required because a SubZoneName is
     * always interpreted relative to its parent Zone.
     */
    record SubZoneTreatmentTarget(ZoneName zoneName, SubZoneName subZoneName) implements TreatmentTarget {
        public SubZoneTreatmentTarget {
            Objects.requireNonNull(zoneName, "zoneName must not be null");
            Objects.requireNonNull(subZoneName, "subZoneName must not be null");
        }
    }

    /**
     * Creates a target scoped to an entire Zone.
     *
     * @param zoneName the Zone receiving the treatment; must not be null
     * @return a {@link ZoneTreatmentTarget}
     */
    static TreatmentTarget zone(ZoneName zoneName) {
        return new ZoneTreatmentTarget(zoneName);
    }

    /**
     * Creates a target scoped to a specific SubZone within a Zone.
     *
     * @param zoneName    the parent Zone; must not be null
     * @param subZoneName the specific SubZone receiving the treatment; must not be null
     * @return a {@link SubZoneTreatmentTarget}
     */
    static TreatmentTarget subZone(ZoneName zoneName, SubZoneName subZoneName) {
        return new SubZoneTreatmentTarget(zoneName, subZoneName);
    }
}
