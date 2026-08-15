package com.naturalist.soil;

import com.naturalist.observability.Level;
import com.naturalist.observability.Observer;
import com.naturalist.soil.observation.LabAnalysis;
import com.naturalist.soil.observation.LabAnalysisInfo;
import com.naturalist.soil.observation.LabAnalysisInfoQuery;
import com.naturalist.soil.observation.MicroNutrients;
import com.naturalist.soil.observation.NutrientName;
import com.naturalist.soil.observation.NutrientPanel;
import com.naturalist.soil.observation.NutrientReading;
import com.naturalist.soil.observation.NutrientReadingQuery;
import com.naturalist.soil.observation.Nutrients;
import com.naturalist.soil.observation.PrimaryNutrients;
import com.naturalist.soil.observation.ReportedOptimumQuery;
import com.naturalist.soil.observation.ReportedRecommendationQuery;
import com.naturalist.soil.observation.SecondaryNutrients;
import com.naturalist.soil.observation.SoilPhysicalCharacteristics;
import com.naturalist.soil.observation.SoilPhysicalCharacteristicsQuery;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Name-keyed assembly of the {@link SoilProfile} read model from its persisted parts: the
 * {@link SoilProfileInfo} root, its {@link LabAnalysisInfo} headers, and — per analysis — the
 * {@link NutrientReading}s (bucketed into a {@link NutrientPanel} by {@link NutrientName} via the
 * {@link Nutrients} catalog) and the {@link SoilPhysicalCharacteristics} row.
 * <p>
 * Package-private concrete factory (no interface, no {@code Impl} suffix) per ADR-020, mirroring
 * {@code InsectTaxonViewFactory}. Per the producer/consumer rule (ADR-017) it validates its own
 * arguments with {@code throwWhenInvalid()} but only observes the assembled profile (metrics).
 */
class SoilProfileFactory {

    private final Observer observer = Observer.forClass(getClass());
    private final SoilProfileInfoQuery soilProfileInfoQuery;
    private final LabAnalysisInfoQuery labAnalysisInfoQuery;
    private final NutrientReadingQuery nutrientReadingQuery;
    private final SoilPhysicalCharacteristicsQuery physicalCharacteristicsQuery;
    private final ReportedOptimumQuery reportedOptimumQuery;
    private final ReportedRecommendationQuery reportedRecommendationQuery;

    SoilProfileFactory(SoilProfileInfoQuery soilProfileInfoQuery,
                       LabAnalysisInfoQuery labAnalysisInfoQuery,
                       NutrientReadingQuery nutrientReadingQuery,
                       SoilPhysicalCharacteristicsQuery physicalCharacteristicsQuery,
                       ReportedOptimumQuery reportedOptimumQuery,
                       ReportedRecommendationQuery reportedRecommendationQuery) {
        observer.arguments("constructor", i -> i
                        .notNull(soilProfileInfoQuery, "soilProfileInfoQuery")
                        .notNull(labAnalysisInfoQuery, "labAnalysisInfoQuery")
                        .notNull(nutrientReadingQuery, "nutrientReadingQuery")
                        .notNull(physicalCharacteristicsQuery, "physicalCharacteristicsQuery")
                        .notNull(reportedOptimumQuery, "reportedOptimumQuery")
                        .notNull(reportedRecommendationQuery, "reportedRecommendationQuery"))
                .throwWhenInvalid();
        this.soilProfileInfoQuery = soilProfileInfoQuery;
        this.labAnalysisInfoQuery = labAnalysisInfoQuery;
        this.nutrientReadingQuery = nutrientReadingQuery;
        this.physicalCharacteristicsQuery = physicalCharacteristicsQuery;
        this.reportedOptimumQuery = reportedOptimumQuery;
        this.reportedRecommendationQuery = reportedRecommendationQuery;
    }

    Optional<SoilProfile> buildByName(SoilProfileName name) {
        observer.arguments("buildByName", i -> i.entityName(name, "name")).throwWhenInvalid();
        return soilProfileInfoQuery.getByName(name)
                .map(info -> observe(new SoilProfile(info, assembleAnalyses(name))));
    }

    private List<LabAnalysis> assembleAnalyses(SoilProfileName name) {
        return labAnalysisInfoQuery.forSoilProfileName(name).stream()
                .map(this::assembleAnalysis)
                .toList();
    }

    private LabAnalysis assembleAnalysis(LabAnalysisInfo info) {
        NutrientPanel panel = assemblePanel(nutrientReadingQuery.forLabAnalysisId(info.id()).stream().toList());
        return new LabAnalysis(
                info,
                panel,
                physicalCharacteristicsQuery.forLabAnalysisId(info.id()),
                // Beside the panel, never inside it — the readings are measurement, these are the
                // lab's targets for the submitted crop and the advice it gave about them.
                reportedOptimumQuery.forLabAnalysisId(info.id()),
                reportedRecommendationQuery.forLabAnalysisId(info.id()));
    }

    /**
     * Buckets an analysis's readings into the panel's named slots. A slot with no matching reading
     * stays empty — the lab did not report that nutrient. This is the intended path, not a
     * degraded one: the slots name the FGL tomato panel, and another crop's panel legitimately
     * omits rows.
     */
    private NutrientPanel assemblePanel(List<NutrientReading> readings) {
        Map<NutrientName, NutrientReading> byName = readings.stream()
                .collect(Collectors.toMap(NutrientReading::nutrientName, Function.identity(), (a, b) -> a));
        return new NutrientPanel(
                new PrimaryNutrients(
                        slot(byName, Nutrients.NITRATE_N),
                        slot(byName, Nutrients.PHOSPHORUS_P2O5),
                        slot(byName, Nutrients.POTASSIUM_EXCHANGEABLE),
                        slot(byName, Nutrients.POTASSIUM_SOLUBLE)),
                new SecondaryNutrients(
                        slot(byName, Nutrients.CALCIUM_EXCHANGEABLE),
                        slot(byName, Nutrients.CALCIUM_SOLUBLE),
                        slot(byName, Nutrients.MAGNESIUM_EXCHANGEABLE),
                        slot(byName, Nutrients.MAGNESIUM_SOLUBLE),
                        slot(byName, Nutrients.SODIUM_EXCHANGEABLE),
                        slot(byName, Nutrients.SODIUM_SOLUBLE),
                        slot(byName, Nutrients.SULFATE)),
                new MicroNutrients(
                        slot(byName, Nutrients.ZINC),
                        slot(byName, Nutrients.MANGANESE),
                        slot(byName, Nutrients.IRON),
                        slot(byName, Nutrients.COPPER),
                        slot(byName, Nutrients.BORON),
                        slot(byName, Nutrients.CHLORIDE)));
    }

    private static Optional<NutrientReading> slot(Map<NutrientName, NutrientReading> byName,
                                                  NutrientName nutrientName) {
        return Optional.ofNullable(byName.get(nutrientName));
    }

    private SoilProfile observe(SoilProfile soilProfile) {
        observer.observable(soilProfile, "soilProfile").observe(Level.WARN);
        return soilProfile;
    }
}
