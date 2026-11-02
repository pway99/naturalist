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

class InsectTaxonTest {
    private static final Observer observer = Observer.forClass(InsectTaxonTest.class);

    @Test
    void speciesIsValid() {
        var mo = observer.forMethod("speciesIsValid");
        InsectSpecies taxon = InsectSpecies.of(validSpecies());

        InvariantObservation result = mo.observable(taxon, "agg");

        assertThat(result.violations()).isEmpty();
        assertThat(taxon.name()).isEqualTo(taxon.info().name());
    }

    @Test
    void speciesIsNotValid() {
        var mo = observer.forMethod("speciesIsNotValid");
        InsectSpecies taxon = new InsectSpecies(null, null);

        InvariantObservation result = mo.observable(taxon, "agg");

        assertThat(result.violationNamesRemovingPrefix(mo.observationPoint()))
                .containsExactlyInAnyOrder(".agg.info", ".agg.images");
    }

    @Test
    void genusIsValid() {
        var mo = observer.forMethod("genusIsValid");
        InsectGenus taxon = InsectGenus.of(validGenus());

        InvariantObservation result = mo.observable(taxon, "agg");

        assertThat(result.violations()).isEmpty();
        assertThat(taxon.name()).isEqualTo(taxon.info().name());
    }

    @Test
    void genusIsNotValid() {
        var mo = observer.forMethod("genusIsNotValid");
        InsectGenus taxon = new InsectGenus(null, null);

        InvariantObservation result = mo.observable(taxon, "agg");

        assertThat(result.violationNamesRemovingPrefix(mo.observationPoint()))
                .containsExactlyInAnyOrder(".agg.info", ".agg.images");
    }

    @Test
    void familyIsValid() {
        var mo = observer.forMethod("familyIsValid");
        InsectFamily taxon = InsectFamily.of(validFamily());

        InvariantObservation result = mo.observable(taxon, "agg");

        assertThat(result.violations()).isEmpty();
        assertThat(taxon.name()).isEqualTo(taxon.info().name());
    }

    @Test
    void familyIsNotValid() {
        var mo = observer.forMethod("familyIsNotValid");
        InsectFamily taxon = new InsectFamily(null, null);

        InvariantObservation result = mo.observable(taxon, "agg");

        assertThat(result.violationNamesRemovingPrefix(mo.observationPoint()))
                .containsExactlyInAnyOrder(".agg.info", ".agg.images");
    }

    @Test
    void orderIsValid() {
        var mo = observer.forMethod("orderIsValid");
        InsectOrder taxon = InsectOrder.of(validOrder());

        InvariantObservation result = mo.observable(taxon, "agg");

        assertThat(result.violations()).isEmpty();
    }

    @Test
    void orderIsNotValid() {
        var mo = observer.forMethod("orderIsNotValid");
        InsectOrder taxon = new InsectOrder(null, null);

        InvariantObservation result = mo.observable(taxon, "agg");

        assertThat(result.violationNamesRemovingPrefix(mo.observationPoint()))
                .containsExactlyInAnyOrder(".agg.info", ".agg.images");
    }

    @Test
    void switchOverEveryPermitCompilesWithoutDefault() {
        InsectTaxon taxon = InsectOrder.of(validOrder());
        String rank = switch (taxon) {
            case InsectSpecies s -> "species";
            case InsectGenus g -> "genus";
            case InsectFamily f -> "family";
            case InsectOrder o -> "order";
        };
        assertThat(rank).isEqualTo("order");
    }

    private static InsectSpeciesInfo validSpecies() {
        return new InsectSpeciesInfo(
                InsectSpeciesName.of(RandomValue.string()),
                InsectGenusName.of("hippodamia"),
                TaxonomicSpecies.of("convergens"),
                description(),
                Set.of(),
                null,
                null,
                null, null, null, null, null, null, null
        );
    }

    private static InsectGenusInfo validGenus() {
        return new InsectGenusInfo(
                InsectGenusName.of(RandomValue.string()),
                InsectFamilyName.of("papilionidae"),
                TaxonomicGenus.of("Battus"),
                description(),
                Set.of(),
                null);
    }

    private static InsectFamilyInfo validFamily() {
        return new InsectFamilyInfo(
                InsectFamilyName.of(RandomValue.string()),
                InsectOrderName.of("diptera"),
                TaxonomicFamily.of("Syrphidae"),
                description(),
                Set.of(),
                null);
    }

    private static InsectOrderInfo validOrder() {
        return new InsectOrderInfo(
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
