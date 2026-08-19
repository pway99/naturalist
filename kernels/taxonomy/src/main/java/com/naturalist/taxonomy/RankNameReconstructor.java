package com.naturalist.taxonomy;

/**
 * Domain-supplied bridge that rebuilds the correct typed permit from a slug + rank pair
 * read out of JSON. Each organism domain injects its own (e.g. {@code InsectRankName::of})
 * into the mapper that reads that domain's catalog; the kernel deserializer stays
 * domain-agnostic.
 */
@FunctionalInterface
public interface RankNameReconstructor {
    RankName reconstruct(String slug, LinealRank rank);
}
