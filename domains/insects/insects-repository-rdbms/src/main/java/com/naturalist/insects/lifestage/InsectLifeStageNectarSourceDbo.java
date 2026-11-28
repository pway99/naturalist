package com.naturalist.insects.lifestage;

import com.naturalist.observability.Constraints;
import com.naturalist.observability.Level;
import com.naturalist.observability.Observer;
import com.naturalist.persistence.Dbo;
import com.naturalist.persistence.DboSchema;
import com.naturalist.persistence.Fk;
import com.naturalist.plants.PlantSpeciesName;

import java.util.function.Consumer;

/** Ordered child row: one {@code PlantSpeciesName} slug (cross-domain, no FK). Carries the life stage NAME. */
@DboSchema(table = "insect_life_stage_nectar_source", primaryKey = "life_stage_id,ordinal",
        foreignKeys = @Fk(columns = "life_stage_id", references = "insect_life_stage(id)"),
        entity = LifeStage.class)
final class InsectLifeStageNectarSourceDbo implements Dbo {
    String lifeStageName;
    Integer ordinal;
    String plantName;

    private static final Observer observer = Observer.forClass(InsectLifeStageNectarSourceDbo.class);

    static InsectLifeStageNectarSourceDbo from(String lifeStageName, Integer ordinal, PlantSpeciesName plant) {
        observer.arguments("from", i -> i
                .kebabFormat(lifeStageName, "lifeStageName")
                .atLeast(ordinal, 0, "ordinal")
                .entityName(plant, "plant"))
                .throwWhenInvalid();
        InsectLifeStageNectarSourceDbo d = new InsectLifeStageNectarSourceDbo();
        d.lifeStageName = lifeStageName;
        d.ordinal = ordinal;
        d.plantName = plant.value();
        return d;
    }

    PlantSpeciesName toName() {
        observer.state("toName", s -> s.observable(this, "row")).observe(Level.WARN);
        return PlantSpeciesName.of(plantName);
    }

    @Override
    public Consumer<? extends Constraints> invariants() {
        return c -> c
                .kebabFormat(lifeStageName, "lifeStageName")
                .atLeast(ordinal, 0, "ordinal")
                .kebabFormat(plantName, "plantName")
                .maxLength(plantName, 64, "plantName");
    }
}
