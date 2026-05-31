package com.naturalist.insects;

import com.naturalist.RandomValue;
import com.naturalist.fieldnotes.Description;
import com.naturalist.observability.InvariantObservation;
import com.naturalist.observability.Observer;
import com.naturalist.taxonomy.TaxonomicFamily;
import com.naturalist.taxonomy.TaxonomicGenus;
import com.naturalist.taxonomy.TaxonomicOrder;
import com.naturalist.taxonomy.TaxonomicSpecies;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class InsectAggregateTest {
    private static final Observer observer = Observer.forClass(InsectAggregateTest.class);

    @Test
    void speciesAggregateIsValid() {
        var mo = observer.forMethod("speciesAggregateIsValid");
        InsectSpeciesAggregate agg = InsectSpeciesAggregate.of(validSpecies());

        InvariantObservation result = mo.observable(agg, "agg");

        assertThat(result.violations()).isEmpty();
        assertThat(agg.name()).isEqualTo(agg.species().name());
    }

    @Test
    void speciesAggregateIsNotValid() {
        var mo = observer.forMethod("speciesAggregateIsNotValid");
        InsectSpeciesAggregate agg = new InsectSpeciesAggregate(null, null);

        InvariantObservation result = mo.observable(agg, "agg");

        assertThat(result.violationNamesRemovingPrefix(mo.observationPoint()))
                .containsExactlyInAnyOrder(".agg.species", ".agg.images");
    }

    @Test
    void genusAggregateIsValid() {
        var mo = observer.forMethod("genusAggregateIsValid");
        InsectGenusAggregate agg = InsectGenusAggregate.of(validGenus());

        InvariantObservation result = mo.observable(agg, "agg");

        assertThat(result.violations()).isEmpty();
        assertThat(agg.name()).isEqualTo(agg.genus().name());
    }

    @Test
    void genusAggregateIsNotValid() {
        var mo = observer.forMethod("genusAggregateIsNotValid");
        InsectGenusAggregate agg = new InsectGenusAggregate(null, null);

        InvariantObservation result = mo.observable(agg, "agg");

        assertThat(result.violationNamesRemovingPrefix(mo.observationPoint()))
                .containsExactlyInAnyOrder(".agg.genus", ".agg.images");
    }

    @Test
    void familyAggregateIsValid() {
        var mo = observer.forMethod("familyAggregateIsValid");
        InsectFamilyAggregate agg = InsectFamilyAggregate.of(validFamily());

        InvariantObservation result = mo.observable(agg, "agg");

        assertThat(result.violations()).isEmpty();
        assertThat(agg.name()).isEqualTo(agg.family().name());
    }

    @Test
    void familyAggregateIsNotValid() {
        var mo = observer.forMethod("familyAggregateIsNotValid");
        InsectFamilyAggregate agg = new InsectFamilyAggregate(null, null);

        InvariantObservation result = mo.observable(agg, "agg");

        assertThat(result.violationNamesRemovingPrefix(mo.observationPoint()))
                .containsExactlyInAnyOrder(".agg.family", ".agg.images");
    }

    @Test
    void orderAggregateIsValid() {
        var mo = observer.forMethod("orderAggregateIsValid");
        InsectOrderAggregate agg = InsectOrderAggregate.of(validOrder());

        InvariantObservation result = mo.observable(agg, "agg");

        assertThat(result.violations()).isEmpty();
    }

    @Test
    void orderAggregateIsNotValid() {
        var mo = observer.forMethod("orderAggregateIsNotValid");
        InsectOrderAggregate agg = new InsectOrderAggregate(null, null);

        InvariantObservation result = mo.observable(agg, "agg");

        assertThat(result.violationNamesRemovingPrefix(mo.observationPoint()))
                .containsExactlyInAnyOrder(".agg.order", ".agg.images");
    }

    private static InsectSpecies validSpecies() {
        return new InsectSpecies(
                InsectSpeciesName.of(RandomValue.string()),
                InsectGenusName.of("hippodamia"),
                InsectFamilyName.of("coccinellidae"),
                TaxonomicSpecies.of("convergens"),
                description(),
                Set.of(),
                null, null,
                null,
                null, null, null, null, null, null, null
        );
    }

    private static InsectGenus validGenus() {
        return new InsectGenus(
                InsectGenusName.of(RandomValue.string()),
                InsectFamilyName.of("papilionidae"),
                InsectOrderName.of("lepidoptera"),
                TaxonomicGenus.of("Battus"),
                description(),
                Set.of(),
                null);
    }

    private static InsectFamily validFamily() {
        return new InsectFamily(
                InsectFamilyName.of(RandomValue.string()),
                InsectOrderName.of("diptera"),
                TaxonomicFamily.of("Syrphidae"),
                description(),
                Set.of(),
                null);
    }

    private static InsectOrder validOrder() {
        return new InsectOrder(
                InsectOrderName.of(RandomValue.string()),
                TaxonomicOrder.of("Diptera"),
                description(),
                Set.of(),
                null);
    }

    private static Description description() {
        return new Description(
                RandomValue.string(),
                RandomValue.string(),
                RandomValue.string(),
                RandomValue.string());
    }
}
