package com.naturalist.plants.console;

import com.naturalist.data.NaturalistDatabase;
import com.naturalist.plants.PlantSpeciesTestEntitySource;
import com.naturalist.plants.cultivar.CultivarTestEntitySource;
import com.naturalist.plants.heritage.SeedLineageTestEntitySource;
import com.naturalist.plants.management.PlantProgramTestEntitySource;
import com.naturalist.plants.phytochemistry.PhytochemicalConstituentTestEntitySource;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PlantsDataConfiguration {

    @Bean
    PlantSpeciesTestEntitySource plantSource(NaturalistDatabase database) {
        return new PlantSpeciesTestEntitySource(database);
    }

    @Bean
    CultivarTestEntitySource cultivarSource(NaturalistDatabase database) {
        return new CultivarTestEntitySource(database);
    }

    @Bean
    SeedLineageTestEntitySource seedLineageSource(NaturalistDatabase database) {
        return new SeedLineageTestEntitySource(database);
    }

    @Bean
    PlantProgramTestEntitySource plantProgramSource(NaturalistDatabase database) {
        return new PlantProgramTestEntitySource(database);
    }

    @Bean
    PhytochemicalConstituentTestEntitySource phytochemicalConstituentSource(NaturalistDatabase database) {
        return new PhytochemicalConstituentTestEntitySource(database);
    }
}
