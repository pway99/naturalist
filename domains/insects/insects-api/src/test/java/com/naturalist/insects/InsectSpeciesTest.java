package com.naturalist.insects;

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
