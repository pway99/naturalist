package com.naturalist.chemistry;

import com.naturalist.chemistry.ChemistryTestContext;
import com.naturalist.chemistry.element.Element;
import com.naturalist.chemistry.element.ElementName;
import com.naturalist.data.NaturalistTestExtension;
import com.naturalist.data.PageRequest;
import gg.jte.output.StringOutput;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ChemistryElementsTemplateTest {

    @RegisterExtension
    final NaturalistTestExtension db = NaturalistTestExtension.create();

    private final ChemistryTestContext context =
            ChemistryTestContext.create(db);

    @Test
    void list_rendersEveryElementLinkedToItsDetailPage() {
        var page = context.elementQuery().findPage(PageRequest.console(0));
        StringOutput output = new StringOutput();

        TestTemplateEngine.create().render("chemistry/elements/list.jte",
                Map.of("elementsPage", page), output);

        String html = output.toString();
        assertThat(html).contains("/chemistry/elements/calcium");
        assertThat(html).contains("/chemistry/elements/boron");
        assertThat(html).contains("Ca");
        assertThat(html).contains("40.08");
    }

    @Test
    void detail_rendersEveryElementWithoutError() {
        for (Element element : context.elementQuery()
                .findPage(PageRequest.first(PageRequest.MAX_PAGE_SIZE)).content()) {
            StringOutput output = new StringOutput();
            TestTemplateEngine.create().render("chemistry/elements/detail.jte",
                    Map.of("element", element), output);
            assertThat(output.toString())
                    .as("rendered output for %s", element.name().value())
                    .isNotBlank();
        }
    }

    @Test
    void detail_showsIonicFormAndChargeCharacter() {
        Element calcium = context.elementQuery().getByName(ElementName.of("calcium")).orElseThrow();
        StringOutput output = new StringOutput();

        TestTemplateEngine.create().render("chemistry/elements/detail.jte",
                Map.of("element", calcium), output);

        String html = output.toString();
        assertThat(html).contains("Ca2+");
        assertThat(html).contains("cation");
        assertThat(html).contains("40.08");
    }

    @Test
    void detail_describesBoronAsNeitherCationNorAnion() {
        Element boron = context.elementQuery().getByName(ElementName.of("boron")).orElseThrow();
        StringOutput output = new StringOutput();

        TestTemplateEngine.create().render("chemistry/elements/detail.jte",
                Map.of("element", boron), output);

        String html = output.toString();
        assertThat(html).contains("H3BO3");
        assertThat(html).contains("uncharged");
        assertThat(html).doesNotContain(">cation<");
    }
}
