package com.naturalist.insects.lifestage;

import com.naturalist.RandomValue;
import com.naturalist.clades.Clade;
import com.naturalist.clades.Papilionidae;
import com.naturalist.fieldnotes.Description;
import com.naturalist.insects.InsectFamily;
import com.naturalist.insects.InsectFamilyName;
import com.naturalist.insects.InsectGenus;
import com.naturalist.insects.InsectGenusName;
import com.naturalist.insects.InsectOrderName;
import com.naturalist.insects.InsectSpecies;
import com.naturalist.insects.InsectSpeciesName;
import com.naturalist.insects.LifeStageKind;
import com.naturalist.taxonomy.TaxonomicFamily;
import com.naturalist.taxonomy.TaxonomicGenus;
import com.naturalist.taxonomy.TaxonomicSpecies;
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

    private static InsectFamily familyWithPlacedIn(Clade placedIn) {
        return new InsectFamily(
                InsectFamilyName.of("papilionidae"),
                InsectOrderName.of("lepidoptera"),
                TaxonomicFamily.of("Papilionidae"),
                description(),
                Set.of(),
                placedIn,
                null, null, null, null);
    }

    private static InsectGenus genusWithPlacedIn(Clade placedIn) {
        return new InsectGenus(
                InsectGenusName.of("battus"),
                InsectFamilyName.of("papilionidae"),
                InsectOrderName.of("lepidoptera"),
                TaxonomicGenus.of("Battus"),
                description(),
                Set.of(),
                placedIn,
                null, null, null, null);
    }

    private static InsectSpecies speciesWithPlacedIn(Clade placedIn) {
        return new InsectSpecies(
                InsectSpeciesName.of("battus-philenor"),
                InsectGenusName.of("battus"),
                InsectFamilyName.of("papilionidae"),
                TaxonomicSpecies.of("philenor"),
                description(),
                Set.of(),
                null, null,
                placedIn,
                null, null, null, null,
                null, null, null, null, null, null, null);
    }

    private static Description description() {
        return new Description(
                RandomValue.string(), RandomValue.string(),
                RandomValue.string(), RandomValue.string());
    }
}
