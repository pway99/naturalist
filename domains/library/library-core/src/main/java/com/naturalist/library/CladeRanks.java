package com.naturalist.library;

import com.naturalist.clades.Clade;
import com.naturalist.taxonomy.LinealRank;

import java.util.Map;
import java.util.Optional;

/**
 * Curated clade → Linnaean rank correspondence. Ranked clades appear here;
 * rank-less clades (Eukaryota, Holometabola, Papilionoidea, etc.) are simply
 * absent and map to {@code Optional.empty()}.
 */
final class CladeRanks {

    private static final Map<String, LinealRank> RANK_BY_SLUG = Map.ofEntries(
            Map.entry("animalia", LinealRank.KINGDOM),
            Map.entry("arthropoda", LinealRank.PHYLUM),
            Map.entry("insecta", LinealRank.CLASS),
            Map.entry("blattodea", LinealRank.ORDER),
            Map.entry("hemiptera", LinealRank.ORDER),
            Map.entry("lepidoptera", LinealRank.ORDER),
            Map.entry("papilionidae", LinealRank.FAMILY),
            Map.entry("termitoidae", LinealRank.FAMILY)
    );

    private CladeRanks() {}

    static Optional<LinealRank> rankFor(Clade clade) {
        return Optional.ofNullable(RANK_BY_SLUG.get(clade.slug()));
    }
}
