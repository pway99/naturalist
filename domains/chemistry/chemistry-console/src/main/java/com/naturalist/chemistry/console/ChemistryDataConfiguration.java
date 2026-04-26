package com.naturalist.chemistry.console;

import com.naturalist.chemistry.compound.CompoundTestEntitySource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ChemistryDataConfiguration {

    @Bean
    CompoundTestEntitySource compoundSource() {
        return new CompoundTestEntitySource();
    }
}
