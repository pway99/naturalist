package com.naturalist.insects;

import com.naturalist.spring.console.ConsoleSliceTemplates;

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
        List<InsectViews.RankCard> cards = orderPage.content().stream()
                .map(o -> new InsectViews.RankCard(o.name(), o.displayName(), o.order().value(), null, null,
                        InsectEntityCollections.ImageCollection.empty()))
                .toList();
        StringOutput output = new StringOutput();

        ConsoleSliceTemplates.create().render(
                "insects/orders.jte",
                Map.of("cards", new Page<>(cards, orderPage.pageNumber(), orderPage.pageSize(),
                        orderPage.pagesAheadKnown(), orderPage.moreBeyondLookahead())),
                output);

        assertThat(output.toString()).isNotBlank();
    }

    @Test
    void order_rendersWithoutError() {
        InsectOrder anyOrder = nte.getNamed(InsectOrderTestEntitySource.class).entityStream()
                .findFirst().orElseThrow();
        StringOutput output = new StringOutput();

        ConsoleSliceTemplates.create().render(
                "insects/order.jte",
                Map.of(
                        "page", orderPage(anyOrder, List.of()),
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
        InsectTaxonView familyChild = InsectTaxonView.FamilyView.of(
                anyFamily,
                InsectEntityCollections.ImageCollection.empty());
        StringOutput output = new StringOutput();

        ConsoleSliceTemplates.create().render(
                "insects/order.jte",
                Map.of(
                        "page", orderPage(order, List.of(familyChild)),
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

    private static InsectViews.RankPage orderPage(InsectOrder order, List<InsectTaxonView> children) {
        return InsectViews.RankPage.anonymous(Insect.empty()
                .withOrder(InsectTaxonView.OrderView.of(order))
                .withChildren(children));
    }
}
