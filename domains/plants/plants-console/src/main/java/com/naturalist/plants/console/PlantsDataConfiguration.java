package com.naturalist.plants.console;

import com.naturalist.plants.PlantTestEntitySource;
import com.naturalist.plants.cultivar.CultivarTestEntitySource;
import com.naturalist.plants.heritage.SeedLineageTestEntitySource;
import com.naturalist.plants.management.PlantProgramTestEntitySource;
import com.naturalist.plants.phytochemistry.PhytochemicalConstituentTestEntitySource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PlantsDataConfiguration {

    @Bean
    PlantTestEntitySource plantSource() {
        return new PlantTestEntitySource();
    }

    @Bean
    CultivarTestEntitySource cultivarSource() {
        return new CultivarTestEntitySource();
    }

    @Bean
    SeedLineageTestEntitySource seedLineageSource() {
        return new SeedLineageTestEntitySource();
    }

    @Bean
    PlantProgramTestEntitySource plantProgramSource() {
        return new PlantProgramTestEntitySource();
    }

    @Bean
    PhytochemicalConstituentTestEntitySource phytochemicalConstituentSource() {
        return new PhytochemicalConstituentTestEntitySource();
    }
}
