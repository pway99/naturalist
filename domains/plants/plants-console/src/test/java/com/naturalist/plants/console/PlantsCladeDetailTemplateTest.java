package com.naturalist.plants.console;

import com.naturalist.clades.Asterids;
import com.naturalist.clades.Clade;
import com.naturalist.clades.CladeTraversal;
import com.naturalist.clades.Superasterids;
import com.naturalist.data.NaturalistDatabase;
import com.naturalist.plants.PlantOrder;
import com.naturalist.plants.PlantOrderTestEntitySource;
import gg.jte.output.StringOutput;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Smoke test for {@code plants/clades/detail.jte} — a clade's page: the orders placed
 * directly at it, with the breadcrumb dropdowns providing tree navigation.
 */
class PlantsCladeDetailTemplateTest {

    private final NaturalistDatabase db = NaturalistDatabase.create();

    @Test
    void cladeDetail_showsDirectOrdersAndBreadcrumbDropdowns() {
        // Superasterids is the "both" case: Caryophyllales is placed directly on it, and it
        // still branches into a narrower Asterids clade reachable from the breadcrumb dropdown.
        PlantOrder caryophyllales = db.getNamed(PlantOrderTestEntitySource.class).entityStream()
                .filter(o -> o.name().value().equals("caryophyllales"))
                .findFirst().orElseThrow();
        List<Clade> trail = CladeTraversal.ancestry(new Superasterids()).reversed();

        StringOutput output = new StringOutput();
        TestTemplateEngine.create().render("plants/clades/detail.jte", Map.of(
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
