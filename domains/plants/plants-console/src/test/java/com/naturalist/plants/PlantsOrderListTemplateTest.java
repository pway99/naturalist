package com.naturalist.plants;

import com.naturalist.clades.Angiosperms;
import com.naturalist.clades.Plantae;
import com.naturalist.data.NaturalistTestExtension;
import com.naturalist.data.Page;
import com.naturalist.data.PageRequest;
import gg.jte.output.StringOutput;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Smoke test for {@code plants/orders/list.jte} — the browse entry point
 * for order-rank records.
 */
class PlantsOrderListTemplateTest {

    @RegisterExtension
    final NaturalistTestExtension nte = NaturalistTestExtension.create();

    @Test
    void orderList_rendersWithoutError() {
        Page<PlantOrder> ordersPage = nte
                .getNamed(PlantOrderTestEntitySource.class)
                .pageOf(PageRequest.console(0));
        StringOutput output = new StringOutput();

        TestTemplateEngine.create().render(
                "plants/orders/list.jte",
                Map.of("ordersPage", ordersPage),
                output);

        assertThat(output.toString())
                .isNotBlank()
                .contains("/plants/orders/lamiales");
    }

    @Test
    void orderList_showsCatalogCladeRootWhenSupplied() {
        Page<PlantOrder> ordersPage = nte
                .getNamed(PlantOrderTestEntitySource.class)
                .pageOf(PageRequest.console(0));
        StringOutput output = new StringOutput();

        // The kingdom-level landing descends to the deepest clade shared by every
        // catalogued order — for a catalogue of flowering plants, Plantae › Angiosperms.
        TestTemplateEngine.create().render(
                "plants/orders/list.jte",
                Map.of("ordersPage", ordersPage,
                        "cladeTrail", List.of(new Plantae(), new Angiosperms())),
                output);

        assertThat(output.toString())
                .contains("Tree of life")
                .contains("href=\"/plants/clades/plantae\"")
                .contains("href=\"/plants/clades/angiosperms\"");
    }
}
