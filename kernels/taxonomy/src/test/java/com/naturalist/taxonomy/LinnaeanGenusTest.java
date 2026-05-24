package com.naturalist.taxonomy;

import com.naturalist.ddd.EntityName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LinnaeanGenusTest {

    @Test
    void genusSlugDerivesFromGenusEpithet() {
        LinnaeanGenus<TestFamilyName> halictus = genus("halictidae", "Halictus");
        assertThat(halictus.genusSlug()).isEqualTo("halictus");
    }

    @Test
    void genusSlugLowerCasesAndKebabsTheEpithet() {
        LinnaeanGenus<TestFamilyName> aristolochia = genus("aristolochiaceae", "Aristolochia");
        assertThat(aristolochia.genusSlug()).isEqualTo("aristolochia");
    }

    @Test
    void genusSlugCollapsesInternalWhitespaceToHyphens() {
        LinnaeanGenus<TestFamilyName> oddity = genus("family-name", "Genus name");
        assertThat(oddity.genusSlug()).isEqualTo("genus-name");
    }

    @Test
    void genusSlugRejectsNullGenus() {
        LinnaeanGenus<TestFamilyName> broken = new TestGenus(
                new TestFamilyName("halictidae"),
                null);
        assertThatThrownBy(broken::genusSlug).isInstanceOf(NullPointerException.class);
    }

    @Test
    void familyNameIsCarriedAsTheUpwardTypedReference() {
        LinnaeanGenus<TestFamilyName> halictus = genus("halictidae", "Halictus");
        assertThat(halictus.familyName()).isEqualTo(new TestFamilyName("halictidae"));
    }

    private static LinnaeanGenus<TestFamilyName> genus(String familySlug, String genusEpithet) {
        return new TestGenus(
                new TestFamilyName(familySlug),
                genusEpithet == null ? null : new TaxonomicGenus(genusEpithet));
    }

    private record TestGenus(
            TestFamilyName familyName,
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
