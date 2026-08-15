package com.naturalist.chemistry.element;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PeriodicElementTest {

    @Test
    void elementNameIsAValidLowerKebabSlug() {
        for (PeriodicElement element : PeriodicElement.values()) {
            assertThat(element.elementName().isValid())
                    .as("%s.elementName() = '%s' must be a valid EntityName",
                            element.name(), element.elementName().value())
                    .isTrue();
        }
    }

    @Test
    void elementNameMatchesTheCatalogSlug() {
        assertThat(PeriodicElement.Ca.elementName().value()).isEqualTo("calcium");
        assertThat(PeriodicElement.Zn.elementName().value()).isEqualTo("zinc");
        assertThat(PeriodicElement.B.elementName().value()).isEqualTo("boron");
    }

    @Test
    void displayNameKeepsThePrintedForm() {
        assertThat(PeriodicElement.Ca.displayName()).isEqualTo("Calcium");
        assertThat(PeriodicElement.Zn.displayName()).isEqualTo("Zinc");
    }

    @Test
    void symbolIsTheEnumConstantName() {
        assertThat(PeriodicElement.Ca.symbol()).isEqualTo("Ca");
        assertThat(PeriodicElement.Fe.symbol()).isEqualTo("Fe");
    }
}
