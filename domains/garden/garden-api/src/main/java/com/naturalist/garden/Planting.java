package com.naturalist.garden;

import com.naturalist.ddd.Entity;
import com.naturalist.observability.Constraints;
import com.naturalist.plants.PlantName;
import com.naturalist.plants.cultivar.CultivarName;
import com.naturalist.zone.ZoneName;
import com.naturalist.zone.subzone.SubZoneName;
import org.jspecify.annotations.Nullable;

import java.time.LocalDate;
import java.util.function.Consumer;

/**
 * What went into the ground, where, and for how long — the finest grain garden records.
 * <p>
 * <b>A row is not a planting.</b> A single row holds several: three tomato varieties down one bed
 * are three plantings sharing a zone and a sub-zone, differing by what was planted, how many, and
 * when. A row of mixed lettuce and kale is two plantings as well. The sub-zone is where they are;
 * it claims nothing about what is in it.
 * <p>
 * <b>Both botanical references are soft names into the plants domain, and both are optional.</b>
 * {@code plantName} reaches the species and everything the plants catalog knows about it —
 * taxonomy, roles, life form, native bioregions, description. {@code cultivarName} reaches the
 * variety and its peculiarities — breeding status, fruit type, seed-saving policy — and is null
 * whenever the variety is not known or not catalogued, which is the common case for a tray of
 * lettuce starts. Between them they carry every detail the application can resolve about what is
 * actually growing here.
 * <p>
 * At least one must be present: a planting that names neither records only that something was put
 * somewhere, which no consumer can use. Where both are given they should agree — plants' own
 * {@code Cultivar} carries a {@code plantName} — but garden cannot check that without importing a
 * peer module, so it is an application-layer concern.
 * <p>
 * <b>No crop type here.</b> A crop type is what you tell a laboratory when you submit a sample; it
 * is not what you put in the ground. The same bed of {@code brassica-oleracea} is kale or broccoli
 * depending on intent, and the plant is the same either way. {@code CropTypeName} exists as an
 * identifier for soil's {@code LabAnalysisInfo.cropType}; garden models no entity behind it until
 * {@code CropProfile} needs a catalog to key against.
 * <p>
 * <b>No soil-profile reference either.</b> The link to a crop-scoped {@code LabAnalysisInfo} is
 * inferred from {@code zoneName} plus the date window, not stored. A bed holds many plantings over
 * years against one soil profile.
 * <p>
 * {@code removedDate} null means still growing; see {@link #isActive(LocalDate)}. The spatial
 * nullability mirrors {@code SoilProfileInfo}: {@code zoneName} always present, {@code subZoneName}
 * null when the planting covers a whole zone.
 */
public record Planting(
        PlantingId id,
        @Nullable PlantName plantName,
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
        if (asOf == null || plantedDate == null || asOf.isBefore(plantedDate)) {
            return false;
        }
        return removedDate == null || !asOf.isAfter(removedDate);
    }

    /** Whether the variety was recorded, as distinct from merely knowing the plant. */
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
                .entityNameOrNull(plantName, "plantName")
                .entityNameOrNull(cultivarName, "cultivarName")
                .isTrue(plantName != null || cultivarName != null, "plantOrCultivarKnown")
                .entityName(zoneName, "zoneName")
                .entityNameOrNull(subZoneName, "subZoneName")
                .notNull(plantedDate, "plantedDate")
                .whenNotNull(plantCount, c -> c.atLeast(plantCount, 1, "plantCount"))
                .isTrue(removedDate == null || plantedDate == null
                        || !removedDate.isBefore(plantedDate), "removedNotBeforePlanted");
    }
}
