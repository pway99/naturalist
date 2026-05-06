package com.naturalist.soil.event;

import com.naturalist.chemistry.compound.CompoundName;
import com.naturalist.ddd.Entity;
import com.naturalist.observability.Constraints;
import com.naturalist.zone.ZoneName;
import com.naturalist.zone.subzone.SubZoneName;
import org.jspecify.annotations.Nullable;

import java.time.LocalDate;
import java.util.function.Consumer;

/**
 * An immutable record of a soil amendment application event at a specific spatial target.
 * <p>
 * {@code AmendmentEvent} is a child entity within the {@link com.naturalist.soil.SoilProfile}
 * aggregate. The event records what was applied, in what quantity, to which spatial unit,
 * and when — providing the full input history required to track nutrient loading and
 * interpret soil chemistry changes over time.
 * <p>
 * <b>Chemical cross-reference:</b> The amendment compound is referenced by
 * {@link CompoundName} (soft reference into the chemistry domain), enabling the
 * application layer to resolve the compound's full chemical profile — solubility,
 * nutrient content, volatilisation risk — without coupling soil-api to chemistry-api
 * at compile time.
 * <p>
 * <b>Spatial targeting:</b> The target is expressed as a {@link ZoneName} (always present)
 * and an optional {@link SubZoneName} (present when the amendment was applied to a specific
 * sub-zone rather than the whole zone). Both types are from the {@code identifiers} module —
 * soil-api carries no compile-time dependency on zone-api. Sub-zone granularity is required
 * for accurate per-area nutrient loading calculations.
 * <p>
 * <b>Oak Vista amendment events of note:</b>
 * <ul>
 *   <li><b>Box 1 blood meal, April 2026</b> — Blood meal applied to Box 1 following the
 *       March 2026 FGL N-Nitrate VERY_LOW result. The application quantity exceeded
 *       the calculated requirement, contributing to an active nitrogen over-application
 *       concern as of April 2026. This event is the primary input driving the blood meal
 *       nitrogen toxicity monitoring.</li>
 *   <li><b>Gypsum (planned)</b> — CaSO₄·2H₂O to address Ca-Sol and Sulfate VERY_LOW in
 *       both beds. Compound: {@code "calcium-sulfate-dihydrate"} in the chemistry domain.</li>
 *   <li><b>Boron (planned)</b> — Borax or solubor to address Boron VERY_LOW. Required
 *       alongside gypsum since Ca transport to fruit requires boron as a co-factor.</li>
 * </ul>
 */
public record AmendmentEvent(
        AmendmentEventId name,
        ZoneName zoneName,
        @Nullable SubZoneName subZoneName,
        CompoundName compoundName,
        AmendmentRate amount,
        AmendmentUnit unit,
        LocalDate appliedDate,
        @Nullable String notes
) implements Entity<AmendmentEventId> {

    @Override
    public Consumer<? extends Constraints> invariants() {
        return i -> i
                .entityId(name, "name")
                .entityName(zoneName, "zoneName")
                .entityNameOrNull(subZoneName, "subZoneName")
                .entityName(compoundName, "compoundName")
                .namedValue(amount, "amount")
                .notNull(this, AmendmentEvent::unit, "unit")
                .notNull(this, AmendmentEvent::appliedDate, "appliedDate");
    }
}
