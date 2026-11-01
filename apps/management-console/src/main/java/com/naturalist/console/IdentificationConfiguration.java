package com.naturalist.console;

import com.naturalist.featuresearch.FeatureCorpus;
import com.naturalist.featuresearch.FeatureSearch;
import com.naturalist.featuresearch.InMemoryFeatureSearch;
import com.naturalist.insects.InsectFeatureId;
import com.naturalist.insects.InsectQuery;
import com.naturalist.textgeneration.NoOpTextGenerationService;
import com.naturalist.textgeneration.TextGenerationService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class IdentificationConfiguration {

    // Not-yet-real dev/test adapter built at the composition root; production wires a
    // Solr-backed FeatureSearch bean here instead.
    @Bean
    FeatureSearch<InsectFeatureId> insectFeatureSearch(InsectQuery insectQuery) {
        return new InMemoryFeatureSearch<>(() ->
                insectQuery.features().corpus()
                        .map(f -> new FeatureCorpus.Indexed<>(f.id(), f.value())));
    }

    // The Anthropic text adapter is not on the app classpath; parity with the controller today.
    @Bean
    TextGenerationService textGenerationService() {
        return new NoOpTextGenerationService();
    }
}
