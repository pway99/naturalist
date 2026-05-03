package com.naturalist.fieldnotes.render;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Kernel-level tests for {@link DescriptionRenderer} using synthetic
 * inputs only &mdash; no domain fixtures. Domain-flavored worked
 * examples (Oak Vista plants, etc.) live in the consuming domain's
 * console module so they can use that domain's test entity sources.
 */
class DescriptionRendererTest {

    private final DescriptionRenderer renderer = new DescriptionRenderer(List.of(
            "Management constraint",
            "Practical significance"));

    @Test
    void nullAndBlankInputsRenderAsEmptyString() {
        assertThat(renderer.render(null)).isEmpty();
        assertThat(renderer.render("")).isEmpty();
        assertThat(renderer.render("   ")).isEmpty();
    }

    @Test
    void plainTextProducesASingleParagraph() {
        String rendered = renderer.render("A simple sentence with no structure.");

        assertThat(rendered).isEqualTo("<p>A simple sentence with no structure.</p>\n");
    }

    @Test
    void htmlSpecialCharactersAreEscaped() {
        String rendered = renderer.render("A < B & C > D.");

        assertThat(rendered)
                .contains("&lt;")
                .contains("&amp;")
                .contains("&gt;")
                .doesNotContain(" < ")
                .doesNotContain(" & ")
                .doesNotContain(" > ");
    }

    // ── Configurable paragraph cues ──────────────────────────────────────

    @Test
    void sentenceFollowedByConfiguredCueBecomesParagraphBreak() {
        String rendered = renderer.render(
                "First sentence. Management constraint applies on hot days.");

        long paragraphs = rendered.lines().filter(line -> line.startsWith("<p>")).count();
        assertThat(paragraphs).isEqualTo(2L);
    }

    @Test
    void sentenceFollowedByUnconfiguredPhraseStaysInSameParagraph() {
        String rendered = renderer.render(
                "First sentence. Some other phrase that is not a cue.");

        long paragraphs = rendered.lines().filter(line -> line.startsWith("<p>")).count();
        assertThat(paragraphs).isEqualTo(1L);
    }

    @Test
    void emptyCueListProducesSingleParagraph() {
        DescriptionRenderer noCues = new DescriptionRenderer(List.of());
        String rendered = noCues.render(
                "First sentence. Management constraint applies on hot days.");

        long paragraphs = rendered.lines().filter(line -> line.startsWith("<p>")).count();
        assertThat(paragraphs).isEqualTo(1L);
    }

    @Test
    void nullCueListIsTreatedAsEmpty() {
        DescriptionRenderer nullCues = new DescriptionRenderer(null);
        String rendered = nullCues.render("Some prose. With more prose.");

        assertThat(rendered).isEqualTo("<p>Some prose. With more prose.</p>\n");
    }

    // ── Authored paragraph breaks (\n\n) ─────────────────────────────────

    @Test
    void blankLineAuthoredByCallerBecomesParagraphBreak() {
        String rendered = renderer.render(
                "First authored paragraph.\n\nSecond authored paragraph.");

        long paragraphs = rendered.lines().filter(line -> line.startsWith("<p>")).count();
        assertThat(paragraphs).isEqualTo(2L);
    }

    @Test
    void authoredBreakWorksEvenWhenNoCueWouldFire() {
        // The classic case: short narrative prose with no domain cue
        // applicable, but the author wants a break for readability.
        DescriptionRenderer noCues = new DescriptionRenderer(List.of());
        String rendered = noCues.render(
                "Short narrative sentence.\n\nFollow-up narrative sentence.");

        long paragraphs = rendered.lines().filter(line -> line.startsWith("<p>")).count();
        assertThat(paragraphs).isEqualTo(2L);
    }

    @Test
    void cuesFireIndependentlyWithinEachAuthoredParagraph() {
        // \n\n splits first; each authored chunk is then scanned for cues.
        String rendered = renderer.render(
                "Lead sentence. Management constraint applies."
                        + "\n\n"
                        + "Second authored paragraph. Practical significance follows.");

        long paragraphs = rendered.lines().filter(line -> line.startsWith("<p>")).count();
        assertThat(paragraphs).isEqualTo(4L);
    }

    @Test
    void multipleConsecutiveBlankLinesCollapseToASingleBreak() {
        String rendered = renderer.render("First.\n\n\n\nSecond.");

        long paragraphs = rendered.lines().filter(line -> line.startsWith("<p>")).count();
        assertThat(paragraphs).isEqualTo(2L);
    }

    // ── Auto-detected section labels ─────────────────────────────────────

    @Test
    void titleCaseNounPhraseEndingInColonBecomesParagraphBreak() {
        DescriptionRenderer noCues = new DescriptionRenderer(List.of());
        String rendered = noCues.render(
                "Opening sentence about the species. Nesting biology: females excavate burrows.");

        long paragraphs = rendered.lines().filter(line -> line.startsWith("<p>")).count();
        assertThat(paragraphs).isEqualTo(2L);
    }

