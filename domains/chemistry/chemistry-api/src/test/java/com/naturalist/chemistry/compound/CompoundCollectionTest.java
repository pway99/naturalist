package com.naturalist.chemistry.compound;

import com.naturalist.chemistry.compound.CompoundEntityCollections.CompoundCollection;
import com.naturalist.chemistry.compound.role.FunctionalRole;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class CompoundCollectionTest {

    @Test
    void withChemicalNatureReturnsOnlyMatchingNature() {
        Compound organic = compound(ChemicalNature.ORGANIC, Set.of(new FunctionalRole.Acaricide()));
        Compound inorganic = compound(ChemicalNature.INORGANIC, Set.of(new FunctionalRole.Fertilizer()));

        CompoundCollection filtered = CompoundCollection.of(List.of(organic, inorganic))
                .withChemicalNature(ChemicalNature.ORGANIC);

        assertThat(filtered.stream().toList()).containsExactly(organic);
    }

    @Test
    void withFunctionalRoleReturnsOnlyCompoundsPlayingRole() {
        Compound fumigant = compound(ChemicalNature.ORGANIC, Set.of(new FunctionalRole.Fumigant()));
        Compound fertilizer = compound(ChemicalNature.INORGANIC, Set.of(new FunctionalRole.Fertilizer()));

        CompoundCollection filtered = CompoundCollection.of(List.of(fumigant, fertilizer))
                .withFunctionalRole(new FunctionalRole.Fumigant());

        assertThat(filtered.stream().toList()).containsExactly(fumigant);
    }

    private static Compound compound(ChemicalNature nature, Set<FunctionalRole> roles) {
        Compound base = CompoundTest.validCompound();
        CompoundInfo info = base.compoundInfo();
        CompoundInfo updated = new CompoundInfo(
                info.formula(), info.molecularWeight(), info.phCharacter(),
                nature, info.physicalForm(), roles, info.constituentElements());
        return new Compound(base.name(), base.commonName(), updated, base.solubility(),
                base.bioavailability(), base.volatilization(), base.safety(),
                base.omriListed(), base.cdfaRegistered(), base.properties());
    }
}
