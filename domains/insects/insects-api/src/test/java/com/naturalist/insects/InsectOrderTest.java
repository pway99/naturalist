package com.naturalist.insects;

import com.naturalist.RandomValue;
import com.naturalist.clades.Clade;
import com.naturalist.clades.Holometabola;
import com.naturalist.fieldnotes.Description;
import com.naturalist.observability.InvariantObservation;
import com.naturalist.observability.Observer;
import com.naturalist.taxonomy.TaxonomicOrder;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class InsectOrderTest {
    private static final Observer observer = Observer.forClass(InsectOrderTest.class);

    @Test
    void validOrderPassesAllInvariants() {
        var mo = observer.forMethod("validOrderPassesAllInvariants");
        InsectOrder order = new InsectOrder(
                InsectOrderName.of("diptera"),
                TaxonomicOrder.of("Diptera"),
                description(),
                Set.of(),
                null);

        InvariantObservation result = mo.namedEntity(order, "order");

        assertThat(result.violations()).isEmpty();
    }

    @Test
    void nullComponentsProduceExpectedInvariantViolations() {
        var mo = observer.forMethod("nullComponentsProduceExpectedInvariantViolations");
        InsectOrder order = new InsectOrder(
                null, null, null, null, null);

        InvariantObservation result = mo.namedEntity(order, "order");

        assertThat(result.violationNamesRemovingPrefix(mo.observationPoint()))
                .containsExactlyInAnyOrder(
                        ".order.name",
                        ".order.order",
                        ".order.description",
                        ".order.commonNames");
    }

    @Test
    void withPlacedInReturnsNewInstanceWithUpdatedClade() {
        InsectOrder order = orderWithPlacedIn(null);

        InsectOrder updated = order.withPlacedIn(new Holometabola());

        assertThat(updated.placedIn()).isEqualTo(new Holometabola());
        assertThat(order.placedIn()).isNull();
    }

    private static InsectOrder orderWithPlacedIn(Clade placedIn) {
        return new InsectOrder(
                InsectOrderName.of("diptera"),
                TaxonomicOrder.of("Diptera"),
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
