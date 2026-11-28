package com.naturalist.insects.lifestage;

import com.naturalist.observability.Constraints;
import com.naturalist.observability.Observer;
import com.naturalist.persistence.Dbo;
import com.naturalist.persistence.DboSchema;
import com.naturalist.persistence.Fk;
import com.naturalist.plants.PlantSpeciesName;

import java.util.function.Consumer;

/** Ordered child row: one {@code PlantSpeciesName} slug (cross-domain, no FK). Carries the life stage NAME. */
@DboSchema(table = "insect_life_stage_host_plant", primaryKey = "life_stage_id,ordinal",
        foreignKeys = @Fk(columns = "life_stage_id", references = "insect_life_stage(id)"),
        entity = LifeStage.class)
final class InsectLifeStageHostPlantDbo implements Dbo {
    String lifeStageName;
    Integer ordinal;
    String plantName;

    private static final Observer observer = Observer.forClass(InsectLifeStageHostPlantDbo.class);

    static InsectLifeStageHostPlantDbo from(String lifeStageName, Integer ordinal, PlantSpeciesName plant) {
        observer.arguments("from", i -> i
                .kebabFormat(lifeStageName, "lifeStageName")
                .atLeast(ordinal, 0, "ordinal")
                .entityName(plant, "plant"))
                .throwWhenInvalid();
        InsectLifeStageHostPlantDbo d = new InsectLifeStageHostPlantDbo();
        d.lifeStageName = lifeStageName;
        d.ordinal = ordinal;
        d.plantName = plant.value();
        return d;
    }

    PlantSpeciesName toName() {
        return PlantSpeciesName.of(plantName);
    }

    @Override
    public Consumer<? extends Constraints> invariants() {
        return c -> c
                .kebabFormat(lifeStageName, "lifeStageName")
                .atLeast(ordinal, 0, "ordinal")
                .kebabFormat(plantName, "plantName")
                .maxLength(plantName, 96, "plantName");
    }
}
