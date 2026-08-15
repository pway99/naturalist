package com.naturalist.soil.observation;

import com.naturalist.ddd.ReadModel;
import com.naturalist.observability.Constraints;

import java.util.List;
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
 * <b>{@code reportedOptima} and {@code reportedRecommendations} are siblings of the panel, not
 * part of it.</b> What the lab printed sits beside the measurements, never inside them: a
 * {@link NutrientReading} is what the instrument found, a {@link ReportedOptimum} is what the lab
 * believes good looks like for the submitted crop, and a {@link ReportedRecommendation} is what it
 * advised doing about the gap. Fusing any of them into the reading is the change this domain most
 * explicitly rejects. Both collections are empty for an analysis whose report has not been
 * transcribed — the readings stand on their own.
 */
public record LabAnalysis(
        LabAnalysisInfo info,
        NutrientPanel nutrients,
        Optional<SoilPhysicalCharacteristics> physicalCharacteristics,
        ReportedOptimumCollection reportedOptima,
        ReportedRecommendationCollection reportedRecommendations
) implements ReadModel {

    public LabAnalysis {
        physicalCharacteristics =
                physicalCharacteristics == null ? Optional.empty() : physicalCharacteristics;
        reportedOptima = reportedOptima == null ? ReportedOptimumCollection.empty() : reportedOptima;
        reportedRecommendations = reportedRecommendations == null
                ? ReportedRecommendationCollection.empty()
                : reportedRecommendations;
    }

    /**
     * The analysis as a reader should see one section of it: every catalogued nutrient of that
     * category, each paired with its measurement and its printed optimum, in the order the report
     * prints them. Absent rows are included as absent — a nutrient the lab did not run keeps its
     * place in the table rather than vanishing from it, because a shorter table silently reads as
     * a complete one.
     * <p>
     * Pure projection over data already assembled here. It exists so the console holds no rules:
     * see {@link NutrientLine}.
     */
    public List<NutrientLine> nutrientLines(NutrientCategory category) {
        return Nutrients.of(category).stream()
                .map(name -> NutrientLine.of(
                        name, nutrients.forNutrient(name), reportedOptima.forNutrient(name)))
                .toList();
    }

    @Override
    public Consumer<? extends Constraints> invariants() {
        return i -> i
                .namedEntity(info, "info")
                .readModel(nutrients, "nutrients")
                .namedEntityOrNull(physicalCharacteristics.orElse(null), "physicalCharacteristics")
                .behavioralCollection(reportedOptima, "reportedOptima")
                .behavioralCollection(reportedRecommendations, "reportedRecommendations");
    }
}
