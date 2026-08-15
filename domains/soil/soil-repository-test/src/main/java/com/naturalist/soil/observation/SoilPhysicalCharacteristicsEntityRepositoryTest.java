package com.naturalist.soil.observation;

import com.naturalist.data.EntityRepositoryTest;
import com.naturalist.data.TestEntitySource;
import com.naturalist.exception.InvariantViolationException;
import com.naturalist.measurements.ElectricalConductivity;
import com.naturalist.soil.SoilPH;
import com.naturalist.soil.TestSoilIdentifiers;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Behavioral contract for {@link SoilPhysicalCharacteristicsRepository}, plus the
 * {@code getByLabAnalysisId} 1:1 lookup.
 */
interface SoilPhysicalCharacteristicsEntityRepositoryTest
        extends EntityRepositoryTest<SoilPhysicalCharacteristicsId, SoilPhysicalCharacteristics> {

    @Override
    SoilPhysicalCharacteristicsRepository repository();

    @Override
    default TestEntitySource<SoilPhysicalCharacteristicsId, SoilPhysicalCharacteristics> source() {
        return db.getNamed(SoilPhysicalCharacteristicsTestEntitySource.class);
    }

    @Override
    default SoilPhysicalCharacteristicsId notFoundName() {
        return TestSoilIdentifiers.SoilProfiles.NotFound.physicalCharacteristics;
    }

    @Override
    default List<SoilPhysicalCharacteristicsId> knownEntityNames() {
        return List.of(
                TestSoilIdentifiers.SoilProfiles.Box1.PhysicalCharacteristics.characteristics,
                TestSoilIdentifiers.SoilProfiles.Backyard.PhysicalCharacteristics.characteristics);
    }

    /** One row per analysis and two analyses, so page at one to keep the boundary covered. */
    @Override
    default int pageSize() {
        return 1;
    }

    @Override
    default SoilPhysicalCharacteristics newEntity() {
        return sample(SoilPhysicalCharacteristicsId.create(), LabAnalysisId.create());
    }

    @Override
    default SoilPhysicalCharacteristics ghostEntity() {
        return sample(SoilPhysicalCharacteristicsId.create(), LabAnalysisId.create());
    }

    @Override
    default SoilPhysicalCharacteristics modifiedEntity(SoilPhysicalCharacteristics original) {
        return new SoilPhysicalCharacteristics(
                original.id(),
                LabAnalysisId.create(),
                CecMeqPer100g.of(new BigDecimal("30.0")),
                SoilPH.of(new BigDecimal("6.8")),
                ElectricalConductivity.of(new BigDecimal("0.7")),
                SodiumAdsorptionRatio.of(new BigDecimal("0.6")),
                LimestonePct.of(new BigDecimal("0.4")),
                SaturationPct.of(new BigDecimal("48")),
                new CationBaseSaturation(
                        BigDecimal.valueOf(72), BigDecimal.valueOf(21),
                        BigDecimal.valueOf(4), BigDecimal.valueOf(2), BigDecimal.ONE,
                        true));
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
    default void getByLabAnalysisId_known_returnsCharacteristics() {
        Optional<SoilPhysicalCharacteristics> result =
                repository().getByLabAnalysisId(TestSoilIdentifiers.SoilProfiles.Box1.LabAnalyses.labAnalysis);

        assertThat(result).isPresent();
        assertThat(result.get().id())
                .isEqualTo(TestSoilIdentifiers.SoilProfiles.Box1.PhysicalCharacteristics.characteristics);
    }

    private static SoilPhysicalCharacteristics sample(SoilPhysicalCharacteristicsId id, LabAnalysisId labAnalysisId) {
        return new SoilPhysicalCharacteristics(
                id,
                labAnalysisId,
                CecMeqPer100g.of(new BigDecimal("40.0")),
                SoilPH.of(new BigDecimal("7.0")),
                ElectricalConductivity.of(new BigDecimal("0.5")),
                SodiumAdsorptionRatio.of(new BigDecimal("0.3")),
                LimestonePct.of(new BigDecimal("1.0")),
                SaturationPct.of(new BigDecimal("50")),
                new CationBaseSaturation(
                        BigDecimal.valueOf(74), BigDecimal.valueOf(20),
                        BigDecimal.valueOf(3), BigDecimal.valueOf(2), BigDecimal.ONE,
                        false));
    }
}
