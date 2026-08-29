package com.naturalist.garden;

import com.naturalist.observability.Constraints;
import com.naturalist.observability.Observer;
import com.naturalist.persistence.Dbo;
import com.naturalist.persistence.DboSchema;
import com.naturalist.plants.PlantRankName;
import com.naturalist.plants.cultivar.CultivarName;
import com.naturalist.taxonomy.LinealRank;
import com.naturalist.zone.ZoneName;
import com.naturalist.zone.subzone.SubZoneName;

import java.time.LocalDate;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Flat persistence view of {@link Planting} — a surrogate-UUID {@code Entity} with no owned children and
 * no foreign keys. Its {@code plantName} is a nullable polymorphic {@link PlantRankName} stored as a
 * {@code plant_rank} discriminator + {@code plant_name} slug (no FK; rebuilt in-module via
 * {@link PlantRankName#of(String, LinealRank)}). The cultivar and zone/sub-zone references are plain
 * slug columns (cross-domain / zone-skeleton, no FK). The uuid {@code id} travels as text with a
 * {@code ::uuid} cast; {@code LocalDate}s use MyBatis's built-in handler.
 */
@DboSchema(table = "planting", primaryKey = "id", entity = Planting.class)
final class PlantingDbo implements Dbo {
    String id;
    String plantRank;      // nullable
    String plantName;      // nullable
    String cultivarName;   // nullable
    String zoneName;
    String subZoneName;    // nullable
    Integer plantCount;    // nullable
    LocalDate plantedDate;
    LocalDate removedDate; // nullable
    String notes;          // nullable

    static PlantingDbo from(Planting p) {
        PlantingDbo d = new PlantingDbo();
        d.id = p.id().value().toString();
        if (p.plantName() != null) {
            d.plantRank = p.plantName().rank().name();
            d.plantName = p.plantName().value();
        }
        d.cultivarName = p.cultivarName() == null ? null : p.cultivarName().value();
        d.zoneName = p.zoneName().value();
        d.subZoneName = p.subZoneName() == null ? null : p.subZoneName().value();
        d.plantCount = p.plantCount();
        d.plantedDate = p.plantedDate();
        d.removedDate = p.removedDate();
        d.notes = p.notes();
        Observer.forClass(PlantingDbo.class)
                .arguments("from", i -> i.observable(d, "dbo"))
                .throwWhenInvalid();
        return d;
    }

    Planting toEntity() {
        return new Planting(
                PlantingId.of(UUID.fromString(id)),
                plantName == null ? null : PlantRankName.of(plantName, LinealRank.valueOf(plantRank)),
                cultivarName == null ? null : CultivarName.of(cultivarName),
                ZoneName.of(zoneName),
                subZoneName == null ? null : SubZoneName.of(subZoneName),
                plantCount,
                plantedDate,
                removedDate,
                notes);
    }

    @Override
    public Consumer<? extends Constraints> invariants() {
        return c -> c
                .notBlank(id, "id")
                .maxLength(plantRank, 16, "plantRank")
                .maxLength(plantName, 64, "plantName")
                .maxLength(cultivarName, 64, "cultivarName")
                .notNull(zoneName, "zoneName").kebabFormat(zoneName, "zoneName").maxLength(zoneName, 64, "zoneName")
                .maxLength(subZoneName, 64, "subZoneName")
                .notNull(plantedDate, "plantedDate");
    }
}
