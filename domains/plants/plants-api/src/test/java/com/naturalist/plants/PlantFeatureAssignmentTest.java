package com.naturalist.plants;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.naturalist.observability.InvariantObservation;
import com.naturalist.observability.MethodObserver;
import com.naturalist.observability.Observer;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PlantFeatureAssignmentTest {

    private static final Observer observer = Observer.forClass(PlantFeatureAssignmentTest.class);

    @Test
    void validAssignment_hasNoInvariantViolations() {
        MethodObserver mo = observer.forMethod("validAssignment_hasNoInvariantViolations");
        PlantFeatureAssignment a = PlantFeatureAssignment.of(
                PlantFeatureAssignmentId.create(),
                PlantFeatureId.create(),
                PlantFamilyName.of("asteraceae"),
                0);

        InvariantObservation result = mo.observable(a, "assignment");

        assertThat(result.violations()).isEmpty();
    }

    @Test
    void nullComponents_reportViolations() {
        MethodObserver mo = observer.forMethod("nullComponents_reportViolations");
        PlantFeatureAssignment a = new PlantFeatureAssignment(null, null, null, 0);

        InvariantObservation result = mo.observable(a, "assignment");

        assertThat(result.violationNamesRemovingPrefix(mo.observationPoint()))
                .containsExactlyInAnyOrder(".assignment.id", ".assignment.featureId", ".assignment.rankName");
    }

    @Test
    void rankNameRoundtripsThroughItsConcretePermit() throws Exception {
        PlantFeatureAssignment a = PlantFeatureAssignment.of(
                PlantFeatureAssignmentId.create(),
                PlantFeatureId.create(),
                PlantFamilyName.of("asteraceae"),
                0);

        ObjectMapper mapper = new ObjectMapper();
        String json = mapper.writeValueAsString(a);
        assertThat(json).contains("\"rank\":\"FAMILY\"").contains("\"rankName\":\"asteraceae\"");

        PlantFeatureAssignment decoded = mapper.readValue(json, PlantFeatureAssignment.class);
        assertThat(decoded.rankName()).isInstanceOf(PlantFamilyName.class);
        assertThat(decoded.rankName().value()).isEqualTo("asteraceae");
        assertThat(decoded).isEqualTo(a);
    }
}
