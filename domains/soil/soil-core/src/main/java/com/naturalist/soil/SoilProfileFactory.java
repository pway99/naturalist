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

    SoilProfileFactory(SoilProfileInfoQuery soilProfileInfoQuery,
                       LabAnalysisInfoQuery labAnalysisInfoQuery,
                       NutrientReadingQuery nutrientReadingQuery,
                       SoilPhysicalCharacteristicsQuery physicalCharacteristicsQuery) {
        observer.arguments("constructor", i -> i
                        .notNull(soilProfileInfoQuery, "soilProfileInfoQuery")
                        .notNull(labAnalysisInfoQuery, "labAnalysisInfoQuery")
                        .notNull(nutrientReadingQuery, "nutrientReadingQuery")
                        .notNull(physicalCharacteristicsQuery, "physicalCharacteristicsQuery"))
                .throwWhenInvalid();
        this.soilProfileInfoQuery = soilProfileInfoQuery;
        this.labAnalysisInfoQuery = labAnalysisInfoQuery;
        this.nutrientReadingQuery = nutrientReadingQuery;
        this.physicalCharacteristicsQuery = physicalCharacteristicsQuery;
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
        SoilPhysicalCharacteristics physical = physicalCharacteristicsQuery.forLabAnalysisId(info.id()).orElse(null);
        return new LabAnalysis(info, panel, physical);
    }

    private NutrientPanel assemblePanel(List<NutrientReading> readings) {
        Map<NutrientName, NutrientReading> byName = readings.stream()
                .collect(Collectors.toMap(NutrientReading::nutrientName, Function.identity(), (a, b) -> a));
        return new NutrientPanel(
                new PrimaryNutrients(
                        byName.get(Nutrients.NITRATE_N),
                        byName.get(Nutrients.PHOSPHORUS_P2O5),
                        byName.get(Nutrients.POTASSIUM_EXCHANGEABLE),
                        byName.get(Nutrients.POTASSIUM_SOLUBLE)),
                new SecondaryNutrients(
                        byName.get(Nutrients.CALCIUM_EXCHANGEABLE),
                        byName.get(Nutrients.CALCIUM_SOLUBLE),
                        byName.get(Nutrients.MAGNESIUM_EXCHANGEABLE),
                        byName.get(Nutrients.MAGNESIUM_SOLUBLE),
                        byName.get(Nutrients.SODIUM_EXCHANGEABLE),
                        byName.get(Nutrients.SODIUM_SOLUBLE),
                        byName.get(Nutrients.SULFATE)),
                new MicroNutrients(
                        byName.get(Nutrients.ZINC),
                        byName.get(Nutrients.MANGANESE),
                        byName.get(Nutrients.IRON),
                        byName.get(Nutrients.COPPER),
                        byName.get(Nutrients.BORON),
                        byName.get(Nutrients.CHLORIDE)));
    }

    private SoilProfile observe(SoilProfile soilProfile) {
        observer.observable(soilProfile, "soilProfile").observe(Level.WARN);
        return soilProfile;
    }
}
