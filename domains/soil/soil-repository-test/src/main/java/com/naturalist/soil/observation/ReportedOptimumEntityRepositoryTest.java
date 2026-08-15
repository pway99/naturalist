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
 * Behavioral contract for {@link ReportedOptimumRepository}, plus the {@code getByLabAnalysisId}
 * (an analysis's printed optima) and {@code getByNutrientName} (one target over time) reverse
 * lookups.
 */
interface ReportedOptimumEntityRepositoryTest extends EntityRepositoryTest<ReportedOptimumId, ReportedOptimum> {

    @Override
    ReportedOptimumRepository repository();

    @Override
    default TestEntitySource<ReportedOptimumId, ReportedOptimum> source() {
        return db.getNamed(ReportedOptimumTestEntitySource.class);
    }

    @Override
    default ReportedOptimumId notFoundName() {
        return TestSoilIdentifiers.SoilProfiles.NotFound.reportedOptimum;
    }

    @Override
    default List<ReportedOptimumId> knownEntityNames() {
        return List.of(
                TestSoilIdentifiers.SoilProfiles.Box1.ReportedOptima.nitrateN,
                TestSoilIdentifiers.SoilProfiles.Box1.ReportedOptima.sodiumSoluble);
    }

    @Override
    default ReportedOptimum newEntity() {
        return sample(ReportedOptimumId.create());
    }

    @Override
    default ReportedOptimum ghostEntity() {
        return sample(ReportedOptimumId.create());
    }

    /**
     * Every mutable field changed — including the range's <em>shape</em>, not just its numbers.
     * A closed range replaced by a ceiling-only one is the update most likely to break a naive
     * adapter, since it changes which JSON properties exist. {@code unit} is unchanged because
     * {@link MeasurementUnit} currently has one constant; it varies again when a second lab does.
     */
    @Override
    default ReportedOptimum modifiedEntity(ReportedOptimum original) {
        return new ReportedOptimum(
                original.id(),
                NutrientName.of("chloride"),
                LabAnalysisId.create(),
                new OptimumRange.UpperBounded(new BigDecimal("42")),
                MeasurementUnit.LBS_PER_1000_SQFT);
    }

    @Test
    default void getByLabAnalysisId_nullArgument() {
        assertThatThrownBy(() -> repository().getByLabAnalysisId(null))
                .isInstanceOf(InvariantViolationException.class)
                .hasMessageContainingAll("labAnalysisId");
    }

    @Test
    default void getByLabAnalysisId_unknown_returnsEmpty() {
        assertThat(repository().getByLabAnalysisId(TestSoilIdentifiers.SoilProfiles.NotFound.labAnalysis))
                .isEmpty();
    }

    @Test
    default void getByLabAnalysisId_known_returnsEveryOptimumForThatAnalysis() {
        List<ReportedOptimum> result =
                repository().getByLabAnalysisId(TestSoilIdentifiers.SoilProfiles.Box1.LabAnalyses.labAnalysis);

        assertThat(result).hasSize(17);
        assertThat(result).allSatisfy(optimum ->
                assertThat(optimum.labAnalysisId())
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
        List<ReportedOptimum> result =
                repository().getByNutrientName(TestSoilIdentifiers.Nutrients.CALCIUM_SOLUBLE);

        assertThat(result).hasSize(2);
        assertThat(result).allSatisfy(optimum ->
                assertThat(optimum.nutrientName()).isEqualTo(TestSoilIdentifiers.Nutrients.CALCIUM_SOLUBLE));
    }

    @Test
    default void getByNutrientName_unknown_returnsEmpty() {
        assertThat(repository().getByNutrientName(TestSoilIdentifiers.SoilProfiles.NotFound.nutrientName))
                .isEmpty();
    }

    /**
     * The upper-bounded row round-trips as an upper-bounded row. A ceiling flattened into a closed
     * range with an invented floor of zero is the failure this guards.
     */
    @Test
    default void getByName_upperBoundedRange_keepsItsShape() {
        ReportedOptimum result = repository()
                .getByName(TestSoilIdentifiers.SoilProfiles.Box1.ReportedOptima.sodiumSoluble)
                .orElseThrow();

        assertThat(result.range()).isInstanceOf(OptimumRange.UpperBounded.class);
        assertThat(((OptimumRange.UpperBounded) result.range()).max()).isEqualByComparingTo("19");
    }

    private static ReportedOptimum sample(ReportedOptimumId id) {
        return new ReportedOptimum(
                id,
                NutrientName.of("sulfate"),
                LabAnalysisId.create(),
                new OptimumRange.Closed(new BigDecimal("18"), new BigDecimal("110")),
                MeasurementUnit.LBS_PER_1000_SQFT);
    }
}
