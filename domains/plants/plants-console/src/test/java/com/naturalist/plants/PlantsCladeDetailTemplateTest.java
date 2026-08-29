package com.naturalist.plants;

import com.naturalist.spring.console.ConsoleSliceTemplates;

import com.naturalist.clades.Asterids;
import com.naturalist.clades.Clade;
import com.naturalist.clades.CladeTraversal;
import com.naturalist.clades.Superasterids;
import com.naturalist.data.NaturalistTestExtension;
import com.naturalist.library.CladeStep;
import gg.jte.output.StringOutput;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Smoke test for {@code plants/clades/detail.jte} — a clade's page: the orders placed
 * directly at it, with the breadcrumb dropdowns providing tree navigation.
 */
class PlantsCladeDetailTemplateTest {

    @RegisterExtension
    private final NaturalistTestExtension nte = NaturalistTestExtension.create();

    @Test
    void cladeDetail_showsDirectOrdersAndBreadcrumbDropdowns() {
        // Superasterids is the "both" case: Caryophyllales is placed directly on it, and it
        // still branches into a narrower Asterids clade reachable from the breadcrumb dropdown.
        PlantOrder caryophyllales = nte.getNamed(PlantOrderTestEntitySource.class).entityStream()
                .filter(o -> o.name().value().equals("caryophyllales"))
                .findFirst().orElseThrow();
        List<CladeStep> trail = CladeTraversal.ancestry(new Superasterids()).reversed().stream()
                .map(c -> new CladeStep(c.slug(), c.displayName(), Optional.empty()))
                .toList();

        StringOutput output = new StringOutput();
        ConsoleSliceTemplates.create().render("plants/clades/detail.jte", Map.of(
                "clade", new Superasterids(),
                "orders", List.of(caryophyllales),
                "childClades", List.of(new Asterids()),
                "cladeTrail", trail), output);

        String html = output.toString();
        // The order placed directly here links into the order catalog.
        assertThat(html).contains("href=\"/plants/orders/caryophyllales\"");
        // The narrower clades are presented on the page like the orders, linking deeper.
        assertThat(html).contains("Clades within Superasterids");
        assertThat(html).contains("href=\"/plants/clades/asterids\"");
        // The current clade is the bold, non-linked breadcrumb node.
        assertThat(html).contains("clade-trail-current");
        assertThat(html).contains("<strong>Superasterids</strong>");
    }
}
