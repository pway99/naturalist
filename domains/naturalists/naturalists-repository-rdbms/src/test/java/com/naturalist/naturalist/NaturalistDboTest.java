package com.naturalist.naturalist;

import com.naturalist.exception.InvariantViolationException;
import com.naturalist.observability.Observer;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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

    @Test void from_throwsWhenAValueExceedsItsColumnWidth() {
        // givenName is unconstrained on the entity but the DBO/column caps it at 100 —
        // from(...) must fail here, one step before the database would truncate/reject.
        Naturalist n = new Naturalist(NaturalistName.of("amir-hassan"), "G".repeat(101), null,
                NaturalistRole.KEEPER, EcologicalStage.NATURALIST, null);
        assertThatThrownBy(() -> NaturalistDbo.from(n))
                .isInstanceOf(InvariantViolationException.class);
    }
}
