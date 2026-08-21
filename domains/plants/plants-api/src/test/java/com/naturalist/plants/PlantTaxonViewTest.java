package com.naturalist.plants;

import com.naturalist.observability.Observer;
import com.naturalist.plants.PlantEntityCollections.ImageCollection;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PlantTaxonViewTest {

    private final Observer observer = Observer.forClass(PlantTaxonViewTest.class);

    private static PlantFamily asteraceae() {
        return new PlantFamily(
                PlantFamilyName.of("asteraceae"),
                PlantOrderName.of("asterales"),
                com.naturalist.taxonomy.TaxonomicFamily.of("Asteraceae"),
                new com.naturalist.fieldnotes.Description("a", "b", "c", "d"),
                java.util.Set.of());
    }

    @Test
    void familyView_of_exposesNameAndFk_andHasNoViolations() {
        PlantFamilyView view = PlantFamilyView.of(asteraceae());
        assertThat(view.name()).isEqualTo(PlantFamilyName.of("asteraceae"));
        assertThat(view.orderName()).isEqualTo(PlantOrderName.of("asterales"));
        assertThat(view.images().isEmpty()).isTrue();
        assertThat(observer.forMethod("valid").observable(view, "view").violations()).isEmpty();
    }

    @Test
    void familyView_belongsToOrder_matchesFk_andToleratesNull() {
        PlantFamilyView family = PlantFamilyView.of(asteraceae());
        PlantOrderView asterales = PlantOrderView.of(new PlantOrder(
                PlantOrderName.of("asterales"),
                com.naturalist.taxonomy.TaxonomicOrder.of("Asterales"),
                new com.naturalist.fieldnotes.Description("a", "b", "c", "d"),
                java.util.Set.of(),
                null));
        assertThat(family.belongsToOrder(asterales)).isTrue();
        assertThat(family.belongsToOrder(null)).isTrue();
    }

    @Test
    void dispatchesAcrossSealedPermits() {
        PlantTaxonView view = PlantFamilyView.of(asteraceae());
        String tag = switch (view) {
            case PlantOrderView v -> "order:" + v.order().name().value();
            case PlantFamilyView v -> "family:" + v.family().name().value();
            case PlantGenusView v -> "genus:" + v.genus().name().value();
            case PlantSpeciesView v -> "species:" + v.species().name().value();
        };
        assertThat(tag).isEqualTo("family:asteraceae");
    }
}
