package com.naturalist.insects.lifestage;

import com.naturalist.insects.LifeStageKind;

import java.util.List;

/**
 * The simplest hexapod developmental pattern: an animal that hatches in
 * adult-like form and grows by direct moulting into the reproductive
 * adult, without distinct intermediate stages. Found in the wingless
 * primitive hexapods — silverfish (Zygentoma), bristletails (Archaeognatha),
 * and the entognathous orders.
 */
public record Ametabolous() implements Metaboly {

    @Override
    public List<LifeStageKind> stages() {
        return List.of(
                LifeStageKind.EGG,
                LifeStageKind.JUVENILE,
                LifeStageKind.ADULT);
    }
}
