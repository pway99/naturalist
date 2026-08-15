package com.naturalist.soil.observation;

import com.naturalist.chemistry.element.ElementName;
import com.naturalist.exception.InvariantViolationException;
import com.naturalist.observability.Observer;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NutrientChemistryTest {

    @Test
    void carriesNutrientSubstanceAndForm() {
        NutrientChemistry chemistry = NutrientChemistry.of(
                Nutrients.PHOSPHORUS_P2O5, ElementName.of("phosphorus"), ReportedForm.OXIDE_EQUIVALENT);

        assertThat(chemistry.nutrient()).isEqualTo(Nutrients.PHOSPHORUS_P2O5);
        assertThat(chemistry.substance().value()).isEqualTo("phosphorus");
        assertThat(chemistry.reportedForm()).isEqualTo(ReportedForm.OXIDE_EQUIVALENT);
    }

    @Test
    void everyReportedFormHasAReaderFacingLabel() {
        for (ReportedForm form : ReportedForm.values()) {
            assertThat(form.label()).isNotBlank();
        }
        assertThat(ReportedForm.OXIDE_EQUIVALENT.label()).isEqualTo("reported as an oxide equivalent");
        assertThat(ReportedForm.ELEMENTAL.label()).isEqualTo("reported as the element");
        assertThat(ReportedForm.ION.label()).isEqualTo("reported as an ion");
    }

    @Test
    void rejectsAMissingSubstance() {
        NutrientChemistry chemistry =
                NutrientChemistry.of(Nutrients.SULFATE, null, ReportedForm.ION);

        assertThatThrownBy(() -> Observer.forClass(NutrientChemistryTest.class)
                .arguments("invariants", i -> i.valueObject(chemistry, "chemistry"))
                .throwWhenInvalid())
                .isInstanceOf(InvariantViolationException.class)
                .hasMessageContaining("substance");
    }
}
