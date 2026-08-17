package com.naturalist.plants;

import com.naturalist.RandomValue;
import com.naturalist.clades.Magnoliids;
import com.naturalist.fieldnotes.CommonName;
import com.naturalist.fieldnotes.Description;
import com.naturalist.observability.InvariantObservation;
import com.naturalist.observability.MethodObserver;
import com.naturalist.observability.Observer;
import com.naturalist.taxonomy.TaxonomicOrder;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class PlantOrderTest {

    private static final Observer observer = Observer.forClass(PlantOrderTest.class);

    @Test
    void fullyPopulatedOrderIsValid() {
        MethodObserver mo = observer.forMethod("fullyPopulatedOrderIsValid");
        PlantOrder order = new PlantOrder(
                PlantOrderName.of("lamiales"),
                TaxonomicOrder.of("Lamiales"),
                description(),
                Set.of(CommonName.of("mint order")),
                new Magnoliids());

        InvariantObservation result = mo.namedEntity(order, "order");

        assertThat(result.violations()).isEmpty();
    }

    @Test
    void emptyCommonNamesAndNullPlacedInAreValid() {
        MethodObserver mo = observer.forMethod("emptyCommonNamesAndNullPlacedInAreValid");
        PlantOrder order = new PlantOrder(
                PlantOrderName.of("piperales"),
                TaxonomicOrder.of("Piperales"),
                description(),
                Set.of(),
                null);

        InvariantObservation result = mo.namedEntity(order, "order");

        assertThat(result.violations()).isEmpty();
    }

    @Test
    void allNullComponentsReportEveryViolation() {
        MethodObserver mo = observer.forMethod("allNullComponentsReportEveryViolation");
        PlantOrder order = new PlantOrder(null, null, null, null, null);

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
        PlantOrder order = new PlantOrder(
                PlantOrderName.of("piperales"),
                TaxonomicOrder.of("Piperales"),
                description(),
                Set.of(),
                null);

        PlantOrder updated = order.withPlacedIn(new Magnoliids());

        assertThat(updated.placedIn()).isEqualTo(new Magnoliids());
        assertThat(order.placedIn()).isNull();
    }

    private static Description description() {
        return new Description(
                RandomValue.string(), RandomValue.string(),
                RandomValue.string(), RandomValue.string());
    }
}
