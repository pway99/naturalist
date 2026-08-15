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
 * Behavioral contract for {@link ReportedRecommendationRepository}, plus the
 * {@code getByLabAnalysisId} (one report's advice) and {@code getByInputName} (one input over
 * time) reverse lookups.
 */
interface ReportedRecommendationEntityRepositoryTest
        extends EntityRepositoryTest<ReportedRecommendationId, ReportedRecommendation> {

    @Override
    ReportedRecommendationRepository repository();

    @Override
    default TestEntitySource<ReportedRecommendationId, ReportedRecommendation> source() {
        return db.getNamed(ReportedRecommendationTestEntitySource.class);
    }

    @Override
    default ReportedRecommendationId notFoundName() {
        return TestSoilIdentifiers.SoilProfiles.NotFound.reportedRecommendation;
    }

    @Override
    default List<ReportedRecommendationId> knownEntityNames() {
        return List.of(
                TestSoilIdentifiers.SoilProfiles.Box1.ReportedRecommendations.potassiumK2O,
                TestSoilIdentifiers.SoilProfiles.Box1.ReportedRecommendations.gypsumRequirement);
    }

    @Override
    default ReportedRecommendation newEntity() {
        return sample(ReportedRecommendationId.create());
    }

    @Override
    default ReportedRecommendation ghostEntity() {
        return sample(ReportedRecommendationId.create());
    }

    /**
     * Every mutable field changed, including the amount's <em>kind</em> and the route from set to
     * null. A quantity replaced by {@code None} is the update most likely to break an adapter that
     * treats "no value" as "no row".
     */
    @Override
    default ReportedRecommendation modifiedEntity(ReportedRecommendation original) {
        return new ReportedRecommendation(
                original.id(),
                RecommendedInputs.LIME_REQUIREMENT,
                LabAnalysisId.create(),
                new RecommendedAmount.None(),
                MeasurementUnit.TONS_PER_ACRE_FOOT,
                null);
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
    default void getByLabAnalysisId_known_returnsEveryRowOfBothBlocks() {
        List<ReportedRecommendation> result =
                repository().getByLabAnalysisId(TestSoilIdentifiers.SoilProfiles.Box1.LabAnalyses.labAnalysis);

        assertThat(result).hasSize(14);
        assertThat(result).filteredOn(r -> r.unit() == MeasurementUnit.TONS_PER_ACRE_FOOT).hasSize(2);
    }

    @Test
    default void getByInputName_nullArgument() {
        assertThatThrownBy(() -> repository().getByInputName(null))
                .isInstanceOf(InvariantViolationException.class)
                .hasMessageContainingAll("inputName");
    }

    @Test
    default void getByInputName_known_returnsOnePerAnalysisOverTime() {
        List<ReportedRecommendation> result = repository().getByInputName(RecommendedInputs.SULFUR);

        assertThat(result).hasSize(2);
        assertThat(result).allSatisfy(r ->
                assertThat(r.inputName()).isEqualTo(RecommendedInputs.SULFUR));
    }

    @Test
    default void getByInputName_unknown_returnsEmpty() {
        assertThat(repository().getByInputName(RecommendedInputName.of("unobtainium"))).isEmpty();
    }

    /**
     * {@code None} survives the round trip as {@code None} — not as null, not as zero. A lab that
     * said "apply no phosphorus" must still be saying it after persistence.
     */
    @Test
    default void getByName_noneAmount_keepsItsShape() {
        ReportedRecommendation result = repository()
                .getByName(TestSoilIdentifiers.SoilProfiles.Box1.ReportedRecommendations.phosphorus)
                .orElseThrow();

        assertThat(result.amount()).isInstanceOf(RecommendedAmount.None.class);
        assertThat(result.recommendsApplication()).isFalse();
    }

    /** The censored requirement round-trips as a bound, not as a measured 0.50. */
    @Test
    default void getByName_belowDetectionLimitAmount_keepsItsShape() {
        ReportedRecommendation result = repository()
                .getByName(TestSoilIdentifiers.SoilProfiles.Box1.ReportedRecommendations.gypsumRequirement)
                .orElseThrow();

        assertThat(result.amount()).isInstanceOf(RecommendedAmount.BelowDetectionLimit.class);
        assertThat(((RecommendedAmount.BelowDetectionLimit) result.amount()).limit())
                .isEqualByComparingTo("0.50");
        assertThat(result.route()).isNull();
    }

    /**
     * Box 1's lime requirement is a measured zero while its lime fertilisation row is
     * {@code None}. Same report, same input, two different statements — and the model keeps them
     * apart.
     */
    @Test
    default void zeroRequirementIsNotTheSameAsNoRecommendation() {
        ReportedRecommendation requirement = repository()
                .getByName(TestSoilIdentifiers.SoilProfiles.Box1.ReportedRecommendations.limeRequirement)
                .orElseThrow();
        ReportedRecommendation fertilisation = repository()
                .getByName(TestSoilIdentifiers.SoilProfiles.Box1.ReportedRecommendations.lime)
                .orElseThrow();

        assertThat(requirement.amount()).isInstanceOf(RecommendedAmount.Quantity.class);
        assertThat(((RecommendedAmount.Quantity) requirement.amount()).value()).isEqualByComparingTo("0");
        assertThat(fertilisation.amount()).isInstanceOf(RecommendedAmount.None.class);
    }

    private static ReportedRecommendation sample(ReportedRecommendationId id) {
        return new ReportedRecommendation(
                id,
                RecommendedInputs.SULFUR,
                LabAnalysisId.create(),
                new RecommendedAmount.Quantity(new BigDecimal("6.8")),
                MeasurementUnit.LBS_PER_1000_SQFT,
                ReportedRecommendation.ApplicationRoute.SOIL);
    }
}
