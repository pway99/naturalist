package com.naturalist.insects.console;

import com.naturalist.insects.InsectImageTestEntitySource;
import com.naturalist.insects.InsectSpeciesTestEntitySource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class InsectDataConfiguration {

    @Bean
    InsectSpeciesTestEntitySource insectSpeciesSource() {
        return new InsectSpeciesTestEntitySource();
    }

    @Bean
    InsectImageTestEntitySource insectImageSource() {
        return new InsectImageTestEntitySource();
    }
}
