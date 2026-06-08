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

class InsectTaxonViewTest {
    private static final Observer observer = Observer.forClass(InsectTaxonViewTest.class);

    @Test
    void speciesViewIsValid() {
        var mo = observer.forMethod("speciesViewIsValid");
        InsectSpeciesView view = InsectSpeciesView.of(validSpecies());

        InvariantObservation result = mo.observable(view, "agg");

        assertThat(result.violations()).isEmpty();
        assertThat(view.name()).isEqualTo(view.species().name());
    }

    @Test
    void speciesViewIsNotValid() {
        var mo = observer.forMethod("speciesViewIsNotValid");
        InsectSpeciesView view = new InsectSpeciesView(null, null);

        InvariantObservation result = mo.observable(view, "agg");

        assertThat(result.violationNamesRemovingPrefix(mo.observationPoint()))
                .containsExactlyInAnyOrder(".agg.species", ".agg.images");
    }

    @Test
    void genusViewIsValid() {
        var mo = observer.forMethod("genusViewIsValid");
        InsectGenusView view = InsectGenusView.of(validGenus());

        InvariantObservation result = mo.observable(view, "agg");

        assertThat(result.violations()).isEmpty();
        assertThat(view.name()).isEqualTo(view.genus().name());
    }

    @Test
    void genusViewIsNotValid() {
        var mo = observer.forMethod("genusViewIsNotValid");
        InsectGenusView view = new InsectGenusView(null, null);

        InvariantObservation result = mo.observable(view, "agg");

        assertThat(result.violationNamesRemovingPrefix(mo.observationPoint()))
                .containsExactlyInAnyOrder(".agg.genus", ".agg.images");
    }

    @Test
    void familyViewIsValid() {
        var mo = observer.forMethod("familyViewIsValid");
        InsectFamilyView view = InsectFamilyView.of(validFamily());

        InvariantObservation result = mo.observable(view, "agg");

        assertThat(result.violations()).isEmpty();
        assertThat(view.name()).isEqualTo(view.family().name());
    }

    @Test
    void familyViewIsNotValid() {
        var mo = observer.forMethod("familyViewIsNotValid");
        InsectFamilyView view = new InsectFamilyView(null, null);

        InvariantObservation result = mo.observable(view, "agg");

        assertThat(result.violationNamesRemovingPrefix(mo.observationPoint()))
                .containsExactlyInAnyOrder(".agg.family", ".agg.images");
    }

    @Test
    void orderViewIsValid() {
        var mo = observer.forMethod("orderViewIsValid");
        InsectOrderView view = InsectOrderView.of(validOrder());

        InvariantObservation result = mo.observable(view, "agg");

        assertThat(result.violations()).isEmpty();
    }

    @Test
    void orderViewIsNotValid() {
        var mo = observer.forMethod("orderViewIsNotValid");
        InsectOrderView view = new InsectOrderView(null, null);

        InvariantObservation result = mo.observable(view, "agg");

        assertThat(result.violationNamesRemovingPrefix(mo.observationPoint()))
                .containsExactlyInAnyOrder(".agg.order", ".agg.images");
    }

    private static InsectSpecies validSpecies() {
        return new InsectSpecies(
                InsectSpeciesName.of(RandomValue.string()),
                InsectGenusName.of("hippodamia"),
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
