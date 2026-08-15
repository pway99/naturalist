package com.naturalist.soil;

import com.naturalist.data.NaturalistDatabaseExtension;
import com.naturalist.exception.InvariantViolationException;
import com.naturalist.garden.CropName;
import com.naturalist.soil.observation.LabAnalysis;
import com.naturalist.soil.observation.LabAnalysisId;
import com.naturalist.soil.observation.LabAnalysisInfo;
import com.naturalist.soil.observation.LabAnalysisInfoTestEntitySource;
import com.naturalist.soil.observation.MeasurementUnit;
import com.naturalist.soil.observation.NutrientReading;
import com.naturalist.soil.observation.NutrientReadingId;
import com.naturalist.soil.observation.NutrientReadingTestEntitySource;
import com.naturalist.soil.observation.Nutrients;
import com.naturalist.soil.observation.RecommendedAmount;
import com.naturalist.soil.observation.RecommendedInputs;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Exercises the full assembled read path: {@link SoilProfileQuery} → {@link SoilProfileFactory} →
 * the four entity queries → their repository mocks, over the real March 2026 fixture data. Verifies
 * a profile surfaces its dated analyses with their nutrient panels and physical characteristics.
 */
class SoilProfileFactoryTest {

    @RegisterExtension
    NaturalistDatabaseExtension db = NaturalistDatabaseExtension.create();

    SoilProfileQuery query = SoilsTestContextInternal.create(db).soilProfileQuery();

    @Test
    void getBySoilProfileName_nullArgument_throws() {
        assertThatThrownBy(() -> query.getBySoilProfileName(null))
                .isInstanceOf(InvariantViolationException.class)
                .hasMessageContainingAll("soilProfileName");
    }

    @Test
    void getBySoilProfileName_unknownProfile_returnsEmpty() {
        assertThat(query.getBySoilProfileName(TestSoilIdentifiers.SoilProfiles.NotFound.soilProfile))
                .isEmpty();
    }

    @Test
    void getBySoilProfileName_box1_assemblesProfileWithPanelAndCharacteristics() {
        Optional<SoilProfile> result =
                query.getBySoilProfileName(TestSoilIdentifiers.SoilProfiles.Box1.name);

        assertThat(result).isPresent();
        SoilProfile profile = result.get();
        assertThat(profile.soilProfileName()).isEqualTo(TestSoilIdentifiers.SoilProfiles.Box1.name);
        assertThat(profile.labAnalyses())
                .extracting(la -> la.info().id())
                .containsExactly(TestSoilIdentifiers.SoilProfiles.Box1.LabAnalyses.labAnalysis);

        LabAnalysis analysis = profile.latestLabAnalysis().orElseThrow();
        // The nutrient panel was assembled from the readings...
        assertThat(analysis.nutrients().secondary().calciumSoluble().orElseThrow().value())
                .isEqualByComparingTo("6.99");
        assertThat(analysis.nutrients().micro().boron().orElseThrow().value())
                .isEqualByComparingTo("0.0202");
        // ...and the physical characteristics attached.
        assertThat(analysis.physicalCharacteristics().orElseThrow().pH().value())
                .isEqualByComparingTo("7.2");
        assertThat(analysis.physicalCharacteristics().orElseThrow().cecMeqPer100g().value())
                .isEqualByComparingTo("44.9");
    }

    /**
     * The report's own words assemble beside the measurements: seventeen optima and fourteen
     * recommendation rows, none of them folded into a reading.
     */
    @Test
    void getBySoilProfileName_box1_carriesWhatTheReportSaidBesideWhatItMeasured() {
        LabAnalysis analysis = query.getBySoilProfileName(TestSoilIdentifiers.SoilProfiles.Box1.name)
                .orElseThrow().latestLabAnalysis().orElseThrow();

        assertThat(analysis.reportedOptima().stream()).hasSize(17);
        assertThat(analysis.reportedRecommendations().stream()).hasSize(14);

        // Three of the twelve fertilisation rows recommend an actual application; the rest are an
        // explicit "apply none", which is advice, not silence.
        assertThat(analysis.reportedRecommendations().applicationsOnly().stream()
                .filter(r -> r.unit() == MeasurementUnit.LBS_PER_1000_SQFT))
                .hasSize(3);
        assertThat(analysis.reportedRecommendations()
                .forInput(RecommendedInputs.POTASSIUM_K2O).orElseThrow().amount())
                .isEqualTo(new RecommendedAmount.Quantity(new BigDecimal("11.2")));
        assertThat(analysis.reportedRecommendations()
                .forInput(RecommendedInputs.NITROGEN).orElseThrow().amount())
                .isInstanceOf(RecommendedAmount.None.class);
    }

    /**
     * The Phase 1 case: a lab panel that does not carry all seventeen tomato-panel rows still
     * assembles. The analysis inserted here is synthetic — deliberately so. It is not an FGL
     * report, it proves a structural property, and it must not be mistaken for real Oak Vista
     * chemistry, so it carries an obviously fake lab and sample id and lives in this test rather
     * than in the JSON catalog.
     */
    @Test
    void getBySoilProfileName_analysisReportingOneNutrient_assemblesWithEmptySlots() {
        LabAnalysisId shortPanel = LabAnalysisId.create();
        db.getNamed(LabAnalysisInfoTestEntitySource.class).insert(new LabAnalysisInfo(
                shortPanel,
                TestSoilIdentifiers.SoilProfiles.Box1.name,
                CropName.of("lettuce"),
                LocalDate.of(2026, 8, 17),
                "SYNTHETIC LAB",
                "XX 0000000-000",
                null,
                null,
                "Synthetic short panel — structural fixture, not a real report."));
        db.getNamed(NutrientReadingTestEntitySource.class).insert(new NutrientReading(
                NutrientReadingId.create(),
                Nutrients.NITRATE_N,
                shortPanel,
                new BigDecimal("2.10"),
                MeasurementUnit.LBS_PER_1000_SQFT));

        SoilProfile profile = query.getBySoilProfileName(TestSoilIdentifiers.SoilProfiles.Box1.name)
                .orElseThrow();

        LabAnalysis analysis = profile.labAnalyses().stream()
                .filter(la -> la.info().id().equals(shortPanel))
                .findFirst()
                .orElseThrow();
        assertThat(analysis.nutrients().primary().nitrateN().orElseThrow().value())
                .isEqualByComparingTo("2.10");
        // Every row the synthetic panel does not carry is absent, not zero, and not a failure.
        assertThat(analysis.nutrients().primary().phosphorusP2O5()).isEmpty();
        assertThat(analysis.nutrients().secondary().calciumSoluble()).isEmpty();
        assertThat(analysis.nutrients().micro().boron()).isEmpty();
        // It has no physical-characteristics row either — also absent rather than an NPE.
        assertThat(analysis.physicalCharacteristics()).isEmpty();
    }
}
