package com.naturalist.plants;

import com.naturalist.data.NaturalistTestExtension;
import com.naturalist.test.context.Wired;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

class PlantFactoryTest {

    @RegisterExtension
    final NaturalistTestExtension nte = NaturalistTestExtension.create();

    /** Provided by {@link PlantsTestContextInternal} over the graph's real queries, sub-context queries included. */
    @Wired PlantFactory factory;

    @Test
    void buildByName_speciesName_composesFullAncestrySpine() {
        Plant plant = factory.buildByName(
                com.naturalist.plants.PlantSpeciesName.of("aristolochia-californica")).orElseThrow();
        assertThat(plant.speciesName()).contains(PlantSpeciesName.of("aristolochia-californica"));
        assertThat(plant.genusName()).contains(PlantGenusName.of("aristolochia"));
        assertThat(plant.familyName()).contains(PlantFamilyName.of("aristolochiaceae"));
        assertThat(plant.orderName()).contains(PlantOrderName.of("piperales"));
    }

    @Test
    void buildByName_orderName_composesOrderOnly() {
        Plant plant = factory.buildByName(PlantOrderName.of("lamiales")).orElseThrow();
        assertThat(plant.orderName()).contains(PlantOrderName.of("lamiales"));
        assertThat(plant.familyName()).isEmpty();
        assertThat(plant.genusName()).isEmpty();
        assertThat(plant.speciesName()).isEmpty();
    }

    @Test
    void buildByName_unknownName_isEmpty() {
        assertThat(factory.buildByName(PlantOrderName.of("unobtainium-ales"))).isEmpty();
    }

    @Test
    void buildByName_nullName_throws() {
        assertThat(catchThrowable(() -> factory.buildByName(null)))
                .isInstanceOf(com.naturalist.exception.InvariantViolationException.class);
    }

    @Test
    void buildByName_composesAncestryFeatures() {
        Plant plant = factory.buildByName(PlantGenusName.of("helianthus")).orElseThrow();
        assertThat(plant.features()).isNotNull();
        assertThat(plant.features().groups().stream().map(g -> g.rank()))
                .containsExactly(PlantOrderName.of("asterales"),
                        PlantFamilyName.of("asteraceae"), PlantGenusName.of("helianthus"));
    }

    @Test
    void buildByName_orderName_composesFamilyChildren() {
        Plant plant = factory.buildByName(PlantOrderName.of("asterales")).orElseThrow();
        assertThat(plant.children()).isNotEmpty();
        assertThat(plant.children()).allSatisfy(c ->
                assertThat(c).isInstanceOf(PlantTaxonView.FamilyView.class));
    }

    @Test
    void buildByName_speciesName_hasNoChildren() {
        Plant plant = factory.buildByName(PlantSpeciesName.of("aristolochia-californica")).orElseThrow();
        assertThat(plant.children()).isEmpty();
    }

    @Test
    void buildByName_composesImages_forSpeciesWithSeededPhotos() {
        // aristolochia-californica has 2 seeded images (plant-images.json).
        Plant plant = factory.buildByName(PlantSpeciesName.of("aristolochia-californica")).orElseThrow();
        assertThat(plant.images().stream().toList()).hasSize(2);
    }

    @Test
    void buildByName_nonSpeciesRank_hasEmptyExtras() {
        Plant plant = factory.buildByName(PlantOrderName.of("asterales")).orElseThrow();
        assertThat(plant.cultivars().isEmpty()).isTrue();
        assertThat(plant.programs().isEmpty()).isTrue();
        assertThat(plant.constituents().isEmpty()).isTrue();
    }

    @Test
    void buildByName_speciesName_composesItsSeededConstituents() {
        // aristolochia-californica carries aristolochic acids I and II (phytochemistry/phytochemical-constituents.json).
        Plant plant = factory.buildByName(PlantSpeciesName.of("aristolochia-californica")).orElseThrow();
        assertThat(plant.constituents().isEmpty()).isFalse();
    }
}
