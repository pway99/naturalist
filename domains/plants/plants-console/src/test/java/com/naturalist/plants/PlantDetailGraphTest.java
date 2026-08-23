package com.naturalist.plants;

import com.naturalist.data.NaturalistTestExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;

/** The species-rank Plant read model composes its cross-sub-context extras. */
class PlantDetailGraphTest {

    @RegisterExtension
    final NaturalistTestExtension nte = NaturalistTestExtension.create();

    private final PlantsTestContext context = PlantsTestContext.create(nte);

    @Test
    void speciesPlant_composesProgramsAndConstituents() {
        var plant = context.plantQuery().getByName(
                PlantSpeciesName.of("aristolochia-californica")).orElseThrow();
        assertThat(plant.programs().isEmpty()).isFalse();      // pipevine has seeded programs
        assertThat(plant.constituents().isEmpty()).isFalse();  // and seeded constituents
    }

    @Test
    void speciesPlant_composesCultivars_whenSeeded() {
        // solanum-lycopersicum (tomato) is the seeded cultivar-bearing species —
        // confirmed against cultivar/cultivars.json (amish-paste, italian-pear-nicks,
        // sungold-cherry, san-marzano-f2 all carry plantName "solanum-lycopersicum").
        var plant = context.plantQuery().getByName(
                PlantSpeciesName.of("solanum-lycopersicum")).orElseThrow();
        assertThat(plant.cultivars().isEmpty()).isFalse();
    }
}
