package com.naturalist.chemistry.compound;

import com.naturalist.chemistry.compound.role.FunctionalRole;
import com.naturalist.ddd.BehavioralCollection;

import java.util.Collection;
import java.util.List;

public final class CompoundCollection extends BehavioralCollection<Compound> {

    CompoundCollection(Collection<Compound> compounds) {
        super(compounds);
    }

    public static CompoundCollection of(Collection<Compound> compounds) {
        return new CompoundCollection(compounds);
    }

    public static CompoundCollection empty() {
        return new CompoundCollection(List.of());
    }

    public CompoundCollection withChemicalNature(ChemicalNature nature) {
        return new CompoundCollection(
                stream()
                        .filter(c -> c.compoundInfo().chemicalNature() == nature)
                        .toList()
        );
    }

    public CompoundCollection withFunctionalRole(FunctionalRole role) {
        return new CompoundCollection(
                stream()
                        .filter(c -> c.playsRole(role))
                        .toList()
        );
    }
}
