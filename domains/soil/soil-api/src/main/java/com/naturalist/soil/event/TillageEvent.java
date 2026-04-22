package com.naturalist.soil.event;

import com.naturalist.ddd.NamedEntity;
import com.naturalist.measurements.DepthInches;
import com.naturalist.observability.Constraints;
import com.naturalist.zone.ZoneName;
import com.naturalist.zone.subzone.SubZoneName;
import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.function.Consumer;

/**
 * An immutable record of a mechanical soil disturbance event (tillage, rototilling,
 * or deep cultivation) at a specific spatial target.
 * <p>
 * {@code TillageEvent} is a child entity within the {@link com.naturalist.soil.SoilProfile}
 * aggregate. It records the mechanical disruption of soil structure — the most significant
 * biological and physical intervention in the soil management system.
 * <p>
 * <b>Biological impact:</b> Tillage disrupts the fungal hyphal networks, earthworm
 * burrow systems, and macropore structure that govern water infiltration and drainage.
 * The sensor-observed drainage recovery time — the primary biological soil health proxy
 * in this system — increases dramatically after tillage and recovers over days to weeks
 * as biological activity re-establishes soil structure. A {@code TillageEvent} provides
 * the context required to correctly interpret drainage recovery trajectories in sensor data.
 * <p>
 * <b>Oak Vista backyard rototill, April 2, 2026:</b> The backyard garden was rototilled
 * to 6 inches prior to transplanting. The WH51 sensor at 5-inch depth subsequently showed
 * drainage recovery times exceeding 12 hours across the April 2–10, 2026 observation window
 * — classified as RECOVERING to DISRUPTED. Without the {@code TillageEvent} record, these
 * slow drainage readings would be misinterpreted as a chronic soil problem rather than the
 * expected short-term post-tillage recovery.
 * <p>
 * <b>Expected recovery timeline:</b> At Oak Vista backyard (native clay amended, clay
 * sublayer), biological drainage recovery after rototilling to 6 inches is estimated at
 * 2–4 weeks to return to pre-tillage drainage rates, assuming no additional large irrigation
 * events. Recovery is slower than a pure raised bed (Box 1 worm casting blend) because the
 * clay sublayer restricts downward flow independently of biological structure.
 */
public record TillageEvent(
        TillageEventName name,
        ZoneName zoneName,
        @Nullable SubZoneName subZoneName,
        LocalDate tillageDate,
        DepthInches depthInches,
        TillageType tillageType,
        @Nullable String notes
) implements NamedEntity<TillageEventName> {

    // ── Domain queries ─────────────────────────────────────────────────────────

    /**
     * The estimated number of days for biological soil drainage recovery after this event.
     * <p>
     * Estimates are based on tillage type, depth, and known characteristics of the
     * target's substrate (inferred from context rather than encoded here — the
     * application layer applies the relevant substrate modifier).
     * <p>
     * Baseline recovery estimates before substrate modification:
     * <ul>
     *   <li>BROADFORK at any depth: 3–7 days (minimal disruption)</li>
     *   <li>HAND_CULTIVATION at ≤3 inches: 5–10 days</li>
     *   <li>ROTOTILL at ≤4 inches: 10–14 days</li>
     *   <li>ROTOTILL at 6 inches: 14–21 days (Oak Vista backyard baseline)</li>
     * </ul>
     * Clay sublayer presence (backyard) extends these estimates by 30–50%.
     *
     * @return estimated days to biological drainage recovery
     */
    public int estimatedRecoveryDays() {
        return switch (tillageType) {
            case BROADFORK -> 5;
            case HAND_CULTIVATION -> depthInches.isAtMost(new BigDecimal("3")) ? 7 : 12;
            case ROTOTILL -> depthInches.isAtMost(new BigDecimal("4")) ? 12 : 21;
        };
    }

    @Override
    public Consumer<? extends Constraints> invariants() {
        return i -> i
                .entityName(name, "name")
                .notNull(this, TillageEvent::zoneName, "zoneName")
                .notNull(this, TillageEvent::tillageDate, "tillageDate")
                .namedValue(this, TillageEvent::depthInches, "depthInches")
                .notNull(this, TillageEvent::tillageType, "tillageType");
    }

    // ─────────────────────────────────────────────────────────────────────────

    /**
     * The type of mechanical tillage implement used.
     * <p>
     * Determines the character of soil disturbance — rototilling homogenises and
     * fragments the soil profile; broadforking aerates without horizontal shear;
     * hand cultivation disturbs only the surface layer.
     */
    public enum TillageType {

        /**
         * Mechanical rototiller — maximum disturbance.
         * <p>
         * Horizontally shears and homogenises the tilled depth. Destroys fungal hyphae,
         * worm burrows, and macropore networks. Longest recovery period.
         * Oak Vista backyard April 2, 2026 event used this implement.
         */
        ROTOTILL,

        /**
         * Broadfork — vertical aeration with minimal horizontal disruption.
         * <p>
         * Tines penetrate vertically to open channels without shearing horizontally.
         * Preserves most of the soil structure while improving aeration and drainage.
         * Preferred over rototilling for established beds where biological structure
         * is intact.
         */
        BROADFORK,

        /**
         * Hand cultivation — surface scratch or hoe work.
         * <p>
         * Disturbs the top 1–3 inches for weed control, surface crust breaking,
         * or incorporation of surface-applied amendments. Minimal biological impact
         * on sub-surface soil structure.
         */
        HAND_CULTIVATION
    }
}
