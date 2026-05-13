package com.naturalist.soil;

import com.naturalist.ddd.ValueObject;
import com.naturalist.measurements.DepthInches;
import com.naturalist.observability.Constraints;
import org.jspecify.annotations.Nullable;

import java.time.LocalDate;
import java.util.function.Consumer;

/**
 * An immutable snapshot of the surface mulch layer currently applied to a soil profile's
 * spatial unit.
 * <p>
 * {@code MulchLayer} is a value object within the {@link com.naturalist.soil.SoilProfile}
 * aggregate — it does not have independent identity. The SoilProfile carries the current
 * layer as a nullable field; a {@code null} current layer indicates bare or unmulched soil.
 * <p>
 * When a mulch change is made (new material applied, depth adjusted, or mulch removed),
 * the SoilProfile produces a new instance with an updated {@code currentMulchLayer}.
 * Historical mulch records, if needed for audit, would be maintained as a list of past
 * layers; the current design captures only the present state.
 * <p>
 * <b>Oak Vista mulch state (April 7, 2026 post-outbreak):</b>
 * <ul>
 *   <li>Backyard north: STRAW, thinned to ~1 inch (from ~2 inches) as thrips habitat
 *       reduction measure.</li>
 *   <li>Backyard center/south: STRAW, ~2 inches (retained for moisture management).</li>
 *   <li>Box 1: STRAW (state not specifically documented as of this writing).</li>
 * </ul>
 * <p>
 * <b>Application layer responsibility:</b> When {@code MulchLayer} changes on a
 * {@code SoilProfile}, the application layer maps the new {@link MulchType} to the
 * corresponding {@code ThripsHabitatRisk} (a zone-api type) and updates the zone domain's
 * {@code SubZone.surfaceHabitatRisk}. The mapping lives in the application layer — not
 * in soil-api — because soil-api and zone-api are peer modules and neither may depend
 * on the other.
 */
public record MulchLayer(
        MulchType mulchType,
        DepthInches depthInches,
        LocalDate appliedDate,
        @Nullable LocalDate replacedDate,
        @Nullable String notes
) implements ValueObject {

    /**
     * Whether this mulch layer is still in place (has not been removed or replaced).
     *
     * @return {@code true} if {@code replacedDate} is null
     */
    public boolean isActive() {
        return replacedDate == null;
    }

    @Override
    public Consumer<? extends Constraints> invariants() {
        return i -> i
                .notNull(mulchType, "mulchType")
                .namedValue(depthInches, "depthInches")
                .notNull(appliedDate, "appliedDate");
    }
}
