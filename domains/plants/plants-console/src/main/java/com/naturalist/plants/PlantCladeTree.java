package com.naturalist.plants;

import com.naturalist.clades.Animalia;
import com.naturalist.clades.Clade;
import com.naturalist.clades.CladeCatalog;
import com.naturalist.clades.CladeTraversal;
import com.naturalist.clades.Plantae;

import java.util.List;

/**
 * Navigation over the plant region of the shared clade tree, backing the
 * clade-breadcrumb dropdowns. {@link #narrower} is a clade's child clades — the
 * branches to drill into. Siblings need no dropdown of their own: a clade's siblings
 * are its parent's children, and the parent sits one node to the left in the trail
 * with exactly that dropdown.
 */
public final class PlantCladeTree {

    private PlantCladeTree() {
    }

    /** Direct child clades — the narrower branches to drill into. */
    public static List<Clade> narrower(Clade clade) {
        return CladeCatalog.childrenOf(clade);
    }

    /**
     * The in-console page for a clade, so the breadcrumb never strands the naturalist in
     * the shared tree-of-life browser. Animal clades cross into the insects console; plant
     * clades and the shared Eukaryota root stay in the plants console (each domain owns the
     * root's page, showing its own organisms and offering the other kingdom as a child).
     */
    public static String pageUrl(Clade clade) {
        return isAnimal(clade) ? "/insects/clades/" + clade.slug() : "/plants/clades/" + clade.slug();
    }

    /** A clade belongs to the plant catalog iff its lineage passes through Plantae. */
    public static boolean isPlant(Clade clade) {
        return CladeTraversal.ancestry(clade).stream().anyMatch(node -> node instanceof Plantae);
    }

    /** A clade belongs to the animal kingdom iff its lineage passes through Animalia. */
    public static boolean isAnimal(Clade clade) {
        return CladeTraversal.ancestry(clade).stream().anyMatch(node -> node instanceof Animalia);
    }
}
