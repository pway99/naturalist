package com.naturalist.insects.lifestage;

import com.naturalist.insects.LifeStageKind;

import java.util.List;

/**
 * Complete metamorphosis: egg → larva → pupa → adult, with a
 * morphologically distinct feeding larva, a non-feeding pupal stage
 * during which adult tissues develop, and a winged adult specialised
 * for dispersal and reproduction. Covers every member of Holometabola
 * — Coleoptera, Lepidoptera, Diptera, Hymenoptera, Neuroptera,
 * Trichoptera, Mecoptera, Siphonaptera, and Strepsiptera.
 */
public record Holometabolous() implements Metaboly {

    @Override
    public List<LifeStageKind> stages() {
        return List.of(
                LifeStageKind.EGG,
                LifeStageKind.LARVA,
                LifeStageKind.PUPA,
                LifeStageKind.ADULT);
    }
}
