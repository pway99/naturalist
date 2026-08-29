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
    int ordinal;
    String plantName;

    static InsectLifeStageHostPlantDbo from(String lifeStageName, int ordinal, PlantSpeciesName plant) {
        InsectLifeStageHostPlantDbo d = new InsectLifeStageHostPlantDbo();
        d.lifeStageName = lifeStageName;
        d.ordinal = ordinal;
        d.plantName = plant.value();
        Observer.forClass(InsectLifeStageHostPlantDbo.class).arguments("from", i -> i.observable(d, "dbo")).throwWhenInvalid();
        return d;
    }

    PlantSpeciesName toName() { return PlantSpeciesName.of(plantName); }

    @Override
    public Consumer<? extends Constraints> invariants() {
        return c -> c.notNull(lifeStageName, "lifeStageName").kebabFormat(lifeStageName, "lifeStageName")
                .notNull(plantName, "plantName").kebabFormat(plantName, "plantName").maxLength(plantName, 96, "plantName");
    }
}
