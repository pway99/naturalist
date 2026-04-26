package com.naturalist.chemistry.product;

import com.naturalist.RandomValue;
import com.naturalist.chemistry.compound.CompoundName;
import com.naturalist.observability.InvariantObservation;
import com.naturalist.observability.Observer;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class ProductTest {
    private static final Observer observer = Observer.forClass(ProductTest.class);

    @Test
    void productIsValid() {
        var mo = observer.forMethod("productIsValid");
        Product p = validProduct();

        InvariantObservation result = mo.namedEntity(p, "p");

        assertThat(result.violations()).isEmpty();
    }

    @Test
    void productIsNotValid() {
        var mo = observer.forMethod("productIsNotValid");
        Product p = new Product(null, null, null, null);

        InvariantObservation observation = mo.namedEntity(p, "p");

        assertThat(observation.violationNamesRemovingPrefix(mo.observationPoint()))
                .containsExactlyInAnyOrder(".p.name", ".p.displayName", ".p.compounds", ".p.properties");
    }

    private static Product validProduct() {
        return new Product(
                ProductName.of(RandomValue.string()),
                RandomValue.string(),
                Set.of(CompoundName.of(RandomValue.string())),
                Map.of());
    }
}
