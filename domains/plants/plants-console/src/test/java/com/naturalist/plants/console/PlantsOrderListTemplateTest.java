package com.naturalist.plants.console;

import com.naturalist.data.NaturalistDatabase;
import com.naturalist.data.Page;
import com.naturalist.data.PageRequest;
import com.naturalist.plants.PlantOrder;
import com.naturalist.plants.PlantOrderTestEntitySource;
import gg.jte.output.StringOutput;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Smoke test for {@code plants/orders/list.jte} — the browse entry point
 * for order-rank records.
 */
class PlantsOrderListTemplateTest {

    @Test
    void orderList_rendersWithoutError() {
        Page<PlantOrder> ordersPage = NaturalistDatabase.create()
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
}
