package com.naturalist.insects.lifestage;

import com.naturalist.habitat.VerticalLayer;
import com.naturalist.observability.Constraints;
import com.naturalist.observability.Observer;
import com.naturalist.persistence.Dbo;
import com.naturalist.persistence.DboSchema;
import com.naturalist.persistence.Fk;

import java.util.function.Consumer;

/** Child row for one {@link VerticalLayer} of a life stage's habitat profile. Carries the life stage NAME. */
@DboSchema(table = "insect_life_stage_habitat_layer", primaryKey = "life_stage_id,layer",
           foreignKeys = @Fk(columns = "life_stage_id", references = "insect_life_stage(id)"),
           entity = LifeStage.class)
final class InsectLifeStageHabitatLayerDbo implements Dbo {
    String lifeStageName;
    String layer;

    static InsectLifeStageHabitatLayerDbo from(String lifeStageName, VerticalLayer layer) {
        InsectLifeStageHabitatLayerDbo d = new InsectLifeStageHabitatLayerDbo();
        d.lifeStageName = lifeStageName;
        d.layer = layer.name();
        Observer.forClass(InsectLifeStageHabitatLayerDbo.class)
                .arguments("from", i -> i.observable(d, "dbo")).throwWhenInvalid();
        return d;
    }

    VerticalLayer toLayer() { return VerticalLayer.valueOf(layer); }

    @Override
    public Consumer<? extends Constraints> invariants() {
        return c -> c.notNull(lifeStageName, "lifeStageName").kebabFormat(lifeStageName, "lifeStageName")
                .notBlank(layer, "layer").maxLength(layer, 24, "layer");
    }
}
