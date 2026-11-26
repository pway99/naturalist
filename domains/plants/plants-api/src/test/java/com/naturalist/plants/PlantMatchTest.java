package com.naturalist.plants;

import com.naturalist.RandomValue;
import com.naturalist.fieldnotes.CommonName;
import com.naturalist.fieldnotes.Description;
import com.naturalist.observability.InvariantObservation;
import com.naturalist.observability.MethodObserver;
import com.naturalist.observability.Observer;
import com.naturalist.plants.PlantEntityCollections.ImageCollection;
import com.naturalist.plants.PlantTaxonView.FamilyView;
import com.naturalist.plants.PlantTaxonView.GenusView;
import com.naturalist.plants.PlantTaxonView.OrderView;
import com.naturalist.taxonomy.TaxonomicFamily;
import com.naturalist.taxonomy.TaxonomicGenus;
import com.naturalist.taxonomy.TaxonomicOrder;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class PlantMatchTest {

    private static final Observer observer = Observer.forClass(PlantMatchTest.class);

    @Test
    void genusFallbackIsConsistent() {
        MethodObserver mo = observer.forMethod("genusFallbackIsConsistent");
        PlantMatch match = new PlantMatch(
                PlantSpeciesName.of("thymus-vulgaris"), PlantGenusName.of("thymus"), thymusPlant());

        InvariantObservation result = mo.observable(match, "match");

        assertThat(result.violationNamesRemovingPrefix(mo.observationPoint())).isEmpty();
    }

    @Test
    void exactMatchIsConsistent() {
        MethodObserver mo = observer.forMethod("exactMatchIsConsistent");
        PlantMatch match = new PlantMatch(PlantGenusName.of("thymus"), PlantGenusName.of("thymus"), thymusPlant());

        InvariantObservation result = mo.observable(match, "match");

        assertThat(result.violationNamesRemovingPrefix(mo.observationPoint())).isEmpty();
        assertThat(match.exact()).isTrue();
    }

    @Test
    void plantNotCarryingFoundIsRefused() {
        MethodObserver mo = observer.forMethod("plantNotCarryingFoundIsRefused");
        PlantMatch match = new PlantMatch(
                PlantGenusName.of("salvia"), PlantGenusName.of("salvia"), thymusPlant());

        InvariantObservation result = mo.observable(match, "match");

        assertThat(result.violationNamesRemovingPrefix(mo.observationPoint()))
                .containsExactly(".match.plantIsFound");
    }

    @Test
    void foundBelowAskedIsRefused() {
        MethodObserver mo = observer.forMethod("foundBelowAskedIsRefused");
        PlantMatch match = new PlantMatch(
                PlantGenusName.of("thymus"), PlantSpeciesName.of("thymus-vulgaris"), thymusPlant());

        InvariantObservation result = mo.observable(match, "match");

        assertThat(result.violationNamesRemovingPrefix(mo.observationPoint()))
                .contains(".match.foundIsAskedOrAbove");
    }

    private static Plant thymusPlant() {
        PlantGenus genus = new PlantGenus(
                PlantGenusName.of("thymus"),
                PlantFamilyName.of("lamiaceae"),
                TaxonomicFamily.of("Lamiaceae"),
                TaxonomicGenus.of("Thymus"),
                new Description(RandomValue.string(), RandomValue.string(), RandomValue.string(), RandomValue.string()),
                Set.of(CommonName.of("thyme")));
        PlantFamily family = new PlantFamily(
                PlantFamilyName.of("lamiaceae"),
                PlantOrderName.of("lamiales"),
                TaxonomicFamily.of("Lamiaceae"),
                new Description(RandomValue.string(), RandomValue.string(), RandomValue.string(), RandomValue.string()),
                Set.of(CommonName.of("mint family")));
        PlantOrder order = new PlantOrder(
                PlantOrderName.of("lamiales"),
                TaxonomicOrder.of("Lamiales"),
                new Description(RandomValue.string(), RandomValue.string(), RandomValue.string(), RandomValue.string()),
                Set.of(CommonName.of("mint order")),
                null);
        return Plant.empty()
                .withOrder(new OrderView(order, ImageCollection.empty()))
                .withFamily(new FamilyView(family, ImageCollection.empty()))
                .withGenus(new GenusView(genus, ImageCollection.empty()));
    }
}
