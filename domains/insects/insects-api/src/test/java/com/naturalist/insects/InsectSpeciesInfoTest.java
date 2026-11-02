package com.naturalist.insects;

import com.naturalist.taxonomy.TaxonomicGenus;
import com.naturalist.fieldnotes.CommonName;
import com.naturalist.RandomValue;
import com.naturalist.clades.Clade;
import com.naturalist.clades.Papilionidae;
import com.naturalist.fieldnotes.Description;
import com.naturalist.taxonomy.TaxonomicSpecies;
import org.junit.jupiter.api.Test;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class InsectSpeciesInfoTest {

    @Test
    void withPlacedInReturnsNewInstanceWithUpdatedClade() {
        InsectSpeciesInfo species = speciesWithPlacedIn(null);

        InsectSpeciesInfo updated = species.withPlacedIn(new Papilionidae());

        assertThat(updated.placedIn()).isEqualTo(new Papilionidae());
        assertThat(species.placedIn()).isNull();
    }

    private static InsectSpeciesInfo speciesWithPlacedIn(Clade placedIn) {
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

    @Test
    void displayName_prefersAlphabeticallyFirstCommonName() {
        var rank = speciesWith(Set.of(CommonName.of("Zebra Swallowtail"), CommonName.of("Pipevine Swallowtail")));

        assertThat(rank.displayName()).isEqualTo("Pipevine Swallowtail");
    }

    @Test
    void displayName_fallsBackToScientificName() {
        var rank = speciesWith(Set.of());

        assertThat(rank.displayName()).isEqualTo("battus-philenor");
    }

    private static Description displayNameDescription() {
        return new Description(
                RandomValue.string(), RandomValue.string(),
                RandomValue.string(), RandomValue.string());
    }

    @Test
    void displayName_breaksCaseOnlyTiesByNaturalOrder() {
        var species = speciesWith(Set.of(CommonName.of("monarch"), CommonName.of("Monarch")));

        assertThat(species.displayName()).isEqualTo("Monarch");
    }

    @Test
    void displayName_withGenusAndNoCommonName_fallsBackToTheBinomial() {
        InsectGenusInfo genus = new InsectGenusInfo(InsectGenusName.of("battus"), InsectFamilyName.of("papilionidae"),
                TaxonomicGenus.of("Battus"), displayNameDescription(), Set.of(), null);

        assertThat(speciesWith(Set.of()).displayName(genus)).isEqualTo("Battus philenor");
        assertThat(speciesWith(Set.of(CommonName.of("Pipevine Swallowtail"))).displayName(genus))
                .isEqualTo("Pipevine Swallowtail");
    }

    @Test
    void binomial_withoutGenus_isTheSlug() {
        InsectSpeciesInfo species = speciesWith(Set.of());

        assertThat(species.binomial(null)).isEqualTo("battus-philenor");
    }

    @Test
    void binomial_withGenus_joinsGenusAndEpithet() {
        InsectSpeciesInfo species = speciesWith(Set.of());
        InsectGenusInfo genus = new InsectGenusInfo(InsectGenusName.of("battus"), InsectFamilyName.of("papilionidae"),
                TaxonomicGenus.of("Battus"), displayNameDescription(), Set.of(), null);

        assertThat(species.binomial(genus)).isEqualTo("Battus philenor");
    }

    private static InsectSpeciesInfo speciesWith(Set<CommonName> commonNames) {
        return new InsectSpeciesInfo(InsectSpeciesName.of("battus-philenor"), InsectGenusName.of("battus"),
                TaxonomicSpecies.of("philenor"), displayNameDescription(), commonNames,
                null, null, null, null, null, null, null, null, null);
    }
}
