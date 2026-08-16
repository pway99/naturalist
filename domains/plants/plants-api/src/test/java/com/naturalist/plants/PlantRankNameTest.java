package com.naturalist.plants;

import com.naturalist.taxonomy.LinealRank;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Per-permit verification that {@link PlantRankName#rank()} returns the matching
 * {@link LinealRank}, and that the factory covers exactly the four ranks plants
 * catalogues — order, family, genus, species.
 * <p>
 * {@code rank()} is total here — every permit is a Linnaean rung by construction, so
 * there is no null case to test. That is the property the type buys by keeping
 * {@code CultivarName} off the permits list.
 */
class PlantRankNameTest {

    @Test
    void orderPermitReportsOrderRank() {
        PlantRankName name = PlantOrderName.of("lamiales");
        assertThat(name.rank()).isEqualTo(LinealRank.ORDER);
    }

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
        PlantRankName name = PlantSpeciesName.of("solanum-lycopersicum");
        assertThat(name.rank()).isEqualTo(LinealRank.SPECIES);
    }

    @Test
    void factoryBuildsThePermitMatchingTheRank() {
        assertThat(PlantRankName.of("lamiales", LinealRank.ORDER))
                .isInstanceOf(PlantOrderName.class);
        assertThat(PlantRankName.of("lamiaceae", LinealRank.FAMILY))
                .isInstanceOf(PlantFamilyName.class);
        assertThat(PlantRankName.of("salvia", LinealRank.GENUS))
                .isInstanceOf(PlantGenusName.class);
        assertThat(PlantRankName.of("solanum-lycopersicum", LinealRank.SPECIES))
                .isInstanceOf(PlantSpeciesName.class);
    }

    @Test
    void factoryRejectsRanksPlantsDoesNotCatalogue() {
        // The ladder runs ORDER → SPECIES. Plants has no subspecies name — further
        // specificity below species is a Cultivar, which is a different axis — and
        // nothing above order, so a slug at those ranks has nowhere to live and the
        // factory refuses rather than inventing a home for it.
        assertThatThrownBy(() -> PlantRankName.of("anything", LinealRank.SUBSPECIES))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("SUBSPECIES");
        assertThatThrownBy(() -> PlantRankName.of("plantae", LinealRank.KINGDOM))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("KINGDOM");
    }

    @Test
    void equalityIsClassQualifiedAcrossPermits() {
        // The plants catalog has genuinely carried the same slug at two ranks —
        // "citrus" was both a PlantSpecies row and a PlantGenus record — so this is load
        // bearing, not theoretical.
        PlantRankName asGenus = PlantGenusName.of("citrus");
        PlantRankName asSpecies = PlantSpeciesName.of("citrus");

        assertThat(asGenus).isNotEqualTo(asSpecies);
        assertThat(asGenus.value()).isEqualTo(asSpecies.value());
    }
}
