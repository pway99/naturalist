package com.naturalist.insects;

import com.naturalist.fieldnotes.CommonName;
import com.naturalist.RandomValue;
import com.naturalist.clades.Clade;
import com.naturalist.clades.Papilionidae;
import com.naturalist.fieldnotes.Description;
import com.naturalist.taxonomy.TaxonomicGenus;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class InsectGenusTest {

    @Test
    void withPlacedInReturnsNewInstanceWithUpdatedClade() {
        InsectGenus genus = genusWithPlacedIn(null);

        InsectGenus updated = genus.withPlacedIn(new Papilionidae());

        assertThat(updated.placedIn()).isEqualTo(new Papilionidae());
        assertThat(genus.placedIn()).isNull();
    }

    private static InsectGenus genusWithPlacedIn(Clade placedIn) {
        return new InsectGenus(
                InsectGenusName.of("battus"),
                InsectFamilyName.of("papilionidae"),
                TaxonomicGenus.of("Battus"),
                description(),
                Set.of(),
                placedIn);
    }

    private static Description description() {
        return new Description(
                RandomValue.string(), RandomValue.string(),
                RandomValue.string(), RandomValue.string());
    }

    @Test
    void displayName_prefersAlphabeticallyFirstCommonName() {
        var rank = genusWith(Set.of(CommonName.of("Zebra Swallowtail"), CommonName.of("Pipevine Swallowtail")));

        assertThat(rank.displayName()).isEqualTo("Pipevine Swallowtail");
    }

    @Test
    void displayName_fallsBackToScientificName() {
        var rank = genusWith(Set.of());

        assertThat(rank.displayName()).isEqualTo("battus");
    }

    private static Description displayNameDescription() {
        return new Description(
                RandomValue.string(), RandomValue.string(),
                RandomValue.string(), RandomValue.string());
    }

    private static InsectGenus genusWith(Set<CommonName> commonNames) {
        return new InsectGenus(InsectGenusName.of("battus"), InsectFamilyName.of("papilionidae"),
                TaxonomicGenus.of("Battus"), displayNameDescription(), commonNames, null);
    }
}
