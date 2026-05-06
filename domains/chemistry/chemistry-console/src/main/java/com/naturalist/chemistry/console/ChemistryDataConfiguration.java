package com.naturalist.chemistry.console;

import com.naturalist.chemistry.compound.CompoundTestEntitySource;
import com.naturalist.data.NaturalistDatabase;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ChemistryDataConfiguration {

    @Bean
    CompoundTestEntitySource compoundSource(NaturalistDatabase database) {
        return new CompoundTestEntitySource(database);
    }
}
