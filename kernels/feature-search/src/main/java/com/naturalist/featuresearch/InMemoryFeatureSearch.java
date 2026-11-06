package com.naturalist.featuresearch;

import com.naturalist.ddd.EntityId;

import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
        if (value == null) return List.of();
        return findSimilarToEach(List.of(value), limit).get(value);
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

    public Map<String, List<FeatureMatch<ID>>> findSimilarToEach(Collection<String> values, int limit) {
        var matches = new LinkedHashMap<String, List<FeatureMatch<ID>>>();
        var queries = new LinkedHashMap<String, Set<String>>();
        for (String value : values) {
            matches.put(value, List.of());
            Set<String> tokens = tokenise(value);
            if (!tokens.isEmpty()) {
                queries.put(value, tokens);
            }
        }
        if (queries.isEmpty() || limit <= 0) {
            return matches;
        }
        try (var entries = corpus.load()) {
            List<FeatureCorpus.Indexed<ID>> features = entries.toList();
            queries.forEach((value, tokens) -> matches.put(value, closest(features, tokens, limit)));
        }
        return matches;
    }

    private List<FeatureMatch<ID>> closest(List<FeatureCorpus.Indexed<ID>> features, Set<String> queryTokens, int limit) {
        return features.stream()
                .map(e -> new FeatureMatch<>(e.id(), e.value(), jaccard(queryTokens, tokenise(e.value()))))
                .filter(m -> m.score() >= THRESHOLD)
                .sorted(Comparator.comparingDouble(FeatureMatch<ID>::score).reversed())
                .limit(limit)
                .toList();
    }
}
