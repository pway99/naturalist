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

class InsectSpeciesTest {

    @Test
    void withPlacedInReturnsNewInstanceWithUpdatedClade() {
        InsectSpecies species = speciesWithPlacedIn(null);

        InsectSpecies updated = species.withPlacedIn(new Papilionidae());

        assertThat(updated.placedIn()).isEqualTo(new Papilionidae());
        assertThat(species.placedIn()).isNull();
    }

    private static InsectSpecies speciesWithPlacedIn(Clade placedIn) {
        return new InsectSpecies(
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
    void binomial_withoutGenus_isTheSlug() {
        InsectSpecies species = speciesWith(Set.of());

        assertThat(species.binomial(null)).isEqualTo("battus-philenor");
    }

    @Test
    void binomial_withGenus_joinsGenusAndEpithet() {
        InsectSpecies species = speciesWith(Set.of());
        InsectGenus genus = new InsectGenus(InsectGenusName.of("battus"), InsectFamilyName.of("papilionidae"),
                TaxonomicGenus.of("Battus"), displayNameDescription(), Set.of(), null);

        assertThat(species.binomial(genus)).isEqualTo("Battus philenor");
    }

    private static InsectSpecies speciesWith(Set<CommonName> commonNames) {
        return new InsectSpecies(InsectSpeciesName.of("battus-philenor"), InsectGenusName.of("battus"),
                TaxonomicSpecies.of("philenor"), displayNameDescription(), commonNames,
                null, null, null, null, null, null, null, null, null);
    }
}
