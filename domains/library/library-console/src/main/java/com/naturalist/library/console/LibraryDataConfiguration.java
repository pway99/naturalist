package com.naturalist.library.console;

import com.naturalist.data.NaturalistDatabase;
import com.naturalist.library.CitationTestEntitySource;
import com.naturalist.library.ConceptTestEntitySource;
import com.naturalist.library.GlossaryTermTestEntitySource;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class LibraryDataConfiguration {

    @Bean
    ConceptTestEntitySource conceptSource(NaturalistDatabase database) {
        return new ConceptTestEntitySource(database);
    }

    @Bean
    GlossaryTermTestEntitySource glossaryTermSource(NaturalistDatabase database) {
        return new GlossaryTermTestEntitySource(database);
    }

    @Bean
    CitationTestEntitySource citationSource(NaturalistDatabase database) {
        return new CitationTestEntitySource(database);
    }
}
