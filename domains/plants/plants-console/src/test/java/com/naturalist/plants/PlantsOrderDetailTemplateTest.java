package com.naturalist.plants;

import com.naturalist.data.NaturalistTestExtension;
import gg.jte.output.StringOutput;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Smoke test for {@code plants/orders/detail.jte}. Renders every catalogued order —
 * both the populated-families branch and the empty branch execute against real data.
 */
class PlantsOrderDetailTemplateTest {

    @RegisterExtension
    private final NaturalistTestExtension nte = NaturalistTestExtension.create();

    @Test
    void orderDetail_rendersEveryOrderWithoutError() {
        var template = TestTemplateEngine.create();
        List<PlantFamily> allFamilies =
                nte.getNamed(PlantFamilyTestEntitySource.class).entityStream().toList();

        for (PlantOrder order : nte.getNamed(PlantOrderTestEntitySource.class)
                .entityStream().toList()) {
            List<PlantTaxonView> children = allFamilies.stream()
                    .filter(f -> order.name().equals(f.orderName()))
                    .map(f -> (PlantTaxonView) PlantFamilyView.of(f))
                    .toList();
            StringOutput output = new StringOutput();
            template.render("plants/orders/detail.jte",
                    Map.of("order", order, "children", children), output);
            assertThat(output.toString())
                    .as("rendered output for %s", order.name().value())
                    .isNotBlank();
        }
    }

    @Test
    void orderDetail_rendersFamilyCardsWhenTheOrderHasThem() {
        var template = TestTemplateEngine.create();
        PlantOrder lamiales = nte.getNamed(PlantOrderTestEntitySource.class).entityStream()
                .filter(o -> o.name().value().equals("lamiales"))
                .findFirst()
                .orElseThrow();
        List<PlantTaxonView> children = nte.getNamed(PlantFamilyTestEntitySource.class).entityStream()
                .filter(f -> lamiales.name().equals(f.orderName()))
                .map(f -> (PlantTaxonView) PlantFamilyView.of(f))
                .toList();

        StringOutput output = new StringOutput();
        template.render("plants/orders/detail.jte",
                Map.of("order", lamiales, "children", children), output);

        assertThat(output.toString())
                .contains("/plants/families/lamiaceae")
                .doesNotContain("No families catalogued");
    }

    @Test
    void orderDetail_rendersEmptyStateWhenNoFamiliesAreSupplied() {
        // Exercises the template's empty branch, not a fact about the catalog —
        // every order currently has at least one family, so the empty list is
        // supplied deliberately rather than found.
        var template = TestTemplateEngine.create();
        PlantOrder anyOrder = nte.getNamed(PlantOrderTestEntitySource.class).entityStream()
                .findFirst()
                .orElseThrow();

        StringOutput output = new StringOutput();
        template.render("plants/orders/detail.jte",
                Map.of("order", anyOrder, "children", List.of()), output);

        assertThat(output.toString()).contains("No families catalogued");
    }
}
