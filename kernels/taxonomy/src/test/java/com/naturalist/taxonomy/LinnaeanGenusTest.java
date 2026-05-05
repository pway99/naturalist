package com.naturalist.taxonomy;

import com.naturalist.ddd.EntityName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LinnaeanGenusTest {

    @Test
    void genusSlugDerivesFromGenusEpithet() {
        LinnaeanGenus<TestFamilyName> halictus = genus("halictidae", "Halictidae", "Halictus");
        assertThat(halictus.genusSlug()).isEqualTo("halictus");
    }

    @Test
    void genusSlugLowerCasesAndKebabsTheEpithet() {
        LinnaeanGenus<TestFamilyName> aristolochia = genus("aristolochiaceae", "Aristolochiaceae", "Aristolochia");
        assertThat(aristolochia.genusSlug()).isEqualTo("aristolochia");
    }

    @Test
    void genusSlugCollapsesInternalWhitespaceToHyphens() {
        LinnaeanGenus<TestFamilyName> oddity = genus("family-name", "Family name", "Genus name");
        assertThat(oddity.genusSlug()).isEqualTo("genus-name");
    }

    @Test
    void genusSlugRejectsNullGenus() {
        LinnaeanGenus<TestFamilyName> broken = new TestGenus(
                new TestFamilyName("halictidae"),
                new TaxonomicFamily("Halictidae"),
                null);
        assertThatThrownBy(broken::genusSlug).isInstanceOf(NullPointerException.class);
    }

    @Test
    void familyNameIsCarriedAsTheUpwardTypedReference() {
        LinnaeanGenus<TestFamilyName> halictus = genus("halictidae", "Halictidae", "Halictus");
        assertThat(halictus.familyName()).isEqualTo(new TestFamilyName("halictidae"));
    }

    @Test
    void familyEpithetIsExposedAlongsideFamilyName() {
        // The redundant epithet supports the cross-rank consistency check at
        // catalog-assembly time without forcing the genus to resolve its parent.
        LinnaeanGenus<TestFamilyName> halictus = genus("halictidae", "Halictidae", "Halictus");
        assertThat(halictus.family()).isEqualTo(new TaxonomicFamily("Halictidae"));
    }

    @Test
    void familyNameSlugMatchesFamilyEpithetKebab() {
        // The cross-rank invariant the catalog relies on: a genus' familyName slug
        // equals lowerKebab(family epithet). Record-time check, independent of
        // whether the family aggregate exists.
        LinnaeanGenus<TestFamilyName> halictus = genus("halictidae", "Halictidae", "Halictus");
        assertThat(halictus.familyName().value()).isEqualTo(TaxonomicSlugs.familySlug(halictus.family()));
    }

    private static LinnaeanGenus<TestFamilyName> genus(String familySlug, String familyEpithet, String genusEpithet) {
        return new TestGenus(
                new TestFamilyName(familySlug),
                new TaxonomicFamily(familyEpithet),
                genusEpithet == null ? null : new TaxonomicGenus(genusEpithet));
    }

    private record TestGenus(
            TestFamilyName familyName,
            TaxonomicFamily family,
            TaxonomicGenus genus
    ) implements LinnaeanGenus<TestFamilyName> {
    }

    private static final class TestFamilyName extends EntityName {
        TestFamilyName(String value) {
            super(value);
        }

        @Override
        protected int maxLength() {
            return 64;
        }
    }
}
