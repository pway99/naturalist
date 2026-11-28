package com.naturalist.insects.lifestage;

import com.naturalist.insects.InsectSpeciesName;
import com.naturalist.observability.Constraints;
import com.naturalist.observability.Observer;
import com.naturalist.persistence.Dbo;
import com.naturalist.persistence.DboSchema;
import com.naturalist.persistence.Fk;

import java.util.function.Consumer;

/** Ordered child row: one parasitoid-host {@code InsectSpeciesName} slug (soft ref, no FK for parity). */
@DboSchema(table = "insect_life_stage_parasitoid_host", primaryKey = "life_stage_id,ordinal",
        foreignKeys = @Fk(columns = "life_stage_id", references = "insect_life_stage(id)"),
        entity = LifeStage.class)
final class InsectLifeStageParasitoidHostDbo implements Dbo {
    String lifeStageName;
    Integer ordinal;
    String speciesName;

    private static final Observer observer = Observer.forClass(InsectLifeStageParasitoidHostDbo.class);

    static InsectLifeStageParasitoidHostDbo from(String lifeStageName, Integer ordinal, InsectSpeciesName species) {
        observer.arguments("from", i -> i
                .kebabFormat(lifeStageName, "lifeStageName")
                .atLeast(ordinal, 0, "ordinal")
                .entityName(species, "species"))
                .throwWhenInvalid();
        InsectLifeStageParasitoidHostDbo d = new InsectLifeStageParasitoidHostDbo();
        d.lifeStageName = lifeStageName;
        d.ordinal = ordinal;
        d.speciesName = species.value();
        return d;
    }

    InsectSpeciesName toName() {
        return InsectSpeciesName.of(speciesName);
    }

    @Override
    public Consumer<? extends Constraints> invariants() {
        return c -> c
                .kebabFormat(lifeStageName, "lifeStageName")
                .atLeast(ordinal, 0, "ordinal")
                .kebabFormat(speciesName, "speciesName")
                .maxLength(speciesName, 96, "speciesName");
    }
}
