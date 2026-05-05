package com.naturalist.taxonomy;

import com.naturalist.ddd.EntityName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LinnaeanSpeciesTest {

    @Test
    void binomialSlugDerivesFromGenusAndSpeciesEpithets() {
        LinnaeanSpecies<TestGenusName> pipevine = species("aristolochia", "Aristolochia", "californica");
        assertThat(pipevine.binomialSlug()).isEqualTo("aristolochia-californica");
    }

    @Test
    void binomialSlugLowerCasesGenusKeepingTheKebabConvention() {
        LinnaeanSpecies<TestGenusName> swallowtail = species("battus", "Battus", "philenor");
        assertThat(swallowtail.binomialSlug()).isEqualTo("battus-philenor");
    }

    @Test
    void binomialSlugCollapsesInternalWhitespaceToHyphens() {
        // No real-world Linnaean epithet contains whitespace, but the helper
        // is robust to data oddities — confirm the rule.
        LinnaeanSpecies<TestGenusName> oddity = species("genus-name", "Genus name", "species_epithet");
        assertThat(oddity.binomialSlug()).isEqualTo("genus-name-species-epithet");
    }

    @Test
    void binomialSlugRejectsNullGenus() {
        LinnaeanSpecies<TestGenusName> broken = new TestSpecies(
                new TestGenusName("aristolochia"),
                null,
                new TaxonomicSpecies("californica"));
        assertThatThrownBy(broken::binomialSlug).isInstanceOf(NullPointerException.class);
    }

    @Test
    void binomialSlugRejectsNullSpecies() {
        LinnaeanSpecies<TestGenusName> broken = new TestSpecies(
                new TestGenusName("aristolochia"),
                new TaxonomicGenus("Aristolochia"),
                null);
        assertThatThrownBy(broken::binomialSlug).isInstanceOf(NullPointerException.class);
    }

    @Test
    void genusNameIsCarriedAsTheUpwardTypedReference() {
        LinnaeanSpecies<TestGenusName> pipevine = species("aristolochia", "Aristolochia", "californica");
        assertThat(pipevine.genusName()).isEqualTo(new TestGenusName("aristolochia"));
    }

    @Test
    void genusNameSlugMatchesGenusEpithetKebab() {
        // The cross-rank invariant the catalog relies on: a species' genusName slug
        // equals lowerKebab(genus epithet). This is a record-time check, independent
        // of whether the genus aggregate exists.
        LinnaeanSpecies<TestGenusName> pipevine = species("aristolochia", "Aristolochia", "californica");
        assertThat(pipevine.genusName().value()).isEqualTo(TaxonomicSlugs.genusSlug(pipevine.genus()));
    }

    private static LinnaeanSpecies<TestGenusName> species(String genusSlug, String genusEpithet, String speciesEpithet) {
        return new TestSpecies(
                new TestGenusName(genusSlug),
                new TaxonomicGenus(genusEpithet),
                speciesEpithet == null ? null : new TaxonomicSpecies(speciesEpithet));
    }

    private record TestSpecies(
            TestGenusName genusName,
            TaxonomicGenus genus,
            TaxonomicSpecies species
    ) implements LinnaeanSpecies<TestGenusName> {
    }

    private static final class TestGenusName extends EntityName {
        TestGenusName(String value) {
            super(value);
        }

        @Override
        protected int maxLength() {
            return 64;
        }
    }
}
