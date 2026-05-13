package com.naturalist.clades;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CladeTest {

    private static final List<Clade> ALL = List.of(
            new Eukaryota(),
            new Animalia(),
            new Arthropoda(),
            new Insecta(),
            new Holometabola(),
            new Lepidoptera(),
            new Papilionidae());

    @Test
    void everyPermitHasANonBlankSlug() {
        assertThat(ALL)
                .extracting(Clade::slug)
                .allMatch(s -> s != null && !s.isBlank());
    }

    @Test
    void slugsAreUniqueAcrossPermits() {
        Set<String> slugs = ALL.stream().map(Clade::slug).collect(java.util.stream.Collectors.toSet());
        assertThat(slugs).hasSize(ALL.size());
    }

    @Test
    void everyPermitHasANonBlankDisplayName() {
        assertThat(ALL)
                .extracting(Clade::displayName)
                .allMatch(s -> s != null && !s.isBlank());
    }

    @Test
    void everyPermitHasAFourLevelDescription() {
        assertThat(ALL).allSatisfy(c -> {
            assertThat(c.description().preschool()).as(c.slug() + ".preschool").isNotBlank();
            assertThat(c.description().elementary()).as(c.slug() + ".elementary").isNotBlank();
            assertThat(c.description().secondary()).as(c.slug() + ".secondary").isNotBlank();
            assertThat(c.description().university()).as(c.slug() + ".university").isNotBlank();
        });
    }

    @Test
    void onlyEukaryotaHasEmptyParent() {
        assertThat(new Eukaryota().parent()).isEmpty();
        Stream.of(new Animalia(), new Arthropoda(), new Insecta(),
                  new Holometabola(), new Lepidoptera(), new Papilionidae())
                .forEach(c -> assertThat(c.parent()).as(c.slug() + ".parent").isPresent());
    }

    @Test
    void parentChainResolvesFromPapilionidaeToEukaryota() {
        assertThat(new Papilionidae().parent()).contains(new Lepidoptera());
        assertThat(new Lepidoptera().parent()).contains(new Holometabola());
        assertThat(new Holometabola().parent()).contains(new Insecta());
        assertThat(new Insecta().parent()).contains(new Arthropoda());
        assertThat(new Arthropoda().parent()).contains(new Animalia());
        assertThat(new Animalia().parent()).contains(new Eukaryota());
        assertThat(new Eukaryota().parent()).isEmpty();
    }

    @Test
    void ofResolvesEverySlugToItsPermit() {
        for (Clade c : ALL) {
            assertThat(Clade.of(c.slug())).isEqualTo(c);
        }
    }

    @Test
    void ofRejectsUnknownSlugs() {
        assertThatThrownBy(() -> Clade.of("not-a-real-clade"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not-a-real-clade");
    }

    @Test
    void jsonRoundtripPreservesIdentity() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        for (Clade c : ALL) {
            String json = mapper.writeValueAsString(c);
            assertThat(json).as(c.slug() + ".json").isEqualTo("\"" + c.slug() + "\"");
            Clade decoded = mapper.readValue(json, Clade.class);
            assertThat(decoded).isEqualTo(c);
        }
    }

    @Test
    void recordsWithSameSlugAreEqual() {
        assertThat(new Holometabola()).isEqualTo(new Holometabola());
        assertThat(new Holometabola()).isNotEqualTo(new Lepidoptera());
    }
}
