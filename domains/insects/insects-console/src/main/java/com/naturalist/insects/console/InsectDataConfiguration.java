package com.naturalist.insects.console;

import com.naturalist.data.NaturalistDatabase;
import com.naturalist.insects.InsectImageTestEntitySource;
import com.naturalist.insects.InsectSpeciesTestEntitySource;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class InsectDataConfiguration {

    @Bean
    InsectSpeciesTestEntitySource insectSpeciesSource(NaturalistDatabase database) {
        return new InsectSpeciesTestEntitySource(database);
    }

    @Bean
    InsectImageTestEntitySource insectImageSource(NaturalistDatabase database) {
        return new InsectImageTestEntitySource(database);
    }
}
