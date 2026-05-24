package com.naturalist.insects;

import com.naturalist.RandomValue;
import com.naturalist.fieldnotes.Description;
import com.naturalist.observability.InvariantObservation;
import com.naturalist.observability.Observer;
import com.naturalist.taxonomy.*;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class InsectAggregateTest {
    private static final Observer observer = Observer.forClass(InsectAggregateTest.class);

    @Test
    void aggregateIsValid() {
        var mo = observer.forMethod("aggregateIsValid");
        InsectAggregate agg = InsectAggregate.of(validSpecies());

        InvariantObservation result = mo.observable(agg, "agg");

        assertThat(result.violations()).isEmpty();
    }

    @Test
    void aggregateIsNotValid() {
        var mo = observer.forMethod("aggregateIsNotValid");
        InsectAggregate agg = new InsectAggregate(null, null);

        InvariantObservation result = mo.observable(agg, "agg");

        assertThat(result.violationNamesRemovingPrefix(mo.observationPoint()))
                .containsExactlyInAnyOrder(".agg.species", ".agg.images");
    }

    private static InsectSpecies validSpecies() {
        return new InsectSpecies(
                InsectSpeciesName.of(RandomValue.string()),
                new TaxonomicClassification(
                        TaxonomicOrder.of("Coleoptera"),
                        TaxonomicFamily.of("Coccinellidae"),
                        TaxonomicGenus.of("Hippodamia"),
                        TaxonomicSpecies.of("convergens")),
                null, null,
                new Description(
                        RandomValue.string(),
                        RandomValue.string(),
                        RandomValue.string(),
                        RandomValue.string()),
                Set.of(),
                null, null,
                null,
                null, null, null, null,
                null, null, null, null, null, null, null
        );
    }
}
