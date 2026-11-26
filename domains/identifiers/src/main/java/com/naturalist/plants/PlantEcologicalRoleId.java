package com.naturalist.plants;

import com.naturalist.ddd.EntityId;

import java.util.UUID;

/**
 * Surrogate identity for {@code com.naturalist.plants.PlantEcologicalRole} — the
 * cross-rank record carrying what a taxon <em>does</em> in the garden.
 * <p>
 * A surrogate rather than a natural key because the record's identity is the
 * assignment, not the taxon: uniqueness lives on {@code plantName}, and the role set
 * changes as the garden is observed without the record becoming a different record.
 */
public final class PlantEcologicalRoleId extends EntityId {

    private PlantEcologicalRoleId(UUID value) {
        super(value);
    }

    public static PlantEcologicalRoleId of(UUID value) {
        return new PlantEcologicalRoleId(value);
    }

    public static PlantEcologicalRoleId create() {
        return new PlantEcologicalRoleId(EntityId.newUUID());
    }
}