    @Test
    void hyphenatedSectionLabelMatches() {
        DescriptionRenderer noCues = new DescriptionRenderer(List.of());
        String rendered = noCues.render(
                "Opening prose. Pollen-host specialisation varies: some are oligolectic.");

        long paragraphs = rendered.lines().filter(line -> line.startsWith("<p>")).count();
        assertThat(paragraphs).isEqualTo(2L);
    }

    @Test
    void multipleSectionLabelsProduceMultipleBreaks() {
        DescriptionRenderer noCues = new DescriptionRenderer(List.of());
        String rendered = noCues.render(
                "Opening prose about the species. "
                        + "Nesting biology: females excavate burrows. "
                        + "Management relevance: bare soil patches are obligate. "
                        + "Functional response: type II.");

        long paragraphs = rendered.lines().filter(line -> line.startsWith("<p>")).count();
        assertThat(paragraphs).isEqualTo(4L);
    }

    @Test
    void singleWordColonSuffixDoesNotTriggerBreak() {
        // Common discourse markers like "Note:" or "However:" should not
        // fragment paragraphs.
        DescriptionRenderer noCues = new DescriptionRenderer(List.of());
        String rendered = noCues.render(
                "Opening sentence. Note: this is a footnote-style aside.");

        long paragraphs = rendered.lines().filter(line -> line.startsWith("<p>")).count();
        assertThat(paragraphs).isEqualTo(1L);
    }

    @Test
    void longTailingColonClauseDoesNotTriggerBreak() {
        // A sentence-internal colon with five or more words preceding it is
        // not a section label, just enumeration.
        DescriptionRenderer noCues = new DescriptionRenderer(List.of());
        String rendered = noCues.render(
                "Opening sentence. The species exhibits the following key trait: "
                        + "burrow construction.");

        long paragraphs = rendered.lines().filter(line -> line.startsWith("<p>")).count();
        assertThat(paragraphs).isEqualTo(1L);
    }

    @Test
    void sentenceCaseColonDoesNotTriggerBreak() {
        // Lowercase-first noun phrase ending in a colon is a sentence-internal
        // marker, not a section header.
        DescriptionRenderer noCues = new DescriptionRenderer(List.of());
        String rendered = noCues.render(
                "Opening sentence. larval development: three instars over fifteen days.");

        long paragraphs = rendered.lines().filter(line -> line.startsWith("<p>")).count();
        assertThat(paragraphs).isEqualTo(1L);
    }

    // ── Header extraction ────────────────────────────────────────────────

    @Test
    void leadingTaxonomicHeaderIsLiftedIntoChip() {
        String rendered = renderer.render(
                "Trifolium incarnatum L. — Fabaceae: Faboideae. Body prose follows.");

        assertThat(rendered).startsWith("<header class=\"description-taxonomy\">");
        assertThat(rendered).contains("<em>Trifolium incarnatum</em>");
        assertThat(rendered).contains("Fabaceae: Faboideae");
    }

    @Test
    void inputWithoutHeaderHasNoChip() {
        String rendered = renderer.render("Plain prose with no header.");

        assertThat(rendered).doesNotContain("description-taxonomy");
    }

    // ── Numbered enumeration lifting ─────────────────────────────────────

    @Test
    void numberedSequenceIsLiftedIntoOrderedList() {
        String rendered = renderer.render(
                "Three things matter: (1) the first thing; (2) the second thing; "
                        + "(3) the third thing.");

        assertThat(rendered).contains("<ol>");
        assertThat(rendered).contains("</ol>");
        long items = rendered.lines().filter(line -> line.contains("<li>")).count();
        assertThat(items).isEqualTo(3L);
    }

    // ── Binomial italics & search wrap ───────────────────────────────────

    @Test
    void fullBinomialIsWrappedInSearchAnchor() {
        String rendered = renderer.render("The vine Aristolochia californica is the larval host.");

        assertThat(rendered).contains(
                "<a href=\"/search?q=Aristolochia+californica\" class=\"discover\">"
                        + "<em>Aristolochia californica</em></a>");
    }

    @Test
    void abbreviatedBinomialIsWrappedInSearchAnchorWithItsOwnSurfaceForm() {
        String rendered = renderer.render("T. pratense leaves accumulate the toxin.");

        assertThat(rendered).contains(
                "<a href=\"/search?q=T.+pratense\" class=\"discover\">"
                        + "<em>T. pratense</em></a>");
    }

    @Test
    void unknownBinomialStillWrapsAsSearchAffordance() {
        String rendered = renderer.render("Nonsensus genericus visits the flower.");

        assertThat(rendered).contains(
                "<a href=\"/search?q=Nonsensus+genericus\" class=\"discover\">"
                        + "<em>Nonsensus genericus</em></a>");
    }

    @Test
    void searchAnchorsCarryDiscoverClass() {
        String rendered = renderer.render("Trifolium pratense and Apis mellifera.");

        assertThat(rendered).contains("class=\"discover\"");
    }
}
