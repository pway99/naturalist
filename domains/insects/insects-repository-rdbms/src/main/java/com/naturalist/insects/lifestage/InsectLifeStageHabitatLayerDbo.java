package com.naturalist.insects.lifestage;

import com.naturalist.habitat.VerticalLayer;
import com.naturalist.observability.Constraints;
import com.naturalist.observability.Observer;
import com.naturalist.persistence.Dbo;
import com.naturalist.persistence.DboSchema;
import com.naturalist.persistence.Fk;

import java.util.Arrays;
import java.util.function.Consumer;

/** Child row for one {@link VerticalLayer} of a life stage's habitat profile. Carries the life stage NAME. */
@DboSchema(table = "insect_life_stage_habitat_layer", primaryKey = "life_stage_id,layer",
        foreignKeys = @Fk(columns = "life_stage_id", references = "insect_life_stage(id)"),
        entity = LifeStage.class)
final class InsectLifeStageHabitatLayerDbo implements Dbo {
    String lifeStageName;
    String layer;

    private static final Observer observer = Observer.forClass(InsectLifeStageHabitatLayerDbo.class);

    static InsectLifeStageHabitatLayerDbo from(String lifeStageName, VerticalLayer layer) {
        observer.arguments("from", i -> i
                .kebabFormat(lifeStageName, "lifeStageName")
                .notNull(layer, "layer"))
                .throwWhenInvalid();
        InsectLifeStageHabitatLayerDbo d = new InsectLifeStageHabitatLayerDbo();
        d.lifeStageName = lifeStageName;
        d.layer = layer.name();
        return d;
    }

    VerticalLayer toLayer() {
        return Arrays.stream(VerticalLayer.values()).filter(l -> l.name().equals(layer)).findFirst().orElse(null);
    }

    @Override
    public Consumer<? extends Constraints> invariants() {
        return c -> c
                .kebabFormat(lifeStageName, "lifeStageName")
                .notBlank(layer, "layer")
                .maxLength(layer, 24, "layer")
                .isTrue(isVerticalLayer(layer), "isVerticalLayer");
    }

    /** Whether {@code value} is the name of a {@link VerticalLayer} constant; null is not. */
    private static boolean isVerticalLayer(String value) {
        return value != null && Arrays.stream(VerticalLayer.values()).anyMatch(v -> v.name().equals(value));
    }
}
