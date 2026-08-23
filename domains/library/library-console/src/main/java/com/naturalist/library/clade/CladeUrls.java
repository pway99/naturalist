package com.naturalist.library.clade;

/**
 * Maps a clade slug to its tree-of-life page URL — the phylogenetic axis of
 * clade navigation.
 *
 * <p>Clade <em>names</em> always stay in the tree of life ({@code /clades/{slug}}).
 * The bridge into the Linnaean catalog is carried by the <em>rank eyebrow</em>
 * instead (see {@link CladeRankLinks}), keeping the two axes distinct: the name
 * traces evolutionary descent, the rank points at the taxon's catalog page.
 *
 * <p>Descent through the tree of life and the catalog stays a single continuous
 * path because {@code CladesController} still redirects {@code /clades/insecta}
 * (the class, which has no tree page of its own) to {@code /insects/orders}.
 */
public final class CladeUrls {

    private CladeUrls() {
    }

    public static String of(String slug) {
        return "/clades/" + slug;
    }
}
