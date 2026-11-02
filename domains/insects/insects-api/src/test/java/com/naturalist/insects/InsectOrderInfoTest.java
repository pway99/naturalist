package com.naturalist.insects;

import com.naturalist.fieldnotes.CommonName;
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

class InsectOrderInfoTest {
    private static final Observer observer = Observer.forClass(InsectOrderInfoTest.class);

    @Test
    void validOrderPassesAllInvariants() {
        var mo = observer.forMethod("validOrderPassesAllInvariants");
        InsectOrderInfo order = new InsectOrderInfo(
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
        InsectOrderInfo order = new InsectOrderInfo(
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
        InsectOrderInfo order = orderWithPlacedIn(null);

        InsectOrderInfo updated = order.withPlacedIn(new Holometabola());

        assertThat(updated.placedIn()).isEqualTo(new Holometabola());
        assertThat(order.placedIn()).isNull();
    }

    private static InsectOrderInfo orderWithPlacedIn(Clade placedIn) {
        return new InsectOrderInfo(
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

    @Test
    void displayName_prefersAlphabeticallyFirstCommonName() {
        var rank = orderWith(Set.of(CommonName.of("Zebra Swallowtail"), CommonName.of("Pipevine Swallowtail")));

        assertThat(rank.displayName()).isEqualTo("Pipevine Swallowtail");
    }

    @Test
    void displayName_fallsBackToScientificName() {
        var rank = orderWith(Set.of());

        assertThat(rank.displayName()).isEqualTo("Lepidoptera");
    }

    private static Description displayNameDescription() {
        return new Description(
                RandomValue.string(), RandomValue.string(),
                RandomValue.string(), RandomValue.string());
    }

    private static InsectOrderInfo orderWith(Set<CommonName> commonNames) {
        return new InsectOrderInfo(InsectOrderName.of("lepidoptera"), TaxonomicOrder.of("Lepidoptera"),
                displayNameDescription(), commonNames, null);
    }
}
