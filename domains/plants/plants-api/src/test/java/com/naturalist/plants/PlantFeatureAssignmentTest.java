package com.naturalist.plants;

import com.fasterxml.jackson.databind.InjectableValues;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.naturalist.observability.InvariantObservation;
import com.naturalist.observability.MethodObserver;
import com.naturalist.observability.Observer;
import com.naturalist.taxonomy.OrganismFeatureAssignment;
import com.naturalist.taxonomy.RankNameReconstructor;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Invariant + round-trip coverage for plant feature assignments, now carried by the
 * generic {@link OrganismFeatureAssignment} parameterised on the plant identifiers.
 * {@code rankName} serialises through the shared {@code {"rank":…,"value":…}} codec and
 * rebuilds its concrete permit via a {@link RankNameReconstructor} injected on the mapper.
 */
class PlantFeatureAssignmentTest {

    private static final Observer observer = Observer.forClass(PlantFeatureAssignmentTest.class);

    @Test
    void validAssignment_hasNoInvariantViolations() {
        MethodObserver mo = observer.forMethod("validAssignment_hasNoInvariantViolations");
        OrganismFeatureAssignment<PlantFeatureAssignmentId, PlantFeatureId, PlantRankName> a =
                OrganismFeatureAssignment.of(
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
        OrganismFeatureAssignment<PlantFeatureAssignmentId, PlantFeatureId, PlantRankName> a =
                new OrganismFeatureAssignment<>(null, null, null, 0);

        InvariantObservation result = mo.observable(a, "assignment");

        assertThat(result.violationNamesRemovingPrefix(mo.observationPoint()))
                .containsExactlyInAnyOrder(".assignment.id", ".assignment.featureId", ".assignment.rankName");
    }

    @Test
    void rankNameRoundtripsThroughItsConcretePermit() throws Exception {
        OrganismFeatureAssignment<PlantFeatureAssignmentId, PlantFeatureId, PlantRankName> a =
                OrganismFeatureAssignment.of(
                        PlantFeatureAssignmentId.create(),
                        PlantFeatureId.create(),
                        PlantFamilyName.of("asteraceae"),
                        0);

        ObjectMapper mapper = new ObjectMapper()
                .setInjectableValues(new InjectableValues.Std()
                        .addValue(RankNameReconstructor.class, (RankNameReconstructor) PlantRankName::of));
        String json = mapper.writeValueAsString(a);
        assertThat(json).contains("\"rank\":\"FAMILY\"").contains("\"value\":\"asteraceae\"");

        var type = mapper.getTypeFactory().constructParametricType(
                OrganismFeatureAssignment.class,
                PlantFeatureAssignmentId.class, PlantFeatureId.class, PlantRankName.class);
        OrganismFeatureAssignment<PlantFeatureAssignmentId, PlantFeatureId, PlantRankName> decoded =
                mapper.readValue(json, type);
        assertThat(decoded.rankName()).isInstanceOf(PlantFamilyName.class);
        assertThat(decoded.rankName().value()).isEqualTo("asteraceae");
        assertThat(decoded).isEqualTo(a);
    }
}
