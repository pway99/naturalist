package com.naturalist.naturalist;

import com.naturalist.observability.Observer;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class NaturalistDboTest {

    private static Naturalist sample(String slug, String family, String notes) {
        return new Naturalist(NaturalistName.of(slug), "Given", family,
                NaturalistRole.KEEPER, EcologicalStage.NATURALIST, notes);
    }

    @Test void roundTrip_preservesAllFields() {
        Naturalist n = sample("amir-hassan", "Hassan", "some notes");
        assertThat(NaturalistDbo.from(n).toEntity()).usingRecursiveComparison().isEqualTo(n);
    }

    @Test void roundTrip_preservesNulls() {
        Naturalist n = sample("flora-mendez", null, null);
        assertThat(NaturalistDbo.from(n).toEntity()).usingRecursiveComparison().isEqualTo(n);
    }

    @Test void invariants_flagOverLongName() {
        NaturalistDbo dbo = NaturalistDbo.from(sample("amir-hassan", "Hassan", null));
        dbo.name = "x".repeat(65); // exceeds VARCHAR(64)
        var violations = Observer.forClass(NaturalistDboTest.class)
                .arguments("t", i -> i.observable(dbo, "dbo")).violations();
        assertThat(violations).isNotEmpty();
    }
}
