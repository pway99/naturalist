package com.naturalist.soil;

import com.naturalist.data.NaturalistDatabaseExtension;
import com.naturalist.exception.InvariantViolationException;
import com.naturalist.soil.observation.LabAnalysis;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

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
        assertThat(analysis.nutrients().secondary().calciumSoluble().value())
                .isEqualByComparingTo("6.99");
        assertThat(analysis.nutrients().micro().boron().value())
                .isEqualByComparingTo("0.0202");
        // ...and the physical characteristics attached.
        assertThat(analysis.physicalCharacteristics().pH().value())
                .isEqualByComparingTo("7.2");
        assertThat(analysis.physicalCharacteristics().cecMeqPer100g().value())
                .isEqualByComparingTo("44.9");
    }
}
