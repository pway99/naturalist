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
        // Species unmapped; genus vanessa is curated to the Papilionoidea
        // superfamily, so the trail descends there and gaps at the species.
        Anchor anchor = InsectCladeAnchors.resolve(List.of(
                new LineageEntry("vanessa-cardui", "Vanessa cardui"),
                new LineageEntry("vanessa", "Vanessa"),
                new LineageEntry("nymphalidae", "Nymphalidae"),
                new LineageEntry("lepidoptera", "Lepidoptera")));
        assertThat(anchor.cladeSlug()).isEqualTo("papilionoidea");
        assertThat(anchor.gapLabel()).isEqualTo("Vanessa cardui");
    }

    @Test
    void fullyUnmappedLineageFallsToInsectaWithCurrentEntityGap() {
        // Beetles have no clade at any rank the kernel models — an honest gap.
        Anchor anchor = InsectCladeAnchors.resolve(List.of(
                new LineageEntry("hippodamia-convergens", "Hippodamia convergens"),
                new LineageEntry("hippodamia", "Hippodamia"),
                new LineageEntry("coccinellidae", "Coccinellidae"),
                new LineageEntry("coleoptera", "Coleoptera")));
        assertThat(anchor.cladeSlug()).isEqualTo("insecta");
        assertThat(anchor.gapLabel()).isEqualTo("Hippodamia convergens");
    }

    @Test
    void beeGenusAnchorsAtAnthophila() {
        Anchor anchor = InsectCladeAnchors.resolve(List.of(
                new LineageEntry("apis", "Apis")));
        assertThat(anchor.cladeSlug()).isEqualTo("anthophila");
        assertThat(anchor.gapLabel()).isNull();
    }

    @Test
    void drosophilaSpeciesAnchorAtTheirRespectiveSubgenera() {
        assertThat(InsectCladeAnchors.resolve(List.of(
                new LineageEntry("drosophila-melanogaster", "Drosophila melanogaster"),
                new LineageEntry("drosophila", "Drosophila"))).cladeSlug())
                .isEqualTo("sophophora");
        assertThat(InsectCladeAnchors.resolve(List.of(
                new LineageEntry("drosophila-funebris", "Drosophila funebris"),
                new LineageEntry("drosophila", "Drosophila"))).cladeSlug())
                .isEqualTo("drosophila-sensu-stricto");
    }

    @Test
    void termiteGenusAnchorsAtTermitoidae() {
        Anchor anchor = InsectCladeAnchors.resolve(List.of(
                new LineageEntry("reticulitermes", "Reticulitermes")));
        assertThat(anchor.cladeSlug()).isEqualTo("termitoidae");
        assertThat(anchor.gapLabel()).isNull();
    }
}
