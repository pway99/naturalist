package com.naturalist.plants;

import com.naturalist.RandomValue;
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
                Set.of(CommonName.of("mint order")));

        InvariantObservation result = mo.namedEntity(order, "order");

        assertThat(result.violations()).isEmpty();
    }

    @Test
    void emptyCommonNamesIsValid() {
        MethodObserver mo = observer.forMethod("emptyCommonNamesIsValid");
        PlantOrder order = new PlantOrder(
                PlantOrderName.of("piperales"),
                TaxonomicOrder.of("Piperales"),
                description(),
                Set.of());

        InvariantObservation result = mo.namedEntity(order, "order");

        assertThat(result.violations()).isEmpty();
    }

    @Test
    void allNullComponentsReportEveryViolation() {
        MethodObserver mo = observer.forMethod("allNullComponentsReportEveryViolation");
        PlantOrder order = new PlantOrder(null, null, null, null);

        InvariantObservation result = mo.namedEntity(order, "order");

        assertThat(result.violationNamesRemovingPrefix(mo.observationPoint()))
                .containsExactlyInAnyOrder(
                        ".order.name",
                        ".order.order",
                        ".order.description",
                        ".order.commonNames");
    }

    private static Description description() {
        return new Description(
                RandomValue.string(), RandomValue.string(),
                RandomValue.string(), RandomValue.string());
    }
}
