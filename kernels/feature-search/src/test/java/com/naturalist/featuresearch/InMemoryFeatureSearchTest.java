package com.naturalist.featuresearch;

import com.naturalist.insects.InsectFeatureId;   // a concrete EntityId for the test
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class InMemoryFeatureSearchTest {

    private static InMemoryFeatureSearch<InsectFeatureId> searchOver(String... values) {
        List<FeatureCorpus.Indexed<InsectFeatureId>> indexed = List.of(values).stream()
                .map(v -> new FeatureCorpus.Indexed<>(InsectFeatureId.create(), v))
                .toList();
        return new InMemoryFeatureSearch<>(indexed::stream);
    }

    @Test
    void surfacesPunctuationOnlyVariantAsTopMatch() {
        InMemoryFeatureSearch<InsectFeatureId> search = searchOver(
                "dark (black) pronotum contrasting with red elytra",
                "long slender cursorial legs");
        List<FeatureSearch.FeatureMatch<InsectFeatureId>> hits = search.findSimilar("dark/black pronotum contrasting with red elytra", 5);
        assertThat(hits).isNotEmpty();
        assertThat(hits.get(0).value()).isEqualTo("dark (black) pronotum contrasting with red elytra");
        assertThat(hits.get(0).score()).isGreaterThan(0.6);
    }

    @Test
    void dropsBelowThreshold() {
        InMemoryFeatureSearch<InsectFeatureId> search = searchOver("hardened elytra");
        assertThat(search.findSimilar("six short slender black legs", 5)).isEmpty();
    }

    @Test
    void respectsLimitAndOrdersByScoreDescending() {
        InMemoryFeatureSearch<InsectFeatureId> search = searchOver("small head concealed under pronotum",
                "small rounded head partially concealed under pronotum",
                "shiny metallic coloration");
        List<FeatureSearch.FeatureMatch<InsectFeatureId>> hits = search.findSimilar("small head largely concealed beneath pronotum", 1);
        assertThat(hits).hasSize(1);
        assertThat(hits.get(0).value()).isEqualTo("small head concealed under pronotum");
    }

    @Test
    void emptyOrBlankQueryReturnsNothing() {
        InMemoryFeatureSearch<InsectFeatureId> search = searchOver("hardened elytra");
        assertThat(search.findSimilar("", 5)).isEmpty();
        assertThat(search.findSimilar(null, 5)).isEmpty();
    }

    @Test
    void searchesManyValuesInOneCorpusLoad() {
        AtomicInteger loads = new AtomicInteger();
        FeatureCorpus.Indexed<InsectFeatureId> hardened = new FeatureCorpus.Indexed<>(InsectFeatureId.create(), "hardened elytra");
        InMemoryFeatureSearch<InsectFeatureId> search = new InMemoryFeatureSearch<InsectFeatureId>(() -> {
            loads.incrementAndGet();
            return Stream.of(hardened);
        });

        Map<String, List<FeatureSearch.FeatureMatch<InsectFeatureId>>> hits = search.findSimilarToEach(List.of("hardened black elytra", "six short legs", " "), 5);

        assertThat(loads).hasValue(1);
        assertThat(hits.keySet()).containsExactly("hardened black elytra", "six short legs", " ");
        assertThat(hits.get("hardened black elytra")).extracting(FeatureSearch.FeatureMatch::value).containsExactly("hardened elytra");
        assertThat(hits.get("six short legs")).isEmpty();
        assertThat(hits.get(" ")).isEmpty();
    }
}
