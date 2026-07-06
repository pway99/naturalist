package com.naturalist.library.console.clade;

/**
 * Maps a clade slug to the console URL that best represents it.
 *
 * <p>Insecta is the single class owned by the insects catalog, so it bridges
 * into that catalog's root ({@code /insects/orders}) rather than its sparse
 * tree-of-life page (which lists only the handful of insect orders modelled as
 * clades). Every other clade resolves to its {@code /clades/{slug}} tree-of-life
 * page. This mirrors the {@code insecta -> /insects/orders} override the insects
 * console applies to its own breadcrumb, keeping descent through the tree of life
 * and the catalog a single continuous path.
 */
public final class CladeUrls {

    private CladeUrls() {
    }

    public static String of(String slug) {
        return "insecta".equals(slug) ? "/insects/orders" : "/clades/" + slug;
    }
}
