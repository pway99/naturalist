package com.naturalist.soil.observation;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class NutrientsTest {

    @Test
    void everyCataloguedNutrientDeclaresItsChemistry() {
        // Drift guard: a nutrient added to ALL without a chemistry entry silently
        // renders as an unlinked row, which no other test would notice.
        for (NutrientName name : Nutrients.ALL) {
            assertThat(Nutrients.chemistryOf(name))
                    .as("nutrient '%s' must declare the substance it measures", name.value())
                    .isPresent();
        }
    }

    @Test
    void bothCalciumFractionsReferenceTheSameElement() {
        // The exchangeable/soluble split is how the lab extracts, not two substances.
        assertThat(Nutrients.chemistryOf(Nutrients.CALCIUM_EXCHANGEABLE).orElseThrow().substance())
                .isEqualTo(Nutrients.chemistryOf(Nutrients.CALCIUM_SOLUBLE).orElseThrow().substance());
    }

    @Test
    void oxideEquivalentsAreMarkedAsSuch() {
        assertThat(Nutrients.chemistryOf(Nutrients.PHOSPHORUS_P2O5).orElseThrow())
                .satisfies(c -> {
                    assertThat(c.substance().value()).isEqualTo("phosphorus");
                    assertThat(c.reportedForm()).isEqualTo(ReportedForm.OXIDE_EQUIVALENT);
                });
        assertThat(Nutrients.chemistryOf(Nutrients.POTASSIUM_EXCHANGEABLE).orElseThrow().reportedForm())
                .isEqualTo(ReportedForm.OXIDE_EQUIVALENT);
    }

    @Test
    void ionsAreMarkedAsIons() {
        assertThat(Nutrients.chemistryOf(Nutrients.SULFATE).orElseThrow())
                .satisfies(c -> {
                    assertThat(c.substance().value()).isEqualTo("sulfur");
                    assertThat(c.reportedForm()).isEqualTo(ReportedForm.ION);
                });
        assertThat(Nutrients.chemistryOf(Nutrients.CHLORIDE).orElseThrow().substance().value())
                .isEqualTo("chlorine");
        assertThat(Nutrients.chemistryOf(Nutrients.NITRATE_N).orElseThrow().substance().value())
                .isEqualTo("nitrogen");
    }

    @Test
    void micronutrientsReferenceTheirOwnElement() {
        assertThat(Nutrients.chemistryOf(Nutrients.BORON).orElseThrow().substance().value())
                .isEqualTo("boron");
        assertThat(Nutrients.chemistryOf(Nutrients.ZINC).orElseThrow().reportedForm())
                .isEqualTo(ReportedForm.ELEMENTAL);
    }

    @Test
    void chemistryOfCarriesTheNutrientItDescribes() {
        assertThat(Nutrients.chemistryOf(Nutrients.IRON).orElseThrow().nutrient())
                .isEqualTo(Nutrients.IRON);
    }

    @Test
    void anUncataloguedNutrientHasNoDeclaredChemistry() {
        assertThat(Nutrients.chemistryOf(NutrientName.of("molybdenum-dtpa"))).isEmpty();
    }
}
