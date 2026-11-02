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

class InsectGenusInfoTest {

    @Test
    void withPlacedInReturnsNewInstanceWithUpdatedClade() {
        InsectGenusInfo genus = genusWithPlacedIn(null);

        InsectGenusInfo updated = genus.withPlacedIn(new Papilionidae());

        assertThat(updated.placedIn()).isEqualTo(new Papilionidae());
        assertThat(genus.placedIn()).isNull();
    }

    private static InsectGenusInfo genusWithPlacedIn(Clade placedIn) {
        return new InsectGenusInfo(
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

        assertThat(rank.displayName()).isEqualTo("Battus");
    }

    private static Description displayNameDescription() {
        return new Description(
                RandomValue.string(), RandomValue.string(),
                RandomValue.string(), RandomValue.string());
    }

    private static InsectGenusInfo genusWith(Set<CommonName> commonNames) {
        return new InsectGenusInfo(InsectGenusName.of("battus"), InsectFamilyName.of("papilionidae"),
                TaxonomicGenus.of("Battus"), displayNameDescription(), commonNames, null);
    }
}
