package com.naturalist.soil.observation;

import com.naturalist.ddd.ReadModel;
import com.naturalist.observability.Constraints;

import java.util.Optional;
import java.util.function.Consumer;

/**
 * A complete soil laboratory analysis, assembled from its persisted parts: the
 * {@link LabAnalysisInfo} header, the {@link NutrientPanel} (built from the analysis's
 * {@link NutrientReading} entities), and the {@link SoilPhysicalCharacteristics} row. A
 * {@link ReadModel} composed on read — never stored — mirroring the {@code SoilProfileInfo} /
 * {@code SoilProfile} convention (bare noun = assembled read model, {@code *Info} = persisted fact).
 * <p>
 * The header is required; the chemistry is not. {@code physicalCharacteristics} is
 * {@link Optional} for the same reason the panel's slots are: it is a separate persisted row that
 * a given analysis may not have. The assembling factory reads it through an {@code Optional}
 * query and cannot promise it exists.
 * <p>
 * <b>{@code reportedOptima} is a sibling of the panel, not part of it.</b> The optima the lab
 * printed sit beside the measurements, never inside them: a {@link NutrientReading} is what the
 * instrument found, a {@link ReportedOptimum} is what the lab believes good looks like for the
 * submitted crop, and fusing the two is the change this domain most explicitly rejects. Empty for
 * an analysis whose optima have not been transcribed — the readings stand on their own.
 */
public record LabAnalysis(
        LabAnalysisInfo info,
        NutrientPanel nutrients,
        Optional<SoilPhysicalCharacteristics> physicalCharacteristics,
        ReportedOptimumCollection reportedOptima
) implements ReadModel {

    public LabAnalysis {
        physicalCharacteristics =
                physicalCharacteristics == null ? Optional.empty() : physicalCharacteristics;
        reportedOptima = reportedOptima == null ? ReportedOptimumCollection.empty() : reportedOptima;
    }

    @Override
    public Consumer<? extends Constraints> invariants() {
        return i -> i
                .namedEntity(info, "info")
                .readModel(nutrients, "nutrients")
                .namedEntityOrNull(physicalCharacteristics.orElse(null), "physicalCharacteristics")
                .behavioralCollection(reportedOptima, "reportedOptima");
    }
}
