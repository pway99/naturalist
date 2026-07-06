package com.naturalist.insects.console;

import com.naturalist.clades.Clade;

import java.util.List;
import java.util.Map;

/**
 * Resolves the deepest tree-of-life clade for an insect catalog lineage.
 *
 * <p>The catalog (Linnaean ranks) and the clade tree (evolutionary lineages)
 * diverge: many catalogued taxa have no clade node (e.g. order Coleoptera),
 * and some map to a clade under a different slug (e.g. genus {@code battus} →
 * tribe {@code troidini}). Slug-equality resolves the common case; the curated
 * {@link #OVERRIDE} map covers the divergent ones. A catalog entity with no
 * clade of its own is a genuine gap — a research invitation, not an error.
 */
final class InsectCladeAnchors {

    /**
     * Catalog slug → clade slug, for entities whose clade lives under a
     * different name (or a deeper clade the kernel models but the catalog
     * doesn't name identically). Keys may be genus or species slugs — the
     * resolver checks the deepest lineage entry first, so a species-level
     * entry wins over its genus. Extend as the naturalist maps more lineages;
     * a taxon absent here with no slug-equal clade is a genuine, honest gap.
     */
    private static final Map<String, String> OVERRIDE = Map.ofEntries(
            // Butterflies — pipevine swallowtail sits in tribe Troidini; the
            // other two butterflies have no family clade, so they anchor at the
            // Papilionoidea superfamily and gap honestly at their unmodelled family.
            Map.entry("battus", "troidini"),
            Map.entry("vanessa", "papilionoidea"),
            Map.entry("colias", "papilionoidea"),
            // Bees — Anthophila is the clade of all bees.
            Map.entry("apis", "anthophila"),
            Map.entry("xylocopa", "anthophila"),
            Map.entry("andrena", "anthophila"),
            Map.entry("halictus", "anthophila"),
            // Beewolf — an apoid wasp: within Apoidea but outside the bee clade.
            Map.entry("philanthus", "apoidea"),
            // Fruit flies — genus to its subfamily, each species to its subgenus.
            Map.entry("drosophila", "drosophilinae"),
            Map.entry("drosophila-melanogaster", "sophophora"),
            Map.entry("drosophila-funebris", "drosophila-sensu-stricto"),
            // Subterranean termite.
            Map.entry("reticulitermes", "termitoidae")
    );

    /** Every catalogued insect sits at least within class Insecta. */
    private static final String FLOOR = "insecta";

    private InsectCladeAnchors() {
    }

    /**
     * @param lineage current entity first (deepest), then each ancestor; empty
     *                for rank-list pages.
     * @return the anchor clade slug plus a gap label (the current entity's
     *         display name) when the current entity itself is unmapped.
     */
    static Anchor resolve(List<LineageEntry> lineage) {
        for (int i = 0; i < lineage.size(); i++) {
            String cladeSlug = OVERRIDE.getOrDefault(lineage.get(i).slug(), lineage.get(i).slug());
            if (isClade(cladeSlug)) {
                String gapLabel = (i == 0) ? null : lineage.get(0).displayName();
                return new Anchor(cladeSlug, gapLabel);
            }
        }
        String gapLabel = lineage.isEmpty() ? null : lineage.get(0).displayName();
        return new Anchor(FLOOR, gapLabel);
    }

    private static boolean isClade(String slug) {
        try {
            Clade.of(slug);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    record LineageEntry(String slug, String displayName) {
    }

    record Anchor(String cladeSlug, String gapLabel) {
    }
}
