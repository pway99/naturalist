package com.naturalist.garden;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.As;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import com.naturalist.ddd.Entity;
import com.naturalist.observability.Constraints;
import com.naturalist.plants.PlantFamilyName;
import com.naturalist.plants.PlantGenusName;
import com.naturalist.plants.PlantSpeciesName;
import com.naturalist.plants.PlantRankName;
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
 * <b>Two independent botanical axes, both soft names into the plants domain, both optional.</b>
 * {@code plantName} is the Linnaean identification at whatever rank the gardener can support — a
 * {@link PlantRankName}, so family, genus or species are all expressible. A tray of unlabelled
 * salvia starts is a genus-rank planting, not a missing one. {@code cultivarName} is the
 * orthogonal horticultural selection within a species — breeding status, fruit type, seed-saving
 * policy — and is null whenever the variety is not known or not catalogued, the common case for
 * a tray of lettuce.
 * <p>
 * The two are separate components rather than one union because a cultivar is not a rank: it is a
 * selection <em>within</em> a species, the same way a clade placement is a classification
 * alongside rank rather than a rung of it. Keeping them apart also lets a planting state both at
 * once — species and variety — which a union could not express. See section D of
 * {@code docs/plans/organism-domain-blueprint.md}.
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
        @JsonTypeInfo(use = Id.NAME, property = "plantRank", include = As.EXTERNAL_PROPERTY)
        @JsonSubTypes({
                @Type(value = PlantFamilyName.class, name = "FAMILY"),
                @Type(value = PlantGenusName.class, name = "GENUS"),
                @Type(value = PlantSpeciesName.class, name = "SPECIES")
        })
        @Nullable PlantRankName plantName,
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
                .whenNotNull(plantName, c -> c.identifier(plantName, "plantName"))
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
