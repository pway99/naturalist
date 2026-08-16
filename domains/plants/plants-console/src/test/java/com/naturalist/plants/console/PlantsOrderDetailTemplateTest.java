package com.naturalist.plants.console;

import com.naturalist.data.NaturalistDatabase;
import com.naturalist.plants.PlantFamily;
import com.naturalist.plants.PlantFamilyTestEntitySource;
import com.naturalist.plants.PlantOrder;
import com.naturalist.plants.PlantOrderTestEntitySource;
import gg.jte.output.StringOutput;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Smoke test for {@code plants/orders/detail.jte}. Renders every catalogued order —
 * both the populated-families branch and the empty branch execute against real data.
 */
class PlantsOrderDetailTemplateTest {

    private final NaturalistDatabase db = NaturalistDatabase.create();

    @Test
    void orderDetail_rendersEveryOrderWithoutError() {
        var template = TestTemplateEngine.create();
        List<PlantFamily> allFamilies =
                db.getNamed(PlantFamilyTestEntitySource.class).entityStream().toList();

        for (PlantOrder order : db.getNamed(PlantOrderTestEntitySource.class)
                .entityStream().toList()) {
            List<PlantFamily> families = allFamilies.stream()
                    .filter(f -> order.name().equals(f.orderName()))
                    .toList();
            StringOutput output = new StringOutput();
            template.render("plants/orders/detail.jte",
                    Map.of("order", order, "families", families), output);
            assertThat(output.toString())
                    .as("rendered output for %s", order.name().value())
                    .isNotBlank();
        }
    }

    @Test
    void orderDetail_rendersFamilyCardsWhenTheOrderHasThem() {
        var template = TestTemplateEngine.create();
        PlantOrder lamiales = db.getNamed(PlantOrderTestEntitySource.class).entityStream()
                .filter(o -> o.name().value().equals("lamiales"))
                .findFirst()
                .orElseThrow();
        List<PlantFamily> families = db.getNamed(PlantFamilyTestEntitySource.class).entityStream()
                .filter(f -> lamiales.name().equals(f.orderName()))
                .toList();

        StringOutput output = new StringOutput();
        template.render("plants/orders/detail.jte",
                Map.of("order", lamiales, "families", families), output);

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
        PlantOrder anyOrder = db.getNamed(PlantOrderTestEntitySource.class).entityStream()
                .findFirst()
                .orElseThrow();

        StringOutput output = new StringOutput();
        template.render("plants/orders/detail.jte",
                Map.of("order", anyOrder, "families", List.of()), output);

        assertThat(output.toString()).contains("No families catalogued");
    }
}
