package com.naturalist.plants.console;

import com.naturalist.clades.Clade;
import com.naturalist.clades.CladeTraversal;
import com.naturalist.clades.Eudicots;
import com.naturalist.clades.Eukaryota;
import com.naturalist.clades.Superasterids;
import com.naturalist.clades.Superrosids;
import com.naturalist.data.NaturalistDatabase;
import com.naturalist.plants.PlantOrder;
import com.naturalist.plants.PlantOrderTestEntitySource;
import gg.jte.output.StringOutput;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Smoke test for {@code plants/clades/detail.jte} — a clade's page in the plant
 * catalog: its orders, its narrower clades, and the trail with the current node marked.
 */
class PlantsCladeDetailTemplateTest {

    private final NaturalistDatabase db = NaturalistDatabase.create();

    @Test
    void cladeDetail_showsOrdersChildrenAndCurrentNode() {
        PlantOrder brassicales = db.getNamed(PlantOrderTestEntitySource.class).entityStream()
                .filter(o -> o.name().value().equals("brassicales"))
                .findFirst().orElseThrow();
        List<Clade> trail = CladeTraversal.ancestry(new Eudicots()).reversed().stream()
                .filter(node -> !(node instanceof Eukaryota))
                .toList();

        StringOutput output = new StringOutput();
        TestTemplateEngine.create().render("plants/clades/detail.jte", Map.of(
                "clade", new Eudicots(),
                "orders", List.of(brassicales),
                "childClades", List.of(new Superrosids(), new Superasterids()),
                "cladeTrail", trail), output);

        String html = output.toString();
        // Orders in the clade link into the order catalog.
        assertThat(html).contains("href=\"/plants/orders/brassicales\"");
        // Narrower clades drill further down, staying in the plant catalog.
        assertThat(html).contains("href=\"/plants/clades/superrosids\"");
        assertThat(html).contains("href=\"/plants/clades/superasterids\"");
        // Ancestors are links; the current clade is the non-linked, bold trail node.
        assertThat(html).contains("href=\"/plants/clades/angiosperms\"");
        assertThat(html).contains("clade-trail-current");
        assertThat(html).doesNotContain("href=\"/plants/clades/eudicots\"");
    }
}
