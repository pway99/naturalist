package com.naturalist.insects.lifestage;

import com.naturalist.RandomValue;
import com.naturalist.clades.Clade;
import com.naturalist.clades.Hemiptera;
import com.naturalist.clades.Papilionidae;
import com.naturalist.fieldnotes.Description;
import com.naturalist.insects.InsectFamilyInfo;
import com.naturalist.insects.InsectFamilyName;
import com.naturalist.insects.InsectGenusInfo;
import com.naturalist.insects.InsectGenusName;
import com.naturalist.insects.InsectOrderInfo;
import com.naturalist.insects.InsectOrderName;
import com.naturalist.insects.InsectSpeciesInfo;
import com.naturalist.insects.InsectSpeciesName;
import com.naturalist.insects.LifeStageKind;
import com.naturalist.taxonomy.TaxonomicFamily;
import com.naturalist.taxonomy.TaxonomicGenus;
import com.naturalist.taxonomy.TaxonomicOrder;
import com.naturalist.taxonomy.TaxonomicSpecies;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class InsectLifeStagesTest {

    private static final List<LifeStageKind> HOLOMETABOLOUS_STAGES = List.of(
            LifeStageKind.EGG,
            LifeStageKind.LARVA,
            LifeStageKind.PUPA,
            LifeStageKind.ADULT);

    private static final List<LifeStageKind> HEMIMETABOLOUS_STAGES = List.of(
            LifeStageKind.EGG,
            LifeStageKind.NYMPH,
            LifeStageKind.ADULT);

    @Test
    void stagesOfSpeciesPlacedInPapilionidaeReturnsHolometabolousStages() {
        InsectSpeciesInfo species = speciesWithPlacedIn(new Papilionidae());

        assertThat(InsectLifeStages.stagesOf(species))
                .containsExactlyElementsOf(HOLOMETABOLOUS_STAGES);
    }

    @Test
    void stagesOfUnplacedSpeciesReturnsEmptyList() {
        InsectSpeciesInfo species = speciesWithPlacedIn(null);

        assertThat(InsectLifeStages.stagesOf(species)).isEmpty();
    }

    @Test
    void stagesOfGenusPlacedInPapilionidaeReturnsHolometabolousStages() {
        InsectGenusInfo genus = genusWithPlacedIn(new Papilionidae());

        assertThat(InsectLifeStages.stagesOf(genus))
                .containsExactlyElementsOf(HOLOMETABOLOUS_STAGES);
    }

    @Test
    void stagesOfUnplacedGenusReturnsEmptyList() {
        InsectGenusInfo genus = genusWithPlacedIn(null);

        assertThat(InsectLifeStages.stagesOf(genus)).isEmpty();
    }

    @Test
    void stagesOfFamilyPlacedInPapilionidaeReturnsHolometabolousStages() {
        InsectFamilyInfo family = familyWithPlacedIn(new Papilionidae());

        assertThat(InsectLifeStages.stagesOf(family))
                .containsExactlyElementsOf(HOLOMETABOLOUS_STAGES);
    }

    @Test
    void stagesOfUnplacedFamilyReturnsEmptyList() {
        InsectFamilyInfo family = familyWithPlacedIn(null);

        assertThat(InsectLifeStages.stagesOf(family)).isEmpty();
    }

    @Test
    void stagesOfWalkUpUsesSpeciesPlacementWhenSet() {
        InsectSpeciesInfo species = speciesWithPlacedIn(new Papilionidae());

        assertThat(InsectLifeStages.stagesOf(species, null, null, null))
                .containsExactlyElementsOf(HOLOMETABOLOUS_STAGES);
    }

    @Test
    void stagesOfWalkUpPrefersCloserParentWhenSpeciesUnplaced() {
        InsectSpeciesInfo species = speciesWithPlacedIn(null);
        InsectGenusInfo   genus   = genusWithPlacedIn(new Papilionidae());
        InsectFamilyInfo  family  = familyWithPlacedIn(null);
        InsectOrderInfo   order   = orderWithPlacedIn(null);

        assertThat(InsectLifeStages.stagesOf(species, genus, family, order))
                .containsExactlyElementsOf(HOLOMETABOLOUS_STAGES);
    }

    @Test
    void stagesOfWalkUpUsesFamilyPlacementWhenSpeciesAndGenusUnplaced() {
        InsectSpeciesInfo species = speciesWithPlacedIn(null);
        InsectGenusInfo   genus   = genusWithPlacedIn(null);
        InsectFamilyInfo  family  = familyWithPlacedIn(new Papilionidae());
        InsectOrderInfo   order   = orderWithPlacedIn(null);

        assertThat(InsectLifeStages.stagesOf(species, genus, family, order))
                .containsExactlyElementsOf(HOLOMETABOLOUS_STAGES);
    }

    @Test
    void stagesOfWalkUpUsesOrderPlacementWhenSpeciesGenusFamilyUnplaced() {
        InsectSpeciesInfo species = speciesWithPlacedIn(null);
        InsectGenusInfo   genus   = genusWithPlacedIn(null);
        InsectFamilyInfo  family  = familyWithPlacedIn(null);
        InsectOrderInfo   order   = orderWithPlacedIn(new Papilionidae());

        assertThat(InsectLifeStages.stagesOf(species, genus, family, order))
                .containsExactlyElementsOf(HOLOMETABOLOUS_STAGES);
    }

    @Test
    void stagesOfWalkUpReturnsEmptyWhenNoRankIsPlaced() {
        InsectSpeciesInfo species = speciesWithPlacedIn(null);
        InsectGenusInfo   genus   = genusWithPlacedIn(null);
        InsectFamilyInfo  family  = familyWithPlacedIn(null);
        InsectOrderInfo   order   = orderWithPlacedIn(null);

        assertThat(InsectLifeStages.stagesOf(species, genus, family, order)).isEmpty();
    }

    @Test
    void stagesOfWalkUpResolvesHemimetabolousFromFamilyPlacement() {
        InsectSpeciesInfo species = speciesWithPlacedIn(null);
        InsectGenusInfo   genus   = genusWithPlacedIn(null);
        InsectFamilyInfo  family  = familyWithPlacedIn(new Hemiptera());
        InsectOrderInfo   order   = orderWithPlacedIn(null);

        assertThat(InsectLifeStages.stagesOf(species, genus, family, order))
                .containsExactlyElementsOf(HEMIMETABOLOUS_STAGES);
    }

    @Test
    void stagesOfWalkUpAcceptsNullParentsWhenSpeciesPlacedIn() {
        InsectSpeciesInfo species = speciesWithPlacedIn(new Papilionidae());

        // Identical call shape to Task 2's pilot — kept as a separate test
        // to document the nullable-parents contract independently from the
        // precedence assertion the pilot is responsible for.
        assertThat(InsectLifeStages.stagesOf(species, null, null, null))
                .containsExactlyElementsOf(HOLOMETABOLOUS_STAGES);
    }

    @Test
    void stagesOfWalkUpReturnsEmptyWhenSpeciesUnplacedAndParentsNull() {
        InsectSpeciesInfo species = speciesWithPlacedIn(null);

        assertThat(InsectLifeStages.stagesOf(species, null, null, null)).isEmpty();
    }

    private static InsectOrderInfo orderWithPlacedIn(@Nullable Clade placedIn) {
        return new InsectOrderInfo(
                InsectOrderName.of("lepidoptera"),
                TaxonomicOrder.of("Lepidoptera"),
                description(),
                Set.of(),
                placedIn);
    }

    private static InsectFamilyInfo familyWithPlacedIn(@Nullable Clade placedIn) {
        return new InsectFamilyInfo(
                InsectFamilyName.of("papilionidae"),
                InsectOrderName.of("lepidoptera"),
                TaxonomicFamily.of("Papilionidae"),
                description(),
                Set.of(),
                placedIn);
    }

    private static InsectGenusInfo genusWithPlacedIn(@Nullable Clade placedIn) {
        return new InsectGenusInfo(
                InsectGenusName.of("battus"),
                InsectFamilyName.of("papilionidae"),
                TaxonomicGenus.of("Battus"),
                description(),
                Set.of(),
                placedIn);
    }

    private static InsectSpeciesInfo speciesWithPlacedIn(@Nullable Clade placedIn) {
        return new InsectSpeciesInfo(
                InsectSpeciesName.of("battus-philenor"),
                InsectGenusName.of("battus"),
                TaxonomicSpecies.of("philenor"),
                description(),
                Set.of(),
                null,
                placedIn,
                null, null, null, null, null, null, null);
    }

    private static Description description() {
        return new Description(
                RandomValue.string(), RandomValue.string(),
                RandomValue.string(), RandomValue.string());
    }
}
