package com.naturalist.garden;

import com.naturalist.ddd.Entity;
import com.naturalist.observability.Constraints;
import com.naturalist.plants.cultivar.CultivarName;
import com.naturalist.zone.ZoneName;
import com.naturalist.zone.subzone.SubZoneName;
import org.jspecify.annotations.Nullable;

import java.time.LocalDate;
import java.util.function.Consumer;

/**
 * One variety, in one place, over one period — the act of cultivation, and the finest grain garden
 * records.
 * <p>
 * <b>A row is not a planting.</b> A single row holds several: three tomato varieties down one bed
 * are three plantings sharing a zone, a sub-zone and a crop type, differing by cultivar, count and
 * date. A row of mixed lettuce and kale is two plantings differing by crop <em>type</em> as well.
 * The sub-zone is where they are; it claims nothing about what is in it.
 * <p>
 * <b>Two axes meet here.</b> {@code cropTypeName} is the agronomic category — what a lab publishes
 * a panel for. {@code cultivarName} is the horticultural variety, owned by the plants domain along
 * with its breeding status and seed policy. Neither derives from the other (kale and cabbage are
 * one species and two crop types), and a planting is the place they are both true at once.
 * {@code cultivarName} is nullable because a gardener plants lettuce without always recording
 * which lettuce.
 * <p>
 * <b>No soil-profile reference.</b> The link to a crop-scoped {@code LabAnalysisInfo} is inferred
 * from {@code zoneName} plus the date window, not stored. A bed holds many plantings over years
 * against one soil profile, and a second spatial key could disagree with {@code zoneName}. Note
 * also that an analysis records the crop type it was interpreted <em>for</em>, which is a decision
 * made when submitting the sample — a bed can be soil-tested for a crop that is not planted yet.
 * <p>
 * {@code removedDate} null means still growing; see {@link #isActive(LocalDate)}. The spatial
 * nullability mirrors {@code SoilProfileInfo}: {@code zoneName} always present, {@code subZoneName}
 * null when the planting covers a whole zone.
 */
public record Planting(
        PlantingId id,
        CropTypeName cropTypeName,
        @Nullable CultivarName cultivarName,
        ZoneName zoneName,
        @Nullable SubZoneName subZoneName,
        @Nullable Integer plantCount,
        LocalDate plantedDate,
        @Nullable LocalDate removedDate,
        @Nullable String notes
) implements Entity<PlantingId> {

    /** Whether this planting was in the ground on the given date. */
    public boolean isActive(LocalDate asOf) {
        if (asOf == null || asOf.isBefore(plantedDate)) {
            return false;
        }
        return removedDate == null || !asOf.isAfter(removedDate);
    }

    /** Whether the variety was recorded, as distinct from the crop type always being known. */
    public boolean isVarietyKnown() {
        return cultivarName != null;
    }

    /** Whether this planting covers a whole zone rather than one subdivision of it. */
    public boolean isZoneScoped() {
        return subZoneName == null;
    }

    @Override
    public Consumer<? extends Constraints> invariants() {
        return i -> i
                .entityId(id, "id")
                .entityName(cropTypeName, "cropTypeName")
                .entityNameOrNull(cultivarName, "cultivarName")
                .entityName(zoneName, "zoneName")
                .entityNameOrNull(subZoneName, "subZoneName")
                .notNull(plantedDate, "plantedDate")
                .whenNotNull(plantCount, c -> c.atLeast(plantCount, 1, "plantCount"))
                .isTrue(removedDate == null || plantedDate == null
                        || !removedDate.isBefore(plantedDate), "removedNotBeforePlanted");
    }
}
