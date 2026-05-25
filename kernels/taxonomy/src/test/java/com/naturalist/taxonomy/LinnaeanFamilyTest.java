package com.naturalist.taxonomy;

import com.naturalist.ddd.EntityName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LinnaeanFamilyTest {

    @Test
    void familySlugDerivesFromFamilyEpithet() {
        LinnaeanFamily<TestOrderName> tachinids = family("diptera", "Tachinidae");
        assertThat(tachinids.familySlug()).isEqualTo("tachinidae");
    }

    @Test
    void familySlugLowerCasesAndKebabsTheEpithet() {
        LinnaeanFamily<TestOrderName> syrphids = family("diptera", "Syrphidae");
        assertThat(syrphids.familySlug()).isEqualTo("syrphidae");
    }

    @Test
    void familySlugCollapsesInternalWhitespaceToHyphens() {
        LinnaeanFamily<TestOrderName> oddity = family("diptera", "Family name");
        assertThat(oddity.familySlug()).isEqualTo("family-name");
    }

    @Test
    void familySlugRejectsNullFamily() {
        LinnaeanFamily<TestOrderName> broken = new TestFamily(new TestOrderName("diptera"), null);
        assertThatThrownBy(broken::familySlug).isInstanceOf(NullPointerException.class);
    }

    @Test
    void orderNameIsCarriedAsTheUpwardTypedReference() {
        LinnaeanFamily<TestOrderName> tachinids = family("diptera", "Tachinidae");
        assertThat(tachinids.orderName()).isEqualTo(new TestOrderName("diptera"));
    }

    private static LinnaeanFamily<TestOrderName> family(String orderSlug, String familyEpithet) {
        return new TestFamily(
                new TestOrderName(orderSlug),
                familyEpithet == null ? null : new TaxonomicFamily(familyEpithet));
    }

    private record TestFamily(
            TestOrderName orderName,
            TaxonomicFamily family
    ) implements LinnaeanFamily<TestOrderName> {
    }

    private static final class TestOrderName extends EntityName {
        TestOrderName(String value) {
            super(value);
        }

        @Override
        protected int maxLength() {
            return 64;
        }
    }
}
