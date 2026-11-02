package com.naturalist.insects;

import com.naturalist.RandomValue;
import com.naturalist.authority.AuthorityReference;
import com.naturalist.authority.AuthoritySource;
import com.naturalist.authority.CitationName;
import com.naturalist.authority.OnlineSource;
import com.naturalist.fieldnotes.Description;
import com.naturalist.insects.InsectEntityCollections.ImageCollection;
import com.naturalist.insects.lifestage.InsectLifeStageEntityCollections.LifeStageCollection;
import com.naturalist.observability.InvariantObservation;
import com.naturalist.observability.Observer;
import com.naturalist.taxonomy.OrganismFeatureView;
import com.naturalist.taxonomy.TaxonomicFamily;
import com.naturalist.taxonomy.TaxonomicGenus;
import com.naturalist.taxonomy.TaxonomicOrder;
import com.naturalist.taxonomy.TaxonomicSpecies;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.List;
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
        assertThat(insect.citations()).isNull();
        assertThat(insect.features()).isNull();
        assertThat(insect.role()).isNull();
        assertThat(insect.children()).isEmpty();
    }

    // ----- identifiedTo() -----

    @Test
    void identifiedToReturnsEmptyForZeroStateAggregate() {
        assertThat(Insect.empty().identifiedTo()).isEmpty();
    }

    @Test
    void identifiedToReturnsOrderNameWhenOnlyOrderSet() {
        Insect insect = Insect.empty().withOrder(order());

        assertThat(insect.identifiedTo()).contains(orderName());
    }

    @Test
    void identifiedToReturnsFamilyNameWhenIdentifiedToFamily() {
        Insect insect = Insect.empty()
                .withOrder(order())
                .withFamily(family());

        assertThat(insect.identifiedTo()).contains(familyName());
    }

    @Test
    void identifiedToReturnsGenusNameWhenIdentifiedToGenus() {
        Insect insect = Insect.empty()
                .withOrder(order())
                .withFamily(family())
                .withGenus(genus());

        assertThat(insect.identifiedTo()).contains(genusName());
    }

    @Test
    void identifiedToReturnsSpeciesNameWhenIdentifiedToSpecies() {
        Insect insect = Insect.empty()
                .withOrder(order())
                .withFamily(family())
                .withGenus(genus())
                .withSpecies(species());

        assertThat(insect.identifiedTo()).contains(speciesName());
    }

    // ----- with* mutators (field assignment, not validity) -----

    @Test
    void withObservationsReplacesObservationsAndPreservesOtherFields() {
        Insect base = Insect.empty().withOrder(order());
        ImageCollection observations = ImageCollection.empty();

        Insect updated = base.withObservations(observations);

        assertThat(updated.observations()).isSameAs(observations);
        assertThat(updated.order()).isSameAs(base.order());
        assertThat(updated.lifeStages()).isSameAs(base.lifeStages());
    }

    @Test
    void withOrderReplacesOrderAndPreservesOtherFields() {
        Insect base = Insect.empty();
        InsectOrder order = order();

        Insect updated = base.withOrder(order);

        assertThat(updated.order()).isSameAs(order);
        assertThat(updated.observations()).isSameAs(base.observations());
        assertThat(updated.lifeStages()).isSameAs(base.lifeStages());
    }

    @Test
    void withFamilyReplacesFamilyAndPreservesOtherFields() {
        Insect base = Insect.empty().withOrder(order());
        InsectFamily family = family();

        Insect updated = base.withFamily(family);

        assertThat(updated.family()).isSameAs(family);
        assertThat(updated.order()).isSameAs(base.order());
    }

    @Test
    void withGenusReplacesGenusAndPreservesOtherFields() {
        Insect base = Insect.empty()
                .withOrder(order())
                .withFamily(family());
        InsectGenus genus = genus();

        Insect updated = base.withGenus(genus);

        assertThat(updated.genus()).isSameAs(genus);
        assertThat(updated.family()).isSameAs(base.family());
    }

    @Test
    void withSpeciesReplacesSpeciesAndPreservesOtherFields() {
        Insect base = Insect.empty()
                .withOrder(order())
                .withFamily(family())
                .withGenus(genus());
        InsectSpecies species = species();

        Insect updated = base.withSpecies(species);

        assertThat(updated.species()).isSameAs(species);
        assertThat(updated.genus()).isSameAs(base.genus());
    }

    @Test
    void withLifeStagesReplacesLifeStagesAndPreservesOtherFields() {
        Insect base = Insect.empty().withOrder(order());
        LifeStageCollection lifeStages = LifeStageCollection.empty();

        Insect updated = base.withLifeStages(lifeStages);

        assertThat(updated.lifeStages()).isSameAs(lifeStages);
        assertThat(updated.order()).isSameAs(base.order());
    }

    @Test
    void withCitationsReplacesCitationsAndPreservesOtherFields() {
        Insect base = Insect.empty().withOrder(order());
        InsectCitations citations = new InsectCitations(
                InsectOrderName.of("lepidoptera"),
                List.of(new InsectCitations.RankedCitation(
                        new OnlineSource(
                                CitationName.of("eol-lepidoptera"),
                                new AuthorityReference(
                                        new AuthoritySource("eol", "Encyclopedia of Life"),
                                        URI.create("https://eol.org/pages/747")),
                                "EOL: Lepidoptera", null, null, null),
                        InsectOrderName.of("lepidoptera"),
                        null)));

        Insect updated = base.withCitations(citations);

        assertThat(updated.citations()).isSameAs(citations);
        assertThat(updated.order()).isSameAs(base.order());
        assertThat(updated.observations()).isSameAs(base.observations());
    }

    @Test
    void withMutatorsReturnNewInstances() {
        Insect base = Insect.empty();

        assertThat(base.withOrder(order())).isNotSameAs(base);
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
                .withOrder(order())
                .withFamily(family())
                .withGenus(genus())
                .withSpecies(species());

        InvariantObservation result = mo.observable(insect, "insect");

        assertThat(result.violations()).isEmpty();
    }

    @Test
    void familyRootedAggregateIsValid() {
        var mo = observer.forMethod("familyRootedAggregateIsValid");
        Insect insect = Insect.empty()
                .withOrder(order())
                .withFamily(family());

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
                LifeStageCollection.empty(),
                null,
                null,
                null,
                List.of());

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
                null,
                null,
                null,
                null,
                List.of());

        InvariantObservation result = mo.observable(insect, "insect");

        assertThat(result.violationNamesRemovingPrefix(mo.observationPoint()))
                .contains(".insect.lifeStages");
    }

    @Test
    void speciesPresentWithoutGenusReportsSpeciesGenusViolation() {
        var mo = observer.forMethod("speciesPresentWithoutGenusReportsSpeciesGenusViolation");
        Insect insect = new Insect(
                ImageCollection.empty(),
                order(),
                family(),
                null,
                species(),
                LifeStageCollection.empty(),
                null,
                null,
                null,
                List.of());

        InvariantObservation result = mo.observable(insect, "insect");

        assertThat(result.violationNamesRemovingPrefix(mo.observationPoint()))
                .contains(".insect.species:genus");
    }

    @Test
    void genusPresentWithoutFamilyReportsGenusFamilyViolation() {
        var mo = observer.forMethod("genusPresentWithoutFamilyReportsGenusFamilyViolation");
        Insect insect = new Insect(
                ImageCollection.empty(),
                order(),
                null,
                genus(),
                null,
                LifeStageCollection.empty(),
                null,
                null,
                null,
                List.of());

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
                family(),
                null,
                null,
                LifeStageCollection.empty(),
                null,
                null,
                null,
                List.of());

        InvariantObservation result = mo.observable(insect, "insect");

        assertThat(result.violationNamesRemovingPrefix(mo.observationPoint()))
                .contains(".insect.family:order");
    }

    @Test
    void withSpeciesOnOrderOnlyAggregateConstructsButReportsAncestorViolations() {
        var mo = observer.forMethod("withSpeciesOnOrderOnlyAggregateConstructsButReportsAncestorViolations");
        Insect orderOnly = Insect.empty().withOrder(order());

        Insect attempted = orderOnly.withSpecies(species());

        InvariantObservation result = mo.observable(attempted, "attempted");
        assertThat(result.violationNamesRemovingPrefix(mo.observationPoint()))
                .contains(".attempted.species:genus");
    }

    // ----- invariants() cross-rank FK consistency -----

    @Test
    void familyWithMismatchedOrderFkReportsFamilyBelongsToOrderViolation() {
        var mo = observer.forMethod("familyWithMismatchedOrderFkReportsFamilyBelongsToOrderViolation");
        InsectFamily mismatched = InsectFamily.of(new InsectFamilyInfo(
                familyName(),
                InsectOrderName.of("diptera"),
                TaxonomicFamily.of("Papilionidae"),
                description(),
                Set.of(),
                null));
        Insect insect = Insect.empty()
                .withOrder(order())
                .withFamily(mismatched);

        InvariantObservation result = mo.observable(insect, "insect");

        assertThat(result.violationNamesRemovingPrefix(mo.observationPoint()))
                .contains(".insect.familyBelongsToOrder");
    }

    @Test
    void genusWithMismatchedFamilyFkReportsGenusBelongsToFamilyViolation() {
        var mo = observer.forMethod("genusWithMismatchedFamilyFkReportsGenusBelongsToFamilyViolation");
        InsectGenus mismatched = InsectGenus.of(new InsectGenusInfo(
                genusName(),
                InsectFamilyName.of("syrphidae"),
                TaxonomicGenus.of("Battus"),
                description(),
                Set.of(),
                null));
        Insect insect = Insect.empty()
                .withOrder(order())
                .withFamily(family())
                .withGenus(mismatched);

        InvariantObservation result = mo.observable(insect, "insect");

        assertThat(result.violationNamesRemovingPrefix(mo.observationPoint()))
                .contains(".insect.genusBelongsToFamily");
    }

    @Test
    void speciesWithMismatchedGenusFkReportsSpeciesBelongsToGenusViolation() {
        var mo = observer.forMethod("speciesWithMismatchedGenusFkReportsSpeciesBelongsToGenusViolation");
        InsectSpecies mismatched = InsectSpecies.of(new InsectSpeciesInfo(
                speciesName(),
                InsectGenusName.of("empoasca"),
                TaxonomicSpecies.of("philenor"),
                description(),
                Set.of(),
                null,
                null,
                null, null, null, null, null, null, null));
        Insect insect = Insect.empty()
                .withOrder(order())
                .withFamily(family())
                .withGenus(genus())
                .withSpecies(mismatched);

        InvariantObservation result = mo.observable(insect, "insect");

        assertThat(result.violationNamesRemovingPrefix(mo.observationPoint()))
                .contains(".insect.speciesBelongsToGenus");
    }

    // ----- invariants() features descent -----

    @Test
    void featuresDescent_reportsNestedViolations() {
        // A structurally invalid features view (null subject) surfaces under ".features".
        OrganismFeatureView<InsectRankName, InsectFeature> badFeatures = new OrganismFeatureView<>(null, List.of());
        Insect insect = Insect.empty().withFeatures(badFeatures);
        var mo = observer.forMethod("featuresDescent_reportsNestedViolations");
        assertThat(mo.observable(insect, "insect").violationNamesRemovingPrefix(mo.observationPoint()))
                .contains(".insect.features.subject");
    }

    // ----- invariants() role + children -----

    @Test
    void childrenNull_reportsInvariantViolation() {
        Insect insect = Insect.empty().withChildren(null);
        var mo = observer.forMethod("childrenNull_reportsInvariantViolation");
        assertThat(mo.observable(insect, "insect").violationNamesRemovingPrefix(mo.observationPoint()))
                .contains(".insect.children");
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

    private static InsectOrder order() {
        return InsectOrder.of(new InsectOrderInfo(
                orderName(),
                TaxonomicOrder.of("Lepidoptera"),
                description(),
                Set.of(),
                null));
    }

    private static InsectFamily family() {
        return InsectFamily.of(new InsectFamilyInfo(
                familyName(),
                orderName(),
                TaxonomicFamily.of("Papilionidae"),
                description(),
                Set.of(),
                null));
    }

    private static InsectGenus genus() {
        return InsectGenus.of(new InsectGenusInfo(
                genusName(),
                familyName(),
                TaxonomicGenus.of("Battus"),
                description(),
                Set.of(),
                null));
    }

    private static InsectSpecies species() {
        return InsectSpecies.of(new InsectSpeciesInfo(
                speciesName(),
                genusName(),
                TaxonomicSpecies.of("philenor"),
                description(),
                Set.of(),
                null,
                null,
                null, null, null, null, null, null, null));
    }

    private static Description description() {
        return new Description(
                RandomValue.string(), RandomValue.string(),
                RandomValue.string(), RandomValue.string());
    }

    // ----- binomial() -----

    @Test
    void binomial_joinsGenusAndEpithet() {
        Insect insect = Insect.empty().withGenus(genus()).withSpecies(species());

        assertThat(insect.binomial()).isEqualTo("Battus philenor");
    }

    @Test
    void binomial_isEmptyWithoutSpecies() {
        assertThat(Insect.empty().binomial()).isEmpty();
        assertThat(Insect.empty().withGenus(genus()).binomial()).isEmpty();
    }

    @Test
    void binomial_withoutGenus_isTheSpeciesSlug() {
        Insect insect = Insect.empty().withSpecies(species());

        assertThat(insect.binomial()).isEqualTo("battus-philenor");
    }

    @Test
    void speciesDisplayName_withoutCommonName_isTheBinomial() {
        Insect insect = Insect.empty().withGenus(genus()).withSpecies(species());

        assertThat(insect.speciesDisplayName()).isEqualTo("Battus philenor");
        assertThat(Insect.empty().speciesDisplayName()).isEmpty();
    }
}
