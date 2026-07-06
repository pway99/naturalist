package com.naturalist.library.console.clade;

import com.naturalist.catalog.Catalog;
import com.naturalist.catalog.EntityRefLinker;
import com.naturalist.taxonomy.LinealRank;

/**
 * Resolves the rank eyebrow of a clade step to a console URL — the Linnaean
 * axis of clade navigation.
 *
 * <p>A rank eyebrow labels a clade as its Linnaean counterpart (e.g. the FAMILY
 * above <em>Papilionidae</em>), so it links to that taxon's catalog page rather
 * than an abstract rank definition. Resolution goes through the cross-domain
 * catalog seam — never a direct dependency on any catalog-owning domain:
 *
 * <ol>
 *   <li>{@code insecta} is special-cased to {@code /insects/orders}: the class
 *       is the root of the insects catalog and has no self-entity, so its
 *       eyebrow points at the catalog's landing (the orders within).</li>
 *   <li>Otherwise the slug is resolved against the {@link Catalog}; a hit is
 *       rendered by the composite {@link EntityRefLinker} (e.g.
 *       {@code /insects/families/papilionidae}).</li>
 *   <li>A clade with no catalog entity (Termitoidae, Animalia, Arthropoda…)
 *       falls back to the {@code /concepts/{rank}} rank-definition page — no
 *       broken links.</li>
 * </ol>
 */
public final class CladeRankLinks {

    private CladeRankLinks() {
    }

    public static String forStep(Catalog catalog, EntityRefLinker linker, String slug, LinealRank rank) {
        if ("insecta".equals(slug)) {
            return "/insects/orders";
        }
        String catalogUrl = catalog.findBySlug(slug).map(linker::linkFor).orElse(null);
        if (catalogUrl != null) {
            return catalogUrl;
        }
        return "/concepts/" + rank.name().toLowerCase();
    }
}
