package com.naturalist.authority.eol;

import com.naturalist.authority.CitationName;
import com.naturalist.authority.OnlineSource;
import com.naturalist.observability.InvariantObservation;
import com.naturalist.observability.Observer;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class EolCitationTest {

    private static final Observer observer = Observer.forClass(EolCitationTest.class);

    @Test
    void citationFactoryProducesValidOnlineSource() {
        OnlineSource citation = Eol.citation(
                CitationName.of("eol-battus-philenor-1188585"),
                new EolPageId("1188585"),
                "Battus philenor — Encyclopedia of Life",
                "EOL Curators",
                2024,
                Instant.parse("2024-03-15T00:00:00Z"));

        assertThat(citation.name().value()).isEqualTo("eol-battus-philenor-1188585");
        assertThat(citation.authorityReference().source()).isEqualTo(Eol.SOURCE);
        assertThat(citation.authorityReference().url().toString())
                .isEqualTo("https://eol.org/pages/1188585");
        assertThat(citation.title()).isEqualTo("Battus philenor — Encyclopedia of Life");
        assertThat(citation.author()).isEqualTo("EOL Curators");
        assertThat(citation.year()).isEqualTo(2024);

        InvariantObservation result = observer.forMethod("citationFactoryProducesValidOnlineSource")
                .namedEntity(citation, "citation");
        assertThat(result.violations()).isEmpty();
    }

    @Test
    void citationFactoryWithNullOptionalsProducesValidOnlineSource() {
        OnlineSource citation = Eol.citation(
                CitationName.of("eol-apis-mellifera-1045608"),
                new EolPageId("1045608"),
                "Apis mellifera — Encyclopedia of Life",
                null, null, null);

        assertThat(citation.author()).isNull();
        assertThat(citation.year()).isNull();
        assertThat(citation.lastModified()).isNull();

        InvariantObservation result = observer.forMethod("citationFactoryWithNullOptionalsProducesValidOnlineSource")
                .namedEntity(citation, "citation");
        assertThat(result.violations()).isEmpty();
    }
}
