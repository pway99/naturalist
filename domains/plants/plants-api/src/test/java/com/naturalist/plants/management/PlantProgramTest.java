package com.naturalist.plants.management;

import com.naturalist.RandomValue;
import com.naturalist.fieldnotes.Description;
import com.naturalist.observability.InvariantObservation;
import com.naturalist.observability.MethodObserver;
import com.naturalist.observability.Observer;
import com.naturalist.plants.PlantName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PlantProgramTest {

    private static final Observer observer = Observer.forClass(PlantProgramTest.class);

    @Test
    void constraintBearingProgramIsValid() {
        MethodObserver mo = observer.forMethod("constraintBearingProgramIsValid");
        PlantProgram program = program("never apply pesticide to this plant", null);

        InvariantObservation result = mo.namedEntity(program, "program");

        assertThat(result.violations()).isEmpty();
    }

    @Test
    void pureScheduleProgramIsValid() {
        // The two program shapes: constraint-bearing and pure-schedule. A null
        // constraint is the legal encoding of the latter, not a missing field.
        MethodObserver mo = observer.forMethod("pureScheduleProgramIsValid");
        PlantProgram program = program(null, "inspect weekly April through June");

        InvariantObservation result = mo.namedEntity(program, "program");

        assertThat(result.violations()).isEmpty();
    }

    @Test
    void allNullComponentsReportEveryViolation() {
        MethodObserver mo = observer.forMethod("allNullComponentsReportEveryViolation");
        PlantProgram program = new PlantProgram(null, null, null, null, null);

        InvariantObservation result = mo.namedEntity(program, "program");

        assertThat(result.violationNamesRemovingPrefix(mo.observationPoint()))
                .containsExactlyInAnyOrder(
                        ".program.name",
                        ".program.plantName",
                        ".program.description");
    }

    @Test
    void hasConstraintDistinguishesTheTwoProgramShapes() {
        assertThat(program("never apply pesticide", null).hasConstraint()).isTrue();
        assertThat(program(null, "inspect weekly").hasConstraint()).isFalse();
    }

    private static PlantProgram program(String constraint, String notes) {
        return new PlantProgram(
                PlantProgramName.of("pipevine-pesticide-exclusion"),
                PlantName.of("aristolochia-californica"),
                description(),
                constraint,
                notes);
    }

    private static Description description() {
        return new Description(
                RandomValue.string(), RandomValue.string(),
                RandomValue.string(), RandomValue.string());
    }
}
