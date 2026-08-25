package com.naturalist.featuresearch;

import com.naturalist.ddd.EntityId;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * In-memory feature search: normalized token-set Jaccard over the corpus, returning
 * the closest existing features above {@link #THRESHOLD}. Dependency-free and
 * gate-safe — a single corpus load then an in-memory scan, no per-feature select.
 */
public final class InMemoryFeatureSearch<ID extends EntityId> implements FeatureSearch<ID> {

    /** Minimum Jaccard overlap to be considered a candidate. Tunable. */
    static final double THRESHOLD = 0.4;
    private static final Pattern SPLIT = Pattern.compile("[\\s\\p{Punct}]+");

    private final FeatureCorpus<ID> corpus;

    public InMemoryFeatureSearch(FeatureCorpus<ID> corpus) {
        this.corpus = corpus;
    }

    @Override
    public List<FeatureMatch<ID>> findSimilar(String value, int limit) {
        if (value == null || value.isBlank() || limit <= 0) return List.of();
        Set<String> queryTokens = tokenise(value);
        if (queryTokens.isEmpty()) return List.of();

        try (var entries = corpus.load()) {
            return entries
                    .map(e -> new FeatureMatch<>(e.id(), e.value(),
                            jaccard(queryTokens, tokenise(e.value()))))
                    .filter(m -> m.score() >= THRESHOLD)
                    .sorted(Comparator.comparingDouble(FeatureMatch<ID>::score).reversed())
                    .limit(limit)
                    .toList();
        }
    }

    private static Set<String> tokenise(String text) {
        return Arrays.stream(SPLIT.split(text.toLowerCase()))
                .filter(t -> t.length() >= 2)
                .collect(Collectors.toSet());
    }

    private static double jaccard(Set<String> a, Set<String> b) {
        if (a.isEmpty() && b.isEmpty()) return 1.0;
        long intersection = a.stream().filter(b::contains).count();
        long union = a.size() + b.size() - intersection;
        return union == 0 ? 0.0 : (double) intersection / union;
    }
}
