package com.naturalist.insects.console;

import com.naturalist.data.NaturalistDatabase;
import com.naturalist.data.Page;
import com.naturalist.data.PageRequest;
import com.naturalist.insects.*;
import gg.jte.output.StringOutput;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class InsectsOrdersTemplateTest {

    @Test
    void orders_rendersWithoutError() {
        NaturalistDatabase database = NaturalistDatabase.create();
        Page<InsectOrder> orderPage = new InsectOrderTestEntitySource(database)
                .pageOf(PageRequest.console(0));
        StringOutput output = new StringOutput();

        TestTemplateEngine.create().render(
                "insects/orders.jte",
                Map.of("orderPage", orderPage),
                output);

        assertThat(output.toString()).isNotBlank();
    }

    @Test
    void order_rendersWithoutError() {
        NaturalistDatabase database = NaturalistDatabase.create();
        InsectOrder anyOrder = new InsectOrderTestEntitySource(database).entityStream()
                .findFirst().orElseThrow();
        StringOutput output = new StringOutput();

        TestTemplateEngine.create().render(
                "insects/order.jte",
                Map.of(
                        "order", anyOrder,
                        "children", List.of(),
                        "descriptionPreschool", "p",
                        "descriptionElementary", "e",
                        "descriptionSecondary", "s",
                        "descriptionUniversity", "u"),
                output);

        assertThat(output.toString()).isNotBlank();
    }

    @Test
    void order_rendersChildFamilyCardFromPermit() {
        NaturalistDatabase database = NaturalistDatabase.create();
        InsectFamily anyFamily = new InsectFamilyTestEntitySource(database).entityStream()
                .findFirst().orElseThrow();
        InsectOrder order = new InsectOrderTestEntitySource(database)
                .getByName(anyFamily.orderName()).orElseThrow();
        InsectTaxonView familyChild = InsectFamilyView.of(
                anyFamily,
                InsectEntityCollections.ImageCollection.empty(),
                InsectEntityCollections.FeatureCollection.empty());
        StringOutput output = new StringOutput();

        TestTemplateEngine.create().render(
                "insects/order.jte",
                Map.of(
                        "order", order,
                        "children", List.of(familyChild),
                        "descriptionPreschool", "p",
                        "descriptionElementary", "e",
                        "descriptionSecondary", "s",
                        "descriptionUniversity", "u"),
                output);

        String familyDisplayName = anyFamily.commonNames().stream().findFirst()
                .map(cn -> cn.label()).orElse(anyFamily.name().value());
        assertThat(output.toString()).contains(familyDisplayName);
        assertThat(output.toString()).contains("/insects/families/" + anyFamily.name().value());
    }
}
