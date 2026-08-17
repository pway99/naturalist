package com.naturalist.insects.console;

import com.naturalist.clades.Clade;
import com.naturalist.clades.CladeTraversal;
import com.naturalist.clades.Plantae;

/**
 * Where a clade node in the insect tree-of-life trail links. Plant clades cross into
 * the <em>plants</em> console ({@code /plants/clades/{slug}}) so a naturalist who follows
 * the lineage up to Eukaryota and over into Plantae lands in the rich plant catalog —
 * not the bare shared browser. Animal and insect clades stay in the shared
 * {@code /clades} tree-of-life browser, from which Insecta redirects into the insect
 * catalog. The mirror of {@code plants} console's {@code PlantCladeTree.pageUrl}; the two
 * consoles share no code, so the tiny routing rule lives once in each.
 */
public final class CladeNav {

    private CladeNav() {
    }

    public static String pageUrl(Clade clade) {
        boolean plant = CladeTraversal.ancestry(clade).stream().anyMatch(node -> node instanceof Plantae);
        return plant ? "/plants/clades/" + clade.slug() : "/clades/" + clade.slug();
    }
}
