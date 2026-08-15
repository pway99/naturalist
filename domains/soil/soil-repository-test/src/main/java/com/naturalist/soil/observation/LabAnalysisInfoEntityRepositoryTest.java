package com.naturalist.soil.observation;

import com.naturalist.RandomValue;
import com.naturalist.data.EntityRepositoryTest;
import com.naturalist.data.TestEntitySource;
import com.naturalist.exception.InvariantViolationException;
import com.naturalist.soil.CropName;
import com.naturalist.soil.SoilProfileName;
import com.naturalist.soil.TestSoilIdentifiers;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Behavioral contract for {@link LabAnalysisInfoRepository} (the analysis header), plus the
 * {@code getBySoilProfileName} reverse lookup.
 */
interface LabAnalysisInfoEntityRepositoryTest extends EntityRepositoryTest<LabAnalysisId, LabAnalysisInfo> {

    @Override
    LabAnalysisInfoRepository repository();

    @Override
    default TestEntitySource<LabAnalysisId, LabAnalysisInfo> source() {
        return db.getNamed(LabAnalysisInfoTestEntitySource.class);
    }

    @Override
    default LabAnalysisId notFoundName() {
        return TestSoilIdentifiers.SoilProfiles.NotFound.labAnalysis;
    }

    @Override
    default List<LabAnalysisId> knownEntityNames() {
        return List.of(
                TestSoilIdentifiers.SoilProfiles.Box1.LabAnalyses.labAnalysis,
                TestSoilIdentifiers.SoilProfiles.Backyard.LabAnalyses.labAnalysis);
    }

    @Override
    default LabAnalysisInfo newEntity() {
        return new LabAnalysisInfo(
                LabAnalysisId.create(),
                SoilProfileName.of(RandomValue.string()),
                CropName.of("tomato"),
                LocalDate.of(2026, 3, 3),
                RandomValue.string(),
                RandomValue.string(),
                null);
    }

    @Override
    default LabAnalysisInfo ghostEntity() {
        return new LabAnalysisInfo(
                LabAnalysisId.create(),
                SoilProfileName.of(RandomValue.string()),
                CropName.of("tomato"),
                LocalDate.of(2026, 3, 3),
                RandomValue.string(),
                RandomValue.string(),
                null);
    }

    @Override
    default LabAnalysisInfo modifiedEntity(LabAnalysisInfo original) {
        return new LabAnalysisInfo(
                original.id(),
                SoilProfileName.of(RandomValue.string()),
                CropName.of("lettuce"),
                LocalDate.of(2025, 1, 1),
                RandomValue.string(),
                RandomValue.string(),
                RandomValue.string());
    }

    @Test
    default void getBySoilProfileName_nullArgument() {
        assertThatThrownBy(() -> repository().getBySoilProfileName(null))
                .isInstanceOf(InvariantViolationException.class)
                .hasMessageContainingAll("soilProfileName");
    }

    @Test
    default void getBySoilProfileName_unknownProfile_returnsEmpty() {
        assertThat(repository().getBySoilProfileName(TestSoilIdentifiers.SoilProfiles.NotFound.soilProfile))
                .isEmpty();
    }

    @Test
    default void getBySoilProfileName_knownProfile_returnsMatchingAnalyses() {
        List<LabAnalysisInfo> result =
                repository().getBySoilProfileName(TestSoilIdentifiers.SoilProfiles.Box1.name);

        assertThat(result)
                .extracting(LabAnalysisInfo::id)
                .containsExactly(TestSoilIdentifiers.SoilProfiles.Box1.LabAnalyses.labAnalysis);
    }
}
