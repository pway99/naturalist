package com.naturalist.insects.console;

import com.naturalist.clades.Clade;
import com.naturalist.clades.CladeTraversal;
import com.naturalist.clades.Plantae;

/**
 * The in-console page for a clade node in the insect tree-of-life trail, so the breadcrumb
 * never strands the naturalist in the shared tree-of-life browser. Plant clades cross into
 * the <em>plants</em> console ({@code /plants/clades/{slug}}); animal and insect clades —
 * and the shared Eukaryota root — stay in the insects console ({@code /insects/clades/{slug}}).
 * The mirror of the plants console's {@code PlantCladeTree.pageUrl}; the two consoles share
 * no code, so the tiny routing rule lives once in each.
 */
public final class CladeNav {

    private CladeNav() {
    }

    public static String pageUrl(Clade clade) {
        boolean plant = CladeTraversal.ancestry(clade).stream().anyMatch(node -> node instanceof Plantae);
        return plant ? "/plants/clades/" + clade.slug() : "/insects/clades/" + clade.slug();
    }
}
