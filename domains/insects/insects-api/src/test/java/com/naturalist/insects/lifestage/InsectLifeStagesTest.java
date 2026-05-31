package com.naturalist.insects.lifestage;

import com.naturalist.RandomValue;
import com.naturalist.clades.Clade;
import com.naturalist.clades.Hemiptera;
import com.naturalist.clades.Papilionidae;
import com.naturalist.fieldnotes.Description;
import com.naturalist.insects.InsectFamily;
import com.naturalist.insects.InsectFamilyName;
import com.naturalist.insects.InsectGenus;
import com.naturalist.insects.InsectGenusName;
import com.naturalist.insects.InsectOrder;
import com.naturalist.insects.InsectOrderName;
import com.naturalist.insects.InsectSpecies;
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
        InsectSpecies species = speciesWithPlacedIn(new Papilionidae());

        assertThat(InsectLifeStages.stagesOf(species))
                .containsExactlyElementsOf(HOLOMETABOLOUS_STAGES);
    }

    @Test
    void stagesOfUnplacedSpeciesReturnsEmptyList() {
        InsectSpecies species = speciesWithPlacedIn(null);

        assertThat(InsectLifeStages.stagesOf(species)).isEmpty();
    }

    @Test
    void stagesOfGenusPlacedInPapilionidaeReturnsHolometabolousStages() {
        InsectGenus genus = genusWithPlacedIn(new Papilionidae());

        assertThat(InsectLifeStages.stagesOf(genus))
                .containsExactlyElementsOf(HOLOMETABOLOUS_STAGES);
    }

    @Test
    void stagesOfUnplacedGenusReturnsEmptyList() {
        InsectGenus genus = genusWithPlacedIn(null);

        assertThat(InsectLifeStages.stagesOf(genus)).isEmpty();
    }

    @Test
    void stagesOfFamilyPlacedInPapilionidaeReturnsHolometabolousStages() {
        InsectFamily family = familyWithPlacedIn(new Papilionidae());

        assertThat(InsectLifeStages.stagesOf(family))
                .containsExactlyElementsOf(HOLOMETABOLOUS_STAGES);
    }

    @Test
    void stagesOfUnplacedFamilyReturnsEmptyList() {
        InsectFamily family = familyWithPlacedIn(null);

        assertThat(InsectLifeStages.stagesOf(family)).isEmpty();
    }

    @Test
    void stagesOfWalkUpUsesSpeciesPlacementWhenSet() {
        InsectSpecies species = speciesWithPlacedIn(new Papilionidae());

        assertThat(InsectLifeStages.stagesOf(species, null, null, null))
                .containsExactlyElementsOf(HOLOMETABOLOUS_STAGES);
    }

    @Test
    void stagesOfWalkUpPrefersCloserParentWhenSpeciesUnplaced() {
        InsectSpecies species = speciesWithPlacedIn(null);
        InsectGenus   genus   = genusWithPlacedIn(new Papilionidae());
        InsectFamily  family  = familyWithPlacedIn(null);
        InsectOrder   order   = orderWithPlacedIn(null);

        assertThat(InsectLifeStages.stagesOf(species, genus, family, order))
                .containsExactlyElementsOf(HOLOMETABOLOUS_STAGES);
    }

    @Test
    void stagesOfWalkUpUsesFamilyPlacementWhenSpeciesAndGenusUnplaced() {
        InsectSpecies species = speciesWithPlacedIn(null);
        InsectGenus   genus   = genusWithPlacedIn(null);
        InsectFamily  family  = familyWithPlacedIn(new Papilionidae());
        InsectOrder   order   = orderWithPlacedIn(null);

        assertThat(InsectLifeStages.stagesOf(species, genus, family, order))
                .containsExactlyElementsOf(HOLOMETABOLOUS_STAGES);
    }

    @Test
    void stagesOfWalkUpUsesOrderPlacementWhenSpeciesGenusFamilyUnplaced() {
        InsectSpecies species = speciesWithPlacedIn(null);
        InsectGenus   genus   = genusWithPlacedIn(null);
        InsectFamily  family  = familyWithPlacedIn(null);
        InsectOrder   order   = orderWithPlacedIn(new Papilionidae());

        assertThat(InsectLifeStages.stagesOf(species, genus, family, order))
                .containsExactlyElementsOf(HOLOMETABOLOUS_STAGES);
    }

    @Test
    void stagesOfWalkUpReturnsEmptyWhenNoRankIsPlaced() {
        InsectSpecies species = speciesWithPlacedIn(null);
        InsectGenus   genus   = genusWithPlacedIn(null);
        InsectFamily  family  = familyWithPlacedIn(null);
        InsectOrder   order   = orderWithPlacedIn(null);

        assertThat(InsectLifeStages.stagesOf(species, genus, family, order)).isEmpty();
    }

    @Test
    void stagesOfWalkUpResolvesHemimetabolousFromFamilyPlacement() {
        InsectSpecies species = speciesWithPlacedIn(null);
        InsectGenus   genus   = genusWithPlacedIn(null);
        InsectFamily  family  = familyWithPlacedIn(new Hemiptera());
        InsectOrder   order   = orderWithPlacedIn(null);

        assertThat(InsectLifeStages.stagesOf(species, genus, family, order))
                .containsExactlyElementsOf(HEMIMETABOLOUS_STAGES);
    }

    @Test
    void stagesOfWalkUpAcceptsNullParentsWhenSpeciesPlacedIn() {
        InsectSpecies species = speciesWithPlacedIn(new Papilionidae());

        // Identical call shape to Task 2's pilot — kept as a separate test
        // to document the nullable-parents contract independently from the
        // precedence assertion the pilot is responsible for.
        assertThat(InsectLifeStages.stagesOf(species, null, null, null))
                .containsExactlyElementsOf(HOLOMETABOLOUS_STAGES);
    }

    @Test
    void stagesOfWalkUpReturnsEmptyWhenSpeciesUnplacedAndParentsNull() {
        InsectSpecies species = speciesWithPlacedIn(null);

        assertThat(InsectLifeStages.stagesOf(species, null, null, null)).isEmpty();
    }

    private static InsectOrder orderWithPlacedIn(@Nullable Clade placedIn) {
        return new InsectOrder(
                InsectOrderName.of("lepidoptera"),
                TaxonomicOrder.of("Lepidoptera"),
                description(),
                Set.of(),
                placedIn);
    }

    private static InsectFamily familyWithPlacedIn(@Nullable Clade placedIn) {
        return new InsectFamily(
                InsectFamilyName.of("papilionidae"),
                InsectOrderName.of("lepidoptera"),
                TaxonomicFamily.of("Papilionidae"),
                description(),
                Set.of(),
                placedIn);
    }

    private static InsectGenus genusWithPlacedIn(@Nullable Clade placedIn) {
        return new InsectGenus(
                InsectGenusName.of("battus"),
                InsectFamilyName.of("papilionidae"),
                InsectOrderName.of("lepidoptera"),
                TaxonomicGenus.of("Battus"),
                description(),
                Set.of(),
                placedIn);
    }

    private static InsectSpecies speciesWithPlacedIn(@Nullable Clade placedIn) {
        return new InsectSpecies(
                InsectSpeciesName.of("battus-philenor"),
                InsectGenusName.of("battus"),
                InsectFamilyName.of("papilionidae"),
                TaxonomicSpecies.of("philenor"),
                description(),
                Set.of(),
                null, null,
                placedIn,
                null, null, null, null, null, null, null);
    }

    private static Description description() {
        return new Description(
                RandomValue.string(), RandomValue.string(),
                RandomValue.string(), RandomValue.string());
    }
}
