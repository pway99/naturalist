package com.naturalist.plants;

import com.naturalist.observability.Constraints;
import com.naturalist.observability.Observer;
import com.naturalist.persistence.Dbo;
import com.naturalist.persistence.DboSchema;
import com.naturalist.taxonomy.LinealRank;

import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * Parent row of {@link PlantEcologicalRole} — a surrogate-UUID {@code Entity} keyed by
 * {@link PlantEcologicalRoleId}. The polymorphic {@link PlantRankName} it describes splits into a
 * {@code plant_rank} discriminator plus a {@code plant_name} slug (no FK — it spans four rank
 * tables), rebuilt in-module via {@link PlantRankName#of(String, LinealRank)}. The non-empty
 * {@code Set<PlantRole>} becomes the {@link PlantEcologicalRoleRoleDbo} child table, loaded in one
 * batched query and passed to {@link #toEntity(Set)}.
 *
 * <p>The {@code UNIQUE(plant_rank, plant_name)} is a composite constraint — enforced by the DDL
 * only, deliberately omitted from {@code @DboSchema.unique}.
 */
@DboSchema(table = "plant_ecological_role", primaryKey = "id", entity = PlantEcologicalRole.class)
final class PlantEcologicalRoleDbo implements Dbo {
    String id;         // the PlantEcologicalRoleId's UUID as text; mapper casts it (::uuid)
    String plantRank;  // ORDER|FAMILY|GENUS|SPECIES — plantName.rank().name()
    String plantName;  // rank-name slug (polymorphic, no FK) — plantName.value()

    static PlantEcologicalRoleDbo from(PlantEcologicalRole role) {
        PlantEcologicalRoleDbo d = new PlantEcologicalRoleDbo();
        d.id = role.id().value().toString();
        d.plantRank = role.plantName().rank().name();
        d.plantName = role.plantName().value();
        Observer.forClass(PlantEcologicalRoleDbo.class)
                .arguments("from", i -> i.observable(d, "dbo"))
                .throwWhenInvalid();
        return d;
    }

    PlantEcologicalRole toEntity(Set<PlantRole> roles) {
        return new PlantEcologicalRole(
                PlantEcologicalRoleId.of(UUID.fromString(id)),
                PlantRankName.of(plantName, LinealRank.valueOf(plantRank)),
                roles);
    }

    @Override
    public Consumer<? extends Constraints> invariants() {
        return c -> c
                .notBlank(id, "id")
                .notBlank(plantRank, "plantRank").maxLength(plantRank, 16, "plantRank")
                .notNull(plantName, "plantName").kebabFormat(plantName, "plantName").maxLength(plantName, 64, "plantName");
    }
}
