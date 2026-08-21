package com.naturalist.plants;

import com.naturalist.observability.MethodObserver;
import com.naturalist.observability.Observer;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PlantTest {

    private final Observer observer = Observer.forClass(PlantTest.class);

    @Test
    void empty_hasNoRanks_andNoViolations() {
        Plant plant = Plant.empty();
        assertThat(plant.identifiedTo()).isEmpty();
        assertThat(observer.forMethod("empty").observable(plant, "plant").violations()).isEmpty();
    }

    @Test
    void childRankWithoutAncestor_reportsAncestorPresenceViolation() {
        // A genus set with no family is a broken ancestry spine.
        PlantGenus thymus = new PlantGenus(
                PlantGenusName.of("thymus"), PlantFamilyName.of("lamiaceae"),
                com.naturalist.taxonomy.TaxonomicFamily.of("Lamiaceae"),
                com.naturalist.taxonomy.TaxonomicGenus.of("Thymus"),
                new com.naturalist.fieldnotes.Description("a", "b", "c", "d"),
                java.util.Set.of());
        Plant plant = Plant.empty().withGenus(PlantGenusView.of(thymus));
        MethodObserver mo = observer.forMethod("childRankWithoutAncestor");
        assertThat(mo.observable(plant, "plant").violationNamesRemovingPrefix(mo.observationPoint()))
                .contains(".plant.genus:family");
    }

    @Test
    void identifiedTo_returnsMostSpecificRank() {
        PlantOrder order = new PlantOrder(
                PlantOrderName.of("lamiales"),
                com.naturalist.taxonomy.TaxonomicOrder.of("Lamiales"),
                new com.naturalist.fieldnotes.Description("a", "b", "c", "d"),
                java.util.Set.of(), null);
        Plant plant = Plant.empty().withOrder(PlantOrderView.of(order));
        assertThat(plant.identifiedTo()).contains(PlantOrderName.of("lamiales"));
    }

    @Test
    void withFeatures_carriesFeatureView() {
        PlantFeatureView fv = new PlantFeatureView(PlantOrderName.of("asterales"), java.util.List.of());
        Plant plant = Plant.empty().withFeatures(fv);
        assertThat(plant.features()).isEqualTo(fv);
    }

    @Test
    void emptyPlant_hasNullFeatures_andNoViolations() {
        Plant plant = Plant.empty();
        assertThat(plant.features()).isNull();
        assertThat(observer.forMethod("emptyFeatures").observable(plant, "plant").violations()).isEmpty();
    }
}
