package com.naturalist.plants;

import com.naturalist.data.NaturalistDatabase;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

class PlantFactoryTest {

    private PlantFactory factory() {
        NaturalistDatabase db = NaturalistDatabase.create();
        PlantQuery.GenusQuery genusQuery =
                new PlantGenusQueryImpl(new PlantGenusRepositoryMock(db));
        PlantQuery.SpeciesQuery speciesQuery =
                new PlantSpeciesQueryImpl(new PlantSpeciesRepositoryMock(db), genusQuery);
        PlantQuery.FamilyQuery familyQuery =
                new PlantFamilyQueryImpl(new PlantFamilyRepositoryMock(db));
        PlantQuery.OrderQuery orderQuery =
                new PlantOrderQueryImpl(new PlantOrderRepositoryMock(db));
        return new PlantFactory(speciesQuery, genusQuery, familyQuery, orderQuery);
    }

    @Test
    void buildByName_speciesName_composesFullAncestrySpine() {
        Plant plant = factory().buildByName(
                com.naturalist.plants.PlantSpeciesName.of("aristolochia-californica")).orElseThrow();
        assertThat(plant.speciesName()).contains(PlantSpeciesName.of("aristolochia-californica"));
        assertThat(plant.genusName()).contains(PlantGenusName.of("aristolochia"));
        assertThat(plant.familyName()).contains(PlantFamilyName.of("aristolochiaceae"));
        assertThat(plant.orderName()).contains(PlantOrderName.of("piperales"));
    }

    @Test
    void buildByName_orderName_composesOrderOnly() {
        Plant plant = factory().buildByName(PlantOrderName.of("lamiales")).orElseThrow();
        assertThat(plant.orderName()).contains(PlantOrderName.of("lamiales"));
        assertThat(plant.familyName()).isEmpty();
        assertThat(plant.genusName()).isEmpty();
        assertThat(plant.speciesName()).isEmpty();
    }

    @Test
    void buildByName_unknownName_isEmpty() {
        assertThat(factory().buildByName(PlantOrderName.of("unobtainium-ales"))).isEmpty();
    }

    @Test
    void buildByName_nullName_throws() {
        assertThat(catchThrowable(() -> factory().buildByName(null)))
                .isInstanceOf(com.naturalist.exception.InvariantViolationException.class);
    }
}
