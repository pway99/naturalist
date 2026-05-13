package com.naturalist.insects.lifestage;

import com.naturalist.insects.LifeStageKind;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MetabolyTest {

    @Test
    void ametabolousStagesAreEggJuvenileAdultInOrder() {
        assertThat(new Ametabolous().stages())
                .containsExactly(LifeStageKind.EGG, LifeStageKind.JUVENILE, LifeStageKind.ADULT);
    }

    @Test
    void hemimetabolousStagesAreEggNymphAdultInOrder() {
        assertThat(new Hemimetabolous().stages())
                .containsExactly(LifeStageKind.EGG, LifeStageKind.NYMPH, LifeStageKind.ADULT);
    }

    @Test
    void holometabolousStagesAreEggLarvaPupaAdultInOrder() {
        assertThat(new Holometabolous().stages())
                .containsExactly(LifeStageKind.EGG, LifeStageKind.LARVA, LifeStageKind.PUPA, LifeStageKind.ADULT);
    }

    @Test
    void recordsWithSameTypeAreValueEqual() {
        assertThat(new Holometabolous()).isEqualTo(new Holometabolous());
        assertThat(new Hemimetabolous()).isEqualTo(new Hemimetabolous());
        assertThat(new Ametabolous()).isEqualTo(new Ametabolous());
    }

    @Test
    void recordsOfDifferentTypeAreNotEqual() {
        assertThat(new Holometabolous()).isNotEqualTo((Object) new Hemimetabolous());
        assertThat(new Hemimetabolous()).isNotEqualTo((Object) new Ametabolous());
    }

    @Test
    void metabolyTraitWrapsTheGivenMetaboly() {
        MetabolyTrait trait = new MetabolyTrait(new Holometabolous());

        assertThat(trait.metaboly()).isEqualTo(new Holometabolous());
    }
}
