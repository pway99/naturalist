package com.naturalist.insects;

import com.naturalist.data.NaturalistTestExtension;
import com.naturalist.data.Page;
import com.naturalist.data.PageRequest;
import com.naturalist.insects.*;
import gg.jte.output.StringOutput;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class InsectsOrdersTemplateTest {

    @RegisterExtension
    NaturalistTestExtension nte = NaturalistTestExtension.create();

    @Test
    void orders_rendersWithoutError() {
        Page<InsectOrder> orderPage = nte.getNamed(InsectOrderTestEntitySource.class)
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
        InsectOrder anyOrder = nte.getNamed(InsectOrderTestEntitySource.class).entityStream()
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
        InsectFamily anyFamily = nte.getNamed(InsectFamilyTestEntitySource.class).entityStream()
                .findFirst().orElseThrow();
        InsectOrder order = nte.getNamed(InsectOrderTestEntitySource.class)
                .getByName(anyFamily.orderName()).orElseThrow();
        InsectTaxonView familyChild = InsectFamilyView.of(
                anyFamily,
                InsectEntityCollections.ImageCollection.empty());
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
