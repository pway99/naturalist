package com.naturalist.plants.console;

import com.naturalist.data.NaturalistDatabase;
import com.naturalist.plants.PlantSpeciesTestEntitySource;
import com.naturalist.plants.cultivar.PlantCultivarTestEntitySource;
import com.naturalist.plants.heritage.PlantSeedLineageTestEntitySource;
import com.naturalist.plants.management.PlantProgramTestEntitySource;
import com.naturalist.plants.phytochemistry.PlantPhytochemicalConstituentTestEntitySource;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PlantsDataConfiguration {

    @Bean
    PlantSpeciesTestEntitySource plantSource(NaturalistDatabase database) {
        return new PlantSpeciesTestEntitySource(database);
    }

    @Bean
    PlantCultivarTestEntitySource cultivarSource(NaturalistDatabase database) {
        return new PlantCultivarTestEntitySource(database);
    }

    @Bean
    PlantSeedLineageTestEntitySource seedLineageSource(NaturalistDatabase database) {
        return new PlantSeedLineageTestEntitySource(database);
    }

    @Bean
    PlantProgramTestEntitySource plantProgramSource(NaturalistDatabase database) {
        return new PlantProgramTestEntitySource(database);
    }

    @Bean
    PlantPhytochemicalConstituentTestEntitySource phytochemicalConstituentSource(NaturalistDatabase database) {
        return new PlantPhytochemicalConstituentTestEntitySource(database);
    }
}
