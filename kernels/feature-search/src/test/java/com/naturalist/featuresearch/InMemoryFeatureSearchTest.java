package com.naturalist.featuresearch;

import com.naturalist.insects.InsectFeatureId;   // a concrete EntityId for the test
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class InMemoryFeatureSearchTest {

    private static InMemoryFeatureSearch<InsectFeatureId> searchOver(String... values) {
        var indexed = List.of(values).stream()
                .map(v -> new FeatureCorpus.Indexed<>(InsectFeatureId.create(), v))
                .toList();
        return new InMemoryFeatureSearch<>(indexed::stream);
    }

    @Test
    void surfacesPunctuationOnlyVariantAsTopMatch() {
        var search = searchOver(
                "dark (black) pronotum contrasting with red elytra",
                "long slender cursorial legs");
        var hits = search.findSimilar("dark/black pronotum contrasting with red elytra", 5);
        assertThat(hits).isNotEmpty();
        assertThat(hits.get(0).value()).isEqualTo("dark (black) pronotum contrasting with red elytra");
        assertThat(hits.get(0).score()).isGreaterThan(0.6);
    }

    @Test
    void dropsBelowThreshold() {
        var search = searchOver("hardened elytra");
        assertThat(search.findSimilar("six short slender black legs", 5)).isEmpty();
    }

    @Test
    void respectsLimitAndOrdersByScoreDescending() {
        var search = searchOver("small head concealed under pronotum",
                "small rounded head partially concealed under pronotum",
                "shiny metallic coloration");
        var hits = search.findSimilar("small head largely concealed beneath pronotum", 1);
        assertThat(hits).hasSize(1);
        assertThat(hits.get(0).value()).isEqualTo("small head concealed under pronotum");
    }

    @Test
    void emptyOrBlankQueryReturnsNothing() {
        var search = searchOver("hardened elytra");
        assertThat(search.findSimilar("", 5)).isEmpty();
        assertThat(search.findSimilar(null, 5)).isEmpty();
    }
}
