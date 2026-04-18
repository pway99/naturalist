package com.naturalist.chemistry.compound;

import com.naturalist.RandomValue;
import com.naturalist.chemistry.element.PeriodicElement;
import com.naturalist.observability.InvariantObservation;
import com.naturalist.observability.Observer;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class CompoundTest {
    private static final Observer observer = Observer.forClass(CompoundTest.class);

    @Test
    void compoundIsValid() {
        // Arrange
        var mo = observer.forMethod("compoundIsValid");
        Compound c = new Compound(
                null,
                CompoundName.of(RandomValue.string()),
                RandomValue.string(),
                new CompoundInfo(RandomValue.string(), null, CompoundType.CHELATE, PhCharacter.STRONGLY_ACIDIC, Set.of(PeriodicElement.P, PeriodicElement.Be)),
                new SolubilityProfile(Solubility.of(RandomValue.bigDecimal()),
                        SolubilityProfile.SolubilityCategory.INSOLUBLE,
                        RandomValue.bigDecimal(),
                        RandomValue.string()),
                new BioavailabilityProfile(BioavailabilityProfile.AbsorptionPathway.FOLIAR_BOTH,
                        RandomValue.bigDecimal(),
                        true, true, true, RandomValue.string()),
                null,
                null,
                Map.of()
        );

        // Act
        InvariantObservation result = mo.observable(c, "c");

        // Assert
        assertThat(result.violations())
                .isEmpty();
    }

    @Test
    void compoundIsNotValid() {
        // Arrange
        var mo = observer.forMethod("compoundIsNotValid");
        Compound c = new Compound(null, null, null, null, null, null, null, null, null);

        // Act
        InvariantObservation observation = mo.entity(c, "c");

        assertThat(observation.violationNamesRemovingPrefix(mo.observationPoint()))
                .containsExactlyInAnyOrder(".c.bioavailability",
                        ".c.commonName",
                        ".c.name",
                        ".c.properties",
                        ".c.compoundInfo",
                        ".c.solubility");

    }
}