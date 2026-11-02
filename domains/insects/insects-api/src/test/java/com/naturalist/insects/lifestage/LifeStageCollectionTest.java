package com.naturalist.insects.lifestage;

import com.naturalist.RandomValue;
import com.naturalist.fieldnotes.Description;
import com.naturalist.insects.InsectSpeciesName;
import com.naturalist.insects.LifeStageKind;
import com.naturalist.insects.LifeStageName;
import com.naturalist.insects.lifestage.InsectLifeStageEntityCollections.LifeStageCollection;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class LifeStageCollectionTest {

    private static final InsectSpeciesName SPECIES = InsectSpeciesName.of("battus-philenor");

    @Test
    void orderedBy_followsTheGivenSequence() {
        LifeStageCollection stages = LifeStageCollection.of(
                List.of(stage(LifeStageKind.ADULT), stage(LifeStageKind.EGG), stage(LifeStageKind.LARVA)));

        LifeStageCollection ordered = stages.orderedBy(
                List.of(LifeStageKind.EGG, LifeStageKind.LARVA, LifeStageKind.PUPA, LifeStageKind.ADULT));

        assertThat(ordered.stream().map(LifeStage::kind))
                .containsExactly(LifeStageKind.EGG, LifeStageKind.LARVA, LifeStageKind.ADULT);
    }

    @Test
    void orderedBy_unknownKindsFallBackToEnumOrder() {
        LifeStageCollection stages = LifeStageCollection.of(
                List.of(stage(LifeStageKind.ADULT), stage(LifeStageKind.EGG), stage(LifeStageKind.LARVA)));

        LifeStageCollection ordered = stages.orderedBy(List.of());

        assertThat(ordered.stream().map(LifeStage::kind))
                .containsExactly(LifeStageKind.EGG, LifeStageKind.LARVA, LifeStageKind.ADULT);
    }

    // Phenology, habitat and chemistry are irrelevant to ordering, so they stay null here.
    private static LifeStage stage(LifeStageKind kind) {
        LifeStageName name = LifeStageName.of(SPECIES, kind);
        Description description = new Description(
                RandomValue.string(), RandomValue.string(), RandomValue.string(), RandomValue.string());
        return switch (kind) {
            case EGG -> new EggStage(name, SPECIES, null, null, null, description, null, null, null);
            case LARVA -> new LarvaStage(name, SPECIES, null, null, null, description,
                    null, List.of(), List.of(), null, null);
            case PUPA -> new PupaStage(name, SPECIES, null, null, null, description,
                    null, null, null);
            case ADULT -> new AdultStage(name, SPECIES, null, null, null, description,
                    null, List.of(), null, null);
            default -> throw new IllegalArgumentException(kind.name());
        };
    }
}
