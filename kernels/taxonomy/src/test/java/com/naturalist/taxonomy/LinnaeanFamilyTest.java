package com.naturalist.taxonomy;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LinnaeanFamilyTest {

    @Test
    void familySlugDerivesFromFamilyEpithet() {
        LinnaeanFamily tachinids = family("Tachinidae");
        assertThat(tachinids.familySlug()).isEqualTo("tachinidae");
    }

    @Test
    void familySlugLowerCasesAndKebabsTheEpithet() {
        LinnaeanFamily syrphids = family("Syrphidae");
        assertThat(syrphids.familySlug()).isEqualTo("syrphidae");
    }

    @Test
    void familySlugCollapsesInternalWhitespaceToHyphens() {
        // No real-world family name contains whitespace, but the helper is robust
        // to data oddities — confirm the rule.
        LinnaeanFamily oddity = family("Family name");
        assertThat(oddity.familySlug()).isEqualTo("family-name");
    }

    @Test
    void familySlugRejectsNullFamily() {
        LinnaeanFamily broken = new TestFamily(null);
        assertThatThrownBy(broken::familySlug).isInstanceOf(NullPointerException.class);
    }

    private static LinnaeanFamily family(String familyEpithet) {
        return new TestFamily(new TaxonomicFamily(familyEpithet));
    }

    private record TestFamily(TaxonomicFamily family) implements LinnaeanFamily {
    }
}
