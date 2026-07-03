package com.naturalist.insects.console;

import com.naturalist.insects.console.InsectCladeAnchors.Anchor;
import com.naturalist.insects.console.InsectCladeAnchors.LineageEntry;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class InsectCladeAnchorsTest {

    @Test
    void emptyLineageAnchorsAtInsectaWithNoGap() {
        Anchor anchor = InsectCladeAnchors.resolve(List.of());
        assertThat(anchor.cladeSlug()).isEqualTo("insecta");
        assertThat(anchor.gapLabel()).isNull();
    }

    @Test
    void mappedOrderResolvesToItsCladeWithNoGap() {
        Anchor anchor = InsectCladeAnchors.resolve(List.of(
                new LineageEntry("lepidoptera", "Lepidoptera")));
        assertThat(anchor.cladeSlug()).isEqualTo("lepidoptera");
        assertThat(anchor.gapLabel()).isNull();
    }

    @Test
    void unmappedOrderFallsToInsectaWithGap() {
        Anchor anchor = InsectCladeAnchors.resolve(List.of(
                new LineageEntry("coleoptera", "Coleoptera")));
        assertThat(anchor.cladeSlug()).isEqualTo("insecta");
        assertThat(anchor.gapLabel()).isEqualTo("Coleoptera");
    }

    @Test
    void curatedOverrideResolvesCurrentEntityWithNoGap() {
        // Genus battus has no "battus" clade, but is curated to tribe Troidini.
        Anchor anchor = InsectCladeAnchors.resolve(List.of(
                new LineageEntry("battus", "Battus")));
        assertThat(anchor.cladeSlug()).isEqualTo("troidini");
        assertThat(anchor.gapLabel()).isNull();
    }

    @Test
    void deepGapAnchorsAtNearestMappedAncestor() {
        // Species/genus/family unmapped; order lepidoptera is the mapped ancestor.
        Anchor anchor = InsectCladeAnchors.resolve(List.of(
                new LineageEntry("vanessa-cardui", "Vanessa cardui"),
                new LineageEntry("vanessa", "Vanessa"),
                new LineageEntry("nymphalidae", "Nymphalidae"),
                new LineageEntry("lepidoptera", "Lepidoptera")));
        assertThat(anchor.cladeSlug()).isEqualTo("lepidoptera");
        assertThat(anchor.gapLabel()).isEqualTo("Vanessa cardui");
    }

    @Test
    void fullyUnmappedLineageFallsToInsectaWithCurrentEntityGap() {
        Anchor anchor = InsectCladeAnchors.resolve(List.of(
                new LineageEntry("harpalus-affinis", "Harpalus affinis"),
                new LineageEntry("harpalus", "Harpalus"),
                new LineageEntry("carabidae", "Carabidae"),
                new LineageEntry("coleoptera", "Coleoptera")));
        assertThat(anchor.cladeSlug()).isEqualTo("insecta");
        assertThat(anchor.gapLabel()).isEqualTo("Harpalus affinis");
    }
}
