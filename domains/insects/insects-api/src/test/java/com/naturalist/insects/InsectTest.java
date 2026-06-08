package com.naturalist.insects;

import com.naturalist.RandomValue;
import com.naturalist.fieldnotes.Description;
import com.naturalist.insects.InsectEntityCollections.ImageCollection;
import com.naturalist.insects.lifestage.InsectLifeStageEntityCollections.LifeStageCollection;
import com.naturalist.observability.InvariantObservation;
import com.naturalist.observability.Observer;
import com.naturalist.taxonomy.TaxonomicFamily;
import com.naturalist.taxonomy.TaxonomicGenus;
import com.naturalist.taxonomy.TaxonomicOrder;
import com.naturalist.taxonomy.TaxonomicSpecies;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class InsectTest {

    private static final Observer observer = Observer.forClass(InsectTest.class);

    // ----- Insect.empty() / shape -----

    @Test
    void emptyAggregateHasNoObservationsNoRanksAndNoLifeStages() {
        Insect insect = Insect.empty();

        assertThat(insect.observations().stream()).isEmpty();
        assertThat(insect.order()).isNull();
        assertThat(insect.family()).isNull();
        assertThat(insect.genus()).isNull();
        assertThat(insect.species()).isNull();
        assertThat(insect.lifeStages().stream()).isEmpty();
    }

    // ----- identifiedTo() -----

    @Test
    void identifiedToReturnsEmptyForZeroStateAggregate() {
        assertThat(Insect.empty().identifiedTo()).isEmpty();
    }

    @Test
    void identifiedToReturnsOrderNameWhenOnlyOrderSet() {
        Insect insect = Insect.empty().withOrder(orderView());

        assertThat(insect.identifiedTo()).contains(orderName());
    }

    @Test
    void identifiedToReturnsFamilyNameWhenIdentifiedToFamily() {
        Insect insect = Insect.empty()
                .withOrder(orderView())
                .withFamily(familyView());

        assertThat(insect.identifiedTo()).contains(familyName());
    }

    @Test
    void identifiedToReturnsGenusNameWhenIdentifiedToGenus() {
        Insect insect = Insect.empty()
                .withOrder(orderView())
                .withFamily(familyView())
                .withGenus(genusView());

        assertThat(insect.identifiedTo()).contains(genusName());
    }

    @Test
    void identifiedToReturnsSpeciesNameWhenIdentifiedToSpecies() {
        Insect insect = Insect.empty()
                .withOrder(orderView())
                .withFamily(familyView())
                .withGenus(genusView())
                .withSpecies(speciesView());

        assertThat(insect.identifiedTo()).contains(speciesName());
    }

    // ----- with* mutators (field assignment, not validity) -----

    @Test
    void withObservationsReplacesObservationsAndPreservesOtherFields() {
        Insect base = Insect.empty().withOrder(orderView());
        ImageCollection observations = ImageCollection.empty();

        Insect updated = base.withObservations(observations);

        assertThat(updated.observations()).isSameAs(observations);
        assertThat(updated.order()).isSameAs(base.order());
        assertThat(updated.lifeStages()).isSameAs(base.lifeStages());
    }

    @Test
    void withOrderReplacesOrderAndPreservesOtherFields() {
        Insect base = Insect.empty();
        InsectOrderView orderView = orderView();

        Insect updated = base.withOrder(orderView);

        assertThat(updated.order()).isSameAs(orderView);
        assertThat(updated.observations()).isSameAs(base.observations());
        assertThat(updated.lifeStages()).isSameAs(base.lifeStages());
    }

    @Test
    void withFamilyReplacesFamilyAndPreservesOtherFields() {
        Insect base = Insect.empty().withOrder(orderView());
        InsectFamilyView familyView = familyView();

        Insect updated = base.withFamily(familyView);

        assertThat(updated.family()).isSameAs(familyView);
        assertThat(updated.order()).isSameAs(base.order());
    }

    @Test
    void withGenusReplacesGenusAndPreservesOtherFields() {
        Insect base = Insect.empty()
                .withOrder(orderView())
                .withFamily(familyView());
        InsectGenusView genusView = genusView();

        Insect updated = base.withGenus(genusView);

        assertThat(updated.genus()).isSameAs(genusView);
        assertThat(updated.family()).isSameAs(base.family());
    }

    @Test
    void withSpeciesReplacesSpeciesAndPreservesOtherFields() {
        Insect base = Insect.empty()
                .withOrder(orderView())
                .withFamily(familyView())
                .withGenus(genusView());
        InsectSpeciesView speciesView = speciesView();

        Insect updated = base.withSpecies(speciesView);

        assertThat(updated.species()).isSameAs(speciesView);
        assertThat(updated.genus()).isSameAs(base.genus());
    }

    @Test
    void withLifeStagesReplacesLifeStagesAndPreservesOtherFields() {
        Insect base = Insect.empty().withOrder(orderView());
        LifeStageCollection lifeStages = LifeStageCollection.empty();

        Insect updated = base.withLifeStages(lifeStages);

        assertThat(updated.lifeStages()).isSameAs(lifeStages);
        assertThat(updated.order()).isSameAs(base.order());
    }

    @Test
    void withMutatorsReturnNewInstances() {
        Insect base = Insect.empty();

        assertThat(base.withOrder(orderView())).isNotSameAs(base);
    }

    // ----- invariants() valid cases -----

    @Test
    void emptyAggregateIsValid() {
        var mo = observer.forMethod("emptyAggregateIsValid");
        Insect insect = Insect.empty();

        InvariantObservation result = mo.observable(insect, "insect");

        assertThat(result.violations()).isEmpty();
    }

    @Test
    void fullyIdentifiedAggregateIsValid() {
        var mo = observer.forMethod("fullyIdentifiedAggregateIsValid");
        Insect insect = Insect.empty()
                .withOrder(orderView())
                .withFamily(familyView())
                .withGenus(genusView())
                .withSpecies(speciesView());

        InvariantObservation result = mo.observable(insect, "insect");

        assertThat(result.violations()).isEmpty();
    }

    @Test
    void familyRootedAggregateIsValid() {
        var mo = observer.forMethod("familyRootedAggregateIsValid");
        Insect insect = Insect.empty()
                .withOrder(orderView())
                .withFamily(familyView());

        InvariantObservation result = mo.observable(insect, "insect");

        assertThat(result.violations()).isEmpty();
    }

    // ----- invariants() violation cases -----

    @Test
    void nullObservationsReportsObservationsViolation() {
        var mo = observer.forMethod("nullObservationsReportsObservationsViolation");
        Insect insect = new Insect(
                null,
                null, null, null, null,
                LifeStageCollection.empty());

        InvariantObservation result = mo.observable(insect, "insect");

        assertThat(result.violationNamesRemovingPrefix(mo.observationPoint()))
                .contains(".insect.observations");
    }

    @Test
    void nullLifeStagesReportsLifeStagesViolation() {
        var mo = observer.forMethod("nullLifeStagesReportsLifeStagesViolation");
        Insect insect = new Insect(
                ImageCollection.empty(),
                null, null, null, null,
                null);

        InvariantObservation result = mo.observable(insect, "insect");

        assertThat(result.violationNamesRemovingPrefix(mo.observationPoint()))
                .contains(".insect.lifeStages");
    }

    @Test
    void speciesPresentWithoutGenusReportsSpeciesGenusViolation() {
        var mo = observer.forMethod("speciesPresentWithoutGenusReportsSpeciesGenusViolation");
        Insect insect = new Insect(
                ImageCollection.empty(),
                orderView(),
                familyView(),
                null,
                speciesView(),
                LifeStageCollection.empty());

        InvariantObservation result = mo.observable(insect, "insect");

        assertThat(result.violationNamesRemovingPrefix(mo.observationPoint()))
                .contains(".insect.species:genus");
    }

    @Test
    void genusPresentWithoutFamilyReportsGenusFamilyViolation() {
        var mo = observer.forMethod("genusPresentWithoutFamilyReportsGenusFamilyViolation");
        Insect insect = new Insect(
                ImageCollection.empty(),
                orderView(),
                null,
                genusView(),
                null,
                LifeStageCollection.empty());

        InvariantObservation result = mo.observable(insect, "insect");

        assertThat(result.violationNamesRemovingPrefix(mo.observationPoint()))
                .contains(".insect.genus:family");
    }

    @Test
    void familyPresentWithoutOrderReportsFamilyOrderViolation() {
        var mo = observer.forMethod("familyPresentWithoutOrderReportsFamilyOrderViolation");
        Insect insect = new Insect(
                ImageCollection.empty(),
                null,
                familyView(),
                null,
                null,
                LifeStageCollection.empty());

        InvariantObservation result = mo.observable(insect, "insect");

        assertThat(result.violationNamesRemovingPrefix(mo.observationPoint()))
                .contains(".insect.family:order");
    }

    @Test
    void withSpeciesOnOrderOnlyAggregateConstructsButReportsAncestorViolations() {
        var mo = observer.forMethod("withSpeciesOnOrderOnlyAggregateConstructsButReportsAncestorViolations");
        Insect orderOnly = Insect.empty().withOrder(orderView());

        Insect attempted = orderOnly.withSpecies(speciesView());

        InvariantObservation result = mo.observable(attempted, "attempted");
        assertThat(result.violationNamesRemovingPrefix(mo.observationPoint()))
                .contains(".attempted.species:genus");
    }

    // ----- invariants() cross-rank FK consistency -----

    @Test
    void familyWithMismatchedOrderFkReportsFamilyBelongsToOrderViolation() {
        var mo = observer.forMethod("familyWithMismatchedOrderFkReportsFamilyBelongsToOrderViolation");
        InsectFamilyView mismatched = InsectFamilyView.of(new InsectFamily(
                familyName(),
                InsectOrderName.of("diptera"),
                TaxonomicFamily.of("Papilionidae"),
                description(),
                Set.of(),
                null));
        Insect insect = Insect.empty()
                .withOrder(orderView())
                .withFamily(mismatched);

        InvariantObservation result = mo.observable(insect, "insect");

        assertThat(result.violationNamesRemovingPrefix(mo.observationPoint()))
                .contains(".insect.familyBelongsToOrder");
    }

    @Test
    void genusWithMismatchedFamilyFkReportsGenusBelongsToFamilyViolation() {
        var mo = observer.forMethod("genusWithMismatchedFamilyFkReportsGenusBelongsToFamilyViolation");
        InsectGenusView mismatched = InsectGenusView.of(new InsectGenus(
                genusName(),
                InsectFamilyName.of("syrphidae"),
                TaxonomicGenus.of("Battus"),
                description(),
                Set.of(),
                null));
        Insect insect = Insect.empty()
                .withOrder(orderView())
                .withFamily(familyView())
                .withGenus(mismatched);

        InvariantObservation result = mo.observable(insect, "insect");

        assertThat(result.violationNamesRemovingPrefix(mo.observationPoint()))
                .contains(".insect.genusBelongsToFamily");
    }

    @Test
    void speciesWithMismatchedGenusFkReportsSpeciesBelongsToGenusViolation() {
        var mo = observer.forMethod("speciesWithMismatchedGenusFkReportsSpeciesBelongsToGenusViolation");
        InsectSpeciesView mismatched = InsectSpeciesView.of(new InsectSpecies(
                speciesName(),
                InsectGenusName.of("empoasca"),
                TaxonomicSpecies.of("philenor"),
                description(),
                Set.of(),
                null, null,
                null,
                null, null, null, null, null, null, null));
        Insect insect = Insect.empty()
                .withOrder(orderView())
                .withFamily(familyView())
                .withGenus(genusView())
                .withSpecies(mismatched);

        InvariantObservation result = mo.observable(insect, "insect");

        assertThat(result.violationNamesRemovingPrefix(mo.observationPoint()))
                .contains(".insect.speciesBelongsToGenus");
    }

    // ----- helpers -----

    private static InsectOrderName orderName() {
        return InsectOrderName.of("lepidoptera");
    }

    private static InsectFamilyName familyName() {
        return InsectFamilyName.of("papilionidae");
    }

    private static InsectGenusName genusName() {
        return InsectGenusName.of("battus");
    }

    private static InsectSpeciesName speciesName() {
        return InsectSpeciesName.of("battus-philenor");
    }

    private static InsectOrderView orderView() {
        return InsectOrderView.of(new InsectOrder(
                orderName(),
                TaxonomicOrder.of("Lepidoptera"),
                description(),
                Set.of(),
                null));
    }

    private static InsectFamilyView familyView() {
        return InsectFamilyView.of(new InsectFamily(
                familyName(),
                orderName(),
                TaxonomicFamily.of("Papilionidae"),
                description(),
                Set.of(),
                null));
    }

    private static InsectGenusView genusView() {
        return InsectGenusView.of(new InsectGenus(
                genusName(),
                familyName(),
                TaxonomicGenus.of("Battus"),
                description(),
                Set.of(),
                null));
    }

    private static InsectSpeciesView speciesView() {
        return InsectSpeciesView.of(new InsectSpecies(
                speciesName(),
                genusName(),
                TaxonomicSpecies.of("philenor"),
                description(),
                Set.of(),
                null, null,
                null,
                null, null, null, null, null, null, null));
    }

    private static Description description() {
        return new Description(
                RandomValue.string(), RandomValue.string(),
                RandomValue.string(), RandomValue.string());
    }
}
