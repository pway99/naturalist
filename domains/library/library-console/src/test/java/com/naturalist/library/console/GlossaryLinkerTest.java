package com.naturalist.library.console;

import com.naturalist.library.GlossaryTerm;
import com.naturalist.library.GlossaryTermName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class GlossaryLinkerTest {

    private static GlossaryTerm term(String slug, String display) {
        return new GlossaryTerm(GlossaryTermName.of(slug), display, display + " means something.", null);
    }

    private static GlossaryLinker linker(GlossaryTerm... terms) {
        return GlossaryLinker.of(List.of(terms));
    }

    @Test
    void wrapsAKnownWholeWordTermInADefinitionPopover() {
        var html = linker(term("dorsum", "Dorsum")).linkHtml("spots on the dorsum here", "seed");

        assertThat(html).contains(
                "<button type=\"button\" class=\"glossary-link\" popovertarget=\"glossary-seed-dorsum\"");
        assertThat(html).contains(">dorsum</button>");
        assertThat(html).contains(
                "<span id=\"glossary-seed-dorsum\" popover class=\"info-popover-box glossary-popover\">");
        assertThat(html).contains("Dorsum means something.");
        assertThat(html).contains("<a href=\"/glossary/dorsum\">Open in glossary →</a>");
        assertThat(html).startsWith("spots on the ");
        assertThat(html).endsWith(" here");
    }

    @Test
    void foldsTheUsageExampleIntoThePopoverWhenPresent() {
        var withExample = new GlossaryTerm(
                GlossaryTermName.of("dorsum"), "Dorsum", "The upper surface of an animal.",
                "Bold white spots on the dorsum.");
        var html = GlossaryLinker.of(List.of(withExample)).linkHtml("marks on the dorsum", "seed");

        assertThat(html).contains("<em class=\"glossary-example\">Bold white spots on the dorsum.</em>");
    }

    @Test
    void omitsTheExampleBlockWhenThereIsNone() {
        var html = linker(term("dorsum", "Dorsum")).linkHtml("the dorsum", "seed");

        assertThat(html).doesNotContain("glossary-example");
    }

    @Test
    void idSeedMakesThePopoverIdUnique() {
        var linker = linker(term("dorsum", "Dorsum"));

        assertThat(linker.linkHtml("dorsum", "family-0")).contains("popovertarget=\"glossary-family-0-dorsum\"");
        assertThat(linker.linkHtml("dorsum", "family-1")).contains("popovertarget=\"glossary-family-1-dorsum\"");
    }

    @Test
    void matchesCaseInsensitivelyButPreservesTheMatchedCasing() {
        var html = linker(term("dorsum", "Dorsum")).linkHtml("Dorsum first", "seed");

        assertThat(html).contains(">Dorsum</button>");
        assertThat(html).contains("aria-label=\"Definition of Dorsum\"");
    }

    @Test
    void doesNotMatchInsideAnotherWord() {
        var html = linker(term("dorsum", "Dorsum")).linkHtml("an endorsement of dorsal marks", "seed");

        assertThat(html).doesNotContain("<button");
    }

    @Test
    void linksOnlyTheFirstOccurrenceOfEachTerm() {
        var html = linker(term("dorsum", "Dorsum")).linkHtml("dorsum and again dorsum", "seed");

        assertThat(html).containsOnlyOnce("<button");
        assertThat(html).endsWith("again dorsum");
    }

    @Test
    void prefersTheLongerTermOverAWordInsideIt() {
        var html = linker(term("field-mark", "field mark"), term("mark", "mark"))
                .linkHtml("a field mark to check", "seed");

        assertThat(html).contains("popovertarget=\"glossary-seed-field-mark\"");
        assertThat(html).contains(">field mark</button>");
        assertThat(html).doesNotContain("glossary-seed-mark");
    }

    @Test
    void escapesHtmlAndInjectsOnlyItsOwnMarkup() {
        var html = linker(term("dorsum", "Dorsum")).linkHtml("a <b>bold</b> & dark dorsum", "seed");

        assertThat(html).contains("&lt;b&gt;bold&lt;/b&gt;");
        assertThat(html).contains("&amp; dark");
        assertThat(html).doesNotContain("<b>");
        assertThat(html).contains(">dorsum</button>");
    }

    @Test
    void passesThroughEscapedTextWhenNoTermMatches() {
        var html = linker(term("dorsum", "Dorsum")).linkHtml("nothing to link < here", "seed");

        assertThat(html).isEqualTo("nothing to link &lt; here");
    }

    @Test
    void noneLinksNothingButStillEscapes() {
        var html = GlossaryLinker.none().linkHtml("plain <text> with dorsum", "seed");

        assertThat(html).isEqualTo("plain &lt;text&gt; with dorsum");
    }
}
