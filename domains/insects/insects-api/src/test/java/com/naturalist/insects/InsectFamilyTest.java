package com.naturalist.insects;

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
}
