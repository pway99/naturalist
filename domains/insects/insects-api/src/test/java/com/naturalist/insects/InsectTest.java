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
        Insect insect = Insect.empty().withOrder(orderAggregate());

        assertThat(insect.identifiedTo()).contains(orderName());
    }

    @Test
    void identifiedToReturnsFamilyNameWhenIdentifiedToFamily() {
        Insect insect = Insect.empty()
                .withOrder(orderAggregate())
                .withFamily(familyAggregate());

        assertThat(insect.identifiedTo()).contains(familyName());
    }

    @Test
    void identifiedToReturnsGenusNameWhenIdentifiedToGenus() {
        Insect insect = Insect.empty()
                .withOrder(orderAggregate())
                .withFamily(familyAggregate())
                .withGenus(genusAggregate());

        assertThat(insect.identifiedTo()).contains(genusName());
    }

    @Test
    void identifiedToReturnsSpeciesNameWhenIdentifiedToSpecies() {
        Insect insect = Insect.empty()
                .withOrder(orderAggregate())
                .withFamily(familyAggregate())
                .withGenus(genusAggregate())
                .withSpecies(speciesAggregate());

        assertThat(insect.identifiedTo()).contains(speciesName());
    }

    // ----- with* mutators (field assignment, not validity) -----

    @Test
    void withObservationsReplacesObservationsAndPreservesOtherFields() {
        Insect base = Insect.empty().withOrder(orderAggregate());
        ImageCollection observations = ImageCollection.empty();

        Insect updated = base.withObservations(observations);

        assertThat(updated.observations()).isSameAs(observations);
        assertThat(updated.order()).isSameAs(base.order());
        assertThat(updated.lifeStages()).isSameAs(base.lifeStages());
    }

    @Test
    void withOrderReplacesOrderAndPreservesOtherFields() {
        Insect base = Insect.empty();
        InsectOrderAggregate orderAgg = orderAggregate();

        Insect updated = base.withOrder(orderAgg);

        assertThat(updated.order()).isSameAs(orderAgg);
        assertThat(updated.observations()).isSameAs(base.observations());
        assertThat(updated.lifeStages()).isSameAs(base.lifeStages());
    }

    @Test
    void withFamilyReplacesFamilyAndPreservesOtherFields() {
        Insect base = Insect.empty().withOrder(orderAggregate());
        InsectFamilyAggregate familyAgg = familyAggregate();

        Insect updated = base.withFamily(familyAgg);

        assertThat(updated.family()).isSameAs(familyAgg);
        assertThat(updated.order()).isSameAs(base.order());
    }

    @Test
    void withGenusReplacesGenusAndPreservesOtherFields() {
        Insect base = Insect.empty()
                .withOrder(orderAggregate())
                .withFamily(familyAggregate());
        InsectGenusAggregate genusAgg = genusAggregate();

        Insect updated = base.withGenus(genusAgg);

        assertThat(updated.genus()).isSameAs(genusAgg);
        assertThat(updated.family()).isSameAs(base.family());
    }

    @Test
    void withSpeciesReplacesSpeciesAndPreservesOtherFields() {
        Insect base = Insect.empty()
                .withOrder(orderAggregate())
                .withFamily(familyAggregate())
                .withGenus(genusAggregate());
        InsectSpeciesAggregate speciesAgg = speciesAggregate();

        Insect updated = base.withSpecies(speciesAgg);

        assertThat(updated.species()).isSameAs(speciesAgg);
        assertThat(updated.genus()).isSameAs(base.genus());
    }

    @Test
    void withLifeStagesReplacesLifeStagesAndPreservesOtherFields() {
        Insect base = Insect.empty().withOrder(orderAggregate());
        LifeStageCollection lifeStages = LifeStageCollection.empty();

        Insect updated = base.withLifeStages(lifeStages);

        assertThat(updated.lifeStages()).isSameAs(lifeStages);
        assertThat(updated.order()).isSameAs(base.order());
    }

    @Test
    void withMutatorsReturnNewInstances() {
        Insect base = Insect.empty();

        assertThat(base.withOrder(orderAggregate())).isNotSameAs(base);
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
                .withOrder(orderAggregate())
                .withFamily(familyAggregate())
                .withGenus(genusAggregate())
                .withSpecies(speciesAggregate());

        InvariantObservation result = mo.observable(insect, "insect");

        assertThat(result.violations()).isEmpty();
    }

    @Test
    void familyRootedAggregateIsValid() {
        var mo = observer.forMethod("familyRootedAggregateIsValid");
        Insect insect = Insect.empty()
                .withOrder(orderAggregate())
                .withFamily(familyAggregate());

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
    void speciesPresentWithoutGenusReportsGenusViolation() {
        var mo = observer.forMethod("speciesPresentWithoutGenusReportsGenusViolation");
        Insect insect = new Insect(
                ImageCollection.empty(),
                orderAggregate(),
                familyAggregate(),
                null,
                speciesAggregate(),
                LifeStageCollection.empty());

        InvariantObservation result = mo.observable(insect, "insect");

        assertThat(result.violationNamesRemovingPrefix(mo.observationPoint()))
                .contains(".insect.genus");
    }

    @Test
    void genusPresentWithoutFamilyReportsFamilyViolation() {
        var mo = observer.forMethod("genusPresentWithoutFamilyReportsFamilyViolation");
        Insect insect = new Insect(
                ImageCollection.empty(),
                orderAggregate(),
                null,
                genusAggregate(),
                null,
                LifeStageCollection.empty());

        InvariantObservation result = mo.observable(insect, "insect");

        assertThat(result.violationNamesRemovingPrefix(mo.observationPoint()))
                .contains(".insect.family");
    }

    @Test
    void familyPresentWithoutOrderReportsOrderViolation() {
        var mo = observer.forMethod("familyPresentWithoutOrderReportsOrderViolation");
        Insect insect = new Insect(
                ImageCollection.empty(),
                null,
                familyAggregate(),
                null,
                null,
                LifeStageCollection.empty());

        InvariantObservation result = mo.observable(insect, "insect");

        assertThat(result.violationNamesRemovingPrefix(mo.observationPoint()))
                .contains(".insect.order");
    }

    @Test
    void withSpeciesOnOrderOnlyAggregateConstructsButReportsGenusViolation() {
        var mo = observer.forMethod("withSpeciesOnOrderOnlyAggregateConstructsButReportsGenusViolation");
        Insect orderOnly = Insect.empty().withOrder(orderAggregate());

        Insect attempted = orderOnly.withSpecies(speciesAggregate());

        InvariantObservation result = mo.observable(attempted, "attempted");
        assertThat(result.violationNamesRemovingPrefix(mo.observationPoint()))
                .contains(".attempted.genus");
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

    private static InsectOrderAggregate orderAggregate() {
        return InsectOrderAggregate.of(new InsectOrder(
                orderName(),
                TaxonomicOrder.of("Lepidoptera"),
                description(),
                Set.of(),
                null));
    }

    private static InsectFamilyAggregate familyAggregate() {
        return InsectFamilyAggregate.of(new InsectFamily(
                familyName(),
                orderName(),
                TaxonomicFamily.of("Papilionidae"),
                description(),
                Set.of(),
                null));
    }

    private static InsectGenusAggregate genusAggregate() {
        return InsectGenusAggregate.of(new InsectGenus(
                genusName(),
                familyName(),
                orderName(),
                TaxonomicGenus.of("Battus"),
                description(),
                Set.of(),
                null));
    }

    private static InsectSpeciesAggregate speciesAggregate() {
        return InsectSpeciesAggregate.of(new InsectSpecies(
                speciesName(),
                genusName(),
                familyName(),
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
