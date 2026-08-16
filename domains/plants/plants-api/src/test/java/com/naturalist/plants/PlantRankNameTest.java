package com.naturalist.plants;

import com.naturalist.taxonomy.LinealRank;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Per-permit verification that {@link PlantRankName#rank()} returns the matching
 * {@link LinealRank}, and that the factory covers exactly the ranks plants catalogues.
 * <p>
 * {@code rank()} is total here — every permit is a Linnaean rung by construction, so
 * there is no null case to test. That is the property the type buys by keeping
 * {@code CultivarName} off the permits list.
 */
class PlantRankNameTest {

    @Test
    void familyPermitReportsFamilyRank() {
        PlantRankName name = PlantFamilyName.of("lamiaceae");
        assertThat(name.rank()).isEqualTo(LinealRank.FAMILY);
    }

    @Test
    void genusPermitReportsGenusRank() {
        PlantRankName name = PlantGenusName.of("salvia");
        assertThat(name.rank()).isEqualTo(LinealRank.GENUS);
    }

    @Test
    void speciesPermitReportsSpeciesRank() {
        // PlantName is the species-rank permit despite its unqualified name.
        PlantRankName name = PlantName.of("solanum-lycopersicum");
        assertThat(name.rank()).isEqualTo(LinealRank.SPECIES);
    }

    @Test
    void factoryBuildsThePermitMatchingTheRank() {
        assertThat(PlantRankName.of("lamiaceae", LinealRank.FAMILY))
                .isInstanceOf(PlantFamilyName.class);
        assertThat(PlantRankName.of("salvia", LinealRank.GENUS))
                .isInstanceOf(PlantGenusName.class);
        assertThat(PlantRankName.of("solanum-lycopersicum", LinealRank.SPECIES))
                .isInstanceOf(PlantName.class);
    }

    @Test
    void factoryRejectsRanksPlantsDoesNotCatalogue() {
        // Plants has no order-rank entity and no subspecies name — a slug at those
        // ranks has nowhere to live, so the factory refuses rather than inventing one.
        assertThatThrownBy(() -> PlantRankName.of("lamiales", LinealRank.ORDER))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ORDER");
        assertThatThrownBy(() -> PlantRankName.of("anything", LinealRank.SUBSPECIES))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void equalityIsClassQualifiedAcrossPermits() {
        // The plants catalog has genuinely carried the same slug at two ranks —
        // "citrus" was both a Plant row and a PlantGenus record — so this is load
        // bearing, not theoretical.
        PlantRankName asGenus = PlantGenusName.of("citrus");
        PlantRankName asSpecies = PlantName.of("citrus");

        assertThat(asGenus).isNotEqualTo(asSpecies);
        assertThat(asGenus.value()).isEqualTo(asSpecies.value());
    }
}
