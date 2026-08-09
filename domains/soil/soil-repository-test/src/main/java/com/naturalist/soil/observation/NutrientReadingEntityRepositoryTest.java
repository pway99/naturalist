package com.naturalist.soil.observation;

import com.naturalist.data.EntityRepositoryTest;
import com.naturalist.data.TestEntitySource;
import com.naturalist.exception.InvariantViolationException;
import com.naturalist.soil.TestSoilIdentifiers;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Behavioral contract for {@link NutrientReadingRepository}, plus the {@code getByLabAnalysisId}
 * (assemble a panel) and {@code getByNutrientName} (monitoring time series) reverse lookups.
 */
interface NutrientReadingEntityRepositoryTest extends EntityRepositoryTest<NutrientReadingId, NutrientReading> {

    @Override
    NutrientReadingRepository repository();

    @Override
    default TestEntitySource<NutrientReadingId, NutrientReading> source() {
        return db.getNamed(NutrientReadingTestEntitySource.class);
    }

    @Override
    default NutrientReadingId notFoundName() {
        return TestSoilIdentifiers.SoilProfiles.NotFound.nutrientReading;
    }

    @Override
    default List<NutrientReadingId> knownEntityNames() {
        return List.of(
                TestSoilIdentifiers.SoilProfiles.Box1.NutrientReadings.nitrateN,
                TestSoilIdentifiers.SoilProfiles.Box1.NutrientReadings.calciumSoluble);
    }

    @Override
    default NutrientReading newEntity() {
        return sample(NutrientReadingId.create());
    }

    @Override
    default NutrientReading ghostEntity() {
        return sample(NutrientReadingId.create());
    }

    @Override
    default NutrientReading modifiedEntity(NutrientReading original) {
        return new NutrientReading(
                original.id(),
                NutrientName.of("sulfate"),
                LabAnalysisId.create(),
                new BigDecimal("9.99"),
                MeasurementUnit.LBS_PER_1000_SQFT);
    }

    @Test
    default void getByLabAnalysisId_nullArgument() {
        assertThatThrownBy(() -> repository().getByLabAnalysisId(null))
                .isInstanceOf(InvariantViolationException.class)
                .hasMessageContainingAll("labAnalysisId");
    }

    @Test
    default void getByLabAnalysisId_known_returnsEveryNutrientForThatAnalysis() {
        List<NutrientReading> result =
                repository().getByLabAnalysisId(TestSoilIdentifiers.SoilProfiles.Box1.LabAnalyses.labAnalysis);

        assertThat(result).hasSize(17);
        assertThat(result).allSatisfy(reading ->
                assertThat(reading.labAnalysisId())
                        .isEqualTo(TestSoilIdentifiers.SoilProfiles.Box1.LabAnalyses.labAnalysis));
    }

    @Test
    default void getByNutrientName_nullArgument() {
        assertThatThrownBy(() -> repository().getByNutrientName(null))
                .isInstanceOf(InvariantViolationException.class)
                .hasMessageContainingAll("nutrientName");
    }

    @Test
    default void getByNutrientName_known_returnsOnePerAnalysisOverTime() {
        List<NutrientReading> result =
                repository().getByNutrientName(TestSoilIdentifiers.Nutrients.CALCIUM_SOLUBLE);

        assertThat(result).hasSize(4);
        assertThat(result).allSatisfy(reading ->
                assertThat(reading.nutrientName()).isEqualTo(TestSoilIdentifiers.Nutrients.CALCIUM_SOLUBLE));
    }

    @Test
    default void getByNutrientName_unknown_returnsEmpty() {
        assertThat(repository().getByNutrientName(TestSoilIdentifiers.SoilProfiles.NotFound.nutrientName))
                .isEmpty();
    }

    private static NutrientReading sample(NutrientReadingId id) {
        return new NutrientReading(
                id,
                NutrientName.of("nitrate-n"),
                LabAnalysisId.create(),
                new BigDecimal("1.23"),
                MeasurementUnit.LBS_PER_1000_SQFT);
    }
}
