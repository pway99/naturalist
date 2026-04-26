package com.naturalist.chemistry.compound;

import com.naturalist.RandomValue;
import com.naturalist.chemistry.TemperatureFahrenheit;
import com.naturalist.chemistry.compound.role.FunctionalRole;
import com.naturalist.chemistry.element.PeriodicElement;
import com.naturalist.observability.InvariantObservation;
import com.naturalist.observability.Observer;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class CompoundTest {
    private static final Observer observer = Observer.forClass(CompoundTest.class);

    @Test
    void compoundIsValid() {
        var mo = observer.forMethod("compoundIsValid");
        Compound c = validCompound();

        InvariantObservation result = mo.observable(c, "c");

        assertThat(result.violations())
                .isEmpty();
    }

    @Test
    void compoundIsNotValid() {
        var mo = observer.forMethod("compoundIsNotValid");
        Compound c = new Compound(null, null, null, null, null, null, null, null);

        InvariantObservation observation = mo.namedEntity(c, "c");

        assertThat(observation.violationNamesRemovingPrefix(mo.observationPoint()))
                .containsExactlyInAnyOrder(".c.bioavailability",
                        ".c.commonName",
                        ".c.name",
                        ".c.properties",
                        ".c.compoundInfo",
                        ".c.solubility");
    }

    @Test
    void isFumigantTrueWhenVolatilizationPresent() {
        Compound c = validCompound();
        Compound fumigant = new Compound(
                c.name(), c.commonName(), c.compoundInfo(), c.solubility(), c.bioavailability(),
                volatilizationProfile(), c.safety(), c.properties());

        assertThat(fumigant.isFumigant()).isTrue();
    }

    @Test
    void isFumigantFalseWhenVolatilizationAbsent() {
        assertThat(validCompound().isFumigant()).isFalse();
    }

    @Test
    void isHazardousTrueWhenSafetyPresent() {
        Compound hazardous = validCompound().withSafety(safetyProfile());

        assertThat(hazardous.isHazardous()).isTrue();
    }

    @Test
    void isHazardousFalseWhenSafetyAbsent() {
        assertThat(validCompound().isHazardous()).isFalse();
    }

    @Test
    void isChelatedDelegatesToBioavailability() {
        assertThat(compoundWithBioavailability(bioavailability(true)).isChelated()).isTrue();
        assertThat(compoundWithBioavailability(bioavailability(false)).isChelated()).isFalse();
    }

    @Test
    void playsRoleReturnsTrueForPresentRoleAndFalseForAbsent() {
        Compound c = compoundWithRoles(Set.of(new FunctionalRole.Fumigant(), new FunctionalRole.Acaricide()));

        assertThat(c.playsRole(new FunctionalRole.Fumigant())).isTrue();
        assertThat(c.playsRole(new FunctionalRole.Acaricide())).isTrue();
        assertThat(c.playsRole(new FunctionalRole.Chelator())).isFalse();
    }

    static Compound validCompound() {
        return new Compound(
                CompoundName.of(RandomValue.string()),
                RandomValue.string(),
                new CompoundInfo(
                        RandomValue.string(),
                        null,
                        PhCharacter.STRONGLY_ACIDIC,
                        ChemicalNature.ORGANIC,
                        PhysicalForm.COMPLEX,
                        Set.of(new FunctionalRole.Chelator()),
                        Set.of(PeriodicElement.P, PeriodicElement.Be)),
                new SolubilityProfile(Solubility.of(RandomValue.bigDecimal()),
                        SolubilityProfile.SolubilityCategory.INSOLUBLE,
                        RandomValue.bigDecimal(),
                        RandomValue.string()),
                bioavailability(true),
                null,
                null,
                Map.of()
        );
    }

    private static Compound compoundWithBioavailability(BioavailabilityProfile bioavailability) {
        Compound c = validCompound();
        return new Compound(c.name(), c.commonName(), c.compoundInfo(), c.solubility(),
                bioavailability, c.volatilization(), c.safety(), c.properties());
    }

    private static Compound compoundWithRoles(Set<FunctionalRole> roles) {
        Compound c = validCompound();
        CompoundInfo info = c.compoundInfo();
        CompoundInfo updated = new CompoundInfo(
                info.formula(), info.molecularWeight(), info.phCharacter(),
                info.chemicalNature(), info.physicalForm(), roles, info.constituentElements());
        return new Compound(c.name(), c.commonName(), updated, c.solubility(),
                c.bioavailability(), c.volatilization(), c.safety(), c.properties());
    }

    private static BioavailabilityProfile bioavailability(boolean chelateEnhanced) {
        return new BioavailabilityProfile(BioavailabilityProfile.AbsorptionPathway.FOLIAR_BOTH,
                RandomValue.bigDecimal(),
                true, true, chelateEnhanced, RandomValue.string());
    }

    private static VolatilizationProfile volatilizationProfile() {
        return new VolatilizationProfile(
                TemperatureFahrenheit.of(BigDecimal.valueOf(50)),
                TemperatureFahrenheit.of(BigDecimal.valueOf(85)),
                TemperatureFahrenheit.of(BigDecimal.valueOf(65)),
                RandomValue.bigDecimal(),
                RandomValue.string(),
                RandomValue.string());
    }

    private static SafetyProfile safetyProfile() {
        return new SafetyProfile(
                SafetyProfile.HazardLevel.MODERATE,
                RandomValue.bigDecimal(),
                null,
                null,
                false,
                false,
                false,
                RandomValue.string());
    }
}
