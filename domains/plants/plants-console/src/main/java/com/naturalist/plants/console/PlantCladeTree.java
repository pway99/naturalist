package com.naturalist.plants.console;

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

    /** A clade belongs to the plant catalog iff its lineage passes through Plantae. */
    public static boolean isPlant(Clade clade) {
        return CladeTraversal.ancestry(clade).stream().anyMatch(node -> node instanceof Plantae);
    }
}
