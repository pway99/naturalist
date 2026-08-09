package com.naturalist.soil.observation;

import com.naturalist.ddd.ReadModel;
import com.naturalist.observability.Constraints;

import java.util.function.Consumer;

/**
 * A complete soil laboratory analysis, assembled from its persisted parts: the
 * {@link LabAnalysisInfo} header, the {@link NutrientPanel} (built from the analysis's
 * {@link NutrientReading} entities), and the {@link SoilPhysicalCharacteristics} row. A
 * {@link ReadModel} composed on read — never stored — mirroring the {@code SoilProfileInfo} /
 * {@code SoilProfile} convention (bare noun = assembled read model, {@code *Info} = persisted fact).
 */
public record LabAnalysis(
        LabAnalysisInfo info,
        NutrientPanel nutrients,
        SoilPhysicalCharacteristics physicalCharacteristics
) implements ReadModel {

    @Override
    public Consumer<? extends Constraints> invariants() {
        return i -> i
                .namedEntity(info, "info")
                .readModel(nutrients, "nutrients")
                .namedEntity(physicalCharacteristics, "physicalCharacteristics");
    }
}
