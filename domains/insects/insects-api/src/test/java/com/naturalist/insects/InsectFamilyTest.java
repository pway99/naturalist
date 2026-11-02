package com.naturalist.insects;

import com.naturalist.fieldnotes.CommonName;
import com.naturalist.RandomValue;
import com.naturalist.clades.Clade;
import com.naturalist.clades.Papilionidae;
import com.naturalist.fieldnotes.Description;
import com.naturalist.taxonomy.TaxonomicFamily;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class InsectFamilyTest {

    @Test
    void withPlacedInReturnsNewInstanceWithUpdatedClade() {
        InsectFamily family = familyWithPlacedIn(null);

        InsectFamily updated = family.withPlacedIn(new Papilionidae());

        assertThat(updated.placedIn()).isEqualTo(new Papilionidae());
        assertThat(family.placedIn()).isNull();
    }

    private static InsectFamily familyWithPlacedIn(Clade placedIn) {
        return new InsectFamily(
                InsectFamilyName.of("papilionidae"),
                InsectOrderName.of("lepidoptera"),
                TaxonomicFamily.of("Papilionidae"),
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
        var rank = familyWith(Set.of(CommonName.of("Zebra Swallowtail"), CommonName.of("Pipevine Swallowtail")));

        assertThat(rank.displayName()).isEqualTo("Pipevine Swallowtail");
    }

    @Test
    void displayName_fallsBackToScientificName() {
        var rank = familyWith(Set.of());

        assertThat(rank.displayName()).isEqualTo("papilionidae");
    }

    private static Description displayNameDescription() {
        return new Description(
                RandomValue.string(), RandomValue.string(),
                RandomValue.string(), RandomValue.string());
    }

    private static InsectFamily familyWith(Set<CommonName> commonNames) {
        return new InsectFamily(InsectFamilyName.of("papilionidae"), InsectOrderName.of("lepidoptera"),
                TaxonomicFamily.of("Papilionidae"), displayNameDescription(), commonNames, null);
    }
}
