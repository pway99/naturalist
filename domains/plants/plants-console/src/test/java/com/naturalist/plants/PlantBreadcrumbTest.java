package com.naturalist.plants;

import com.naturalist.data.NaturalistTestExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.assertj.core.api.Assertions.assertThat;

class PlantBreadcrumbTest {

    @RegisterExtension
    final NaturalistTestExtension nte = NaturalistTestExtension.create();

    private final PlantsTestContext context = PlantsTestContext.create(nte);

    @Test
    void breadcrumb_forSpecies_isPlantaeThroughAncestorsToCurrent() {
        var plant = context.plantQuery().getByName(
                PlantSpeciesName.of("aristolochia-californica")).orElseThrow();
        var crumbs = PlantsController.breadcrumbFor(plant);

        assertThat(crumbs).extracting(BreadcrumbSegment::label).containsExactly(
                "Plantae", "Piperales", "Aristolochiaceae", "Aristolochia", "Aristolochia californica");
        assertThat(crumbs).extracting(BreadcrumbSegment::url).containsExactly(
                "/plants/orders", "/plants/orders/piperales", "/plants/families/aristolochiaceae",
                "/plants/genera/aristolochia", null);   // current segment has no link
        assertThat(crumbs.get(crumbs.size() - 1).currentPage()).isTrue();
    }

    @Test
    void breadcrumb_forOrder_isPlantaeThenCurrentOrder() {
        var plant = context.plantQuery().getByName(PlantOrderName.of("lamiales")).orElseThrow();
        var crumbs = PlantsController.breadcrumbFor(plant);

        assertThat(crumbs).extracting(BreadcrumbSegment::label).containsExactly("Plantae", "Lamiales");
        assertThat(crumbs.get(1).currentPage()).isTrue();
    }
}
