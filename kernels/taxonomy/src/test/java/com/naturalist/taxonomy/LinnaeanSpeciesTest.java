package com.naturalist.taxonomy;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LinnaeanSpeciesTest {

    @Test
    void binomialSlugDerivesFromGenusAndSpeciesEpithets() {
        LinnaeanSpecies pipevine = species("Aristolochia", "californica");
        assertThat(pipevine.binomialSlug()).isEqualTo("aristolochia-californica");
    }

    @Test
    void binomialSlugLowerCasesGenusKeepingTheKebabConvention() {
        LinnaeanSpecies swallowtail = species("Battus", "philenor");
        assertThat(swallowtail.binomialSlug()).isEqualTo("battus-philenor");
    }

    @Test
    void binomialSlugCollapsesInternalWhitespaceToHyphens() {
        // No real-world Linnaean epithet contains whitespace, but the helper
        // is robust to data oddities — confirm the rule.
        LinnaeanSpecies oddity = species("Genus name", "species_epithet");
        assertThat(oddity.binomialSlug()).isEqualTo("genus-name-species-epithet");
    }

    @Test
    void binomialSlugRejectsNullGenus() {
        LinnaeanSpecies broken = new TestSpecies(null, new TaxonomicSpecies("californica"));
        assertThatThrownBy(broken::binomialSlug).isInstanceOf(NullPointerException.class);
    }

    @Test
    void binomialSlugRejectsNullSpecies() {
        LinnaeanSpecies broken = new TestSpecies(new TaxonomicGenus("Aristolochia"), null);
        assertThatThrownBy(broken::binomialSlug).isInstanceOf(NullPointerException.class);
    }

    private static LinnaeanSpecies species(String genus, String species) {
        return new TestSpecies(new TaxonomicGenus(genus), new TaxonomicSpecies(species));
    }

    private record TestSpecies(TaxonomicGenus genus, TaxonomicSpecies species) implements LinnaeanSpecies {
    }
}
