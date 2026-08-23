package com.naturalist.plants.render;

import com.naturalist.data.NaturalistTestExtension;
import com.naturalist.fieldnotes.render.DescriptionRenderer;
import com.naturalist.plants.PlantSpecies;
import com.naturalist.plants.PlantSpeciesTestEntitySource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Acceptance for M6 and M7' ({@code docs/plans/catalog-kernel-redirect.md}). The
 * renderer is unit-tested with three complementary lenses:
 *
 * <ol>
 *   <li>A <b>catalog smoke test</b> — every plant × every Durrell level
 *       passes through the renderer without throwing, the output is
 *       non-blank, and the character count is conserved (the rendered
 *       HTML is at least as long as the escaped input).</li>
 *   <li><b>Worked-example assertions</b> — the structural artefacts the
 *       milestone calls out (header chip, paragraph count, italicised
 *       binomial, lifted numbered list) are pinned against the
 *       {@code crimson-clover} and {@code white-clover} university
 *       descriptions.</li>
 *   <li><b>Search-affordance assertions</b> — every detected binomial
 *       (full or abbreviated) is wrapped in a {@code /search?q=...} anchor
 *       with class {@code discover}, regardless of catalog membership. The
 *       URL round-trips the surface form through {@link java.net.URLEncoder}.</li>
 * </ol>
 */
class DescriptionRendererTest {

    @RegisterExtension
    NaturalistTestExtension db = NaturalistTestExtension.create();

    private final DescriptionRenderer renderer = new DescriptionRenderer(PlantsParagraphCues.CUES);
    private final List<PlantSpecies> plants = db.getNamed(PlantSpeciesTestEntitySource.class).entityStream().toList();

    // ── Catalog smoke test ───────────────────────────────────────────────

    @Test
    void rendersEveryPlantAtEveryLevelWithoutThrowing() {
        for (PlantSpecies plant : plants) {
            renderer.render(plant.description().preschool());
            renderer.render(plant.description().elementary());
            renderer.render(plant.description().secondary());
            renderer.render(plant.description().university());
        }
    }

    @Test
    void everyPlantAtEveryLevelEmitsNonBlankOutput() {
        for (PlantSpecies plant : plants) {
            assertThat(renderer.render(plant.description().preschool()))
                    .as("preschool/%s", plant.name().value()).isNotBlank();
            assertThat(renderer.render(plant.description().elementary()))
                    .as("elementary/%s", plant.name().value()).isNotBlank();
            assertThat(renderer.render(plant.description().secondary()))
                    .as("secondary/%s", plant.name().value()).isNotBlank();
            assertThat(renderer.render(plant.description().university()))
                    .as("university/%s", plant.name().value()).isNotBlank();
        }
    }

    @Test
    void renderedOutputIsAtLeastAsLongAsTheInput() {
        // Content-conservation invariant from the plan's acceptance: the
        // output is the input plus structural markup, never less.
        for (PlantSpecies plant : plants) {
            assertOutputGrowsOrEquals(plant, "preschool", plant.description().preschool());
            assertOutputGrowsOrEquals(plant, "elementary", plant.description().elementary());
            assertOutputGrowsOrEquals(plant, "secondary", plant.description().secondary());
            assertOutputGrowsOrEquals(plant, "university", plant.description().university());
        }
    }

    private void assertOutputGrowsOrEquals(PlantSpecies plant, String level, String input) {
        String rendered = renderer.render(input);
        assertThat(rendered.length())
                .as("%s/%s length", level, plant.name().value())
                .isGreaterThanOrEqualTo(input.length());
    }

    // ── Worked-example: crimson-clover.university ────────────────────────

    @Test
    void crimsonCloverUniversityEmitsTaxonomicHeaderChip() {
        String input = plantByName("trifolium-incarnatum").description().university();

        String rendered = renderer.render(input);

        assertThat(rendered).startsWith("<header class=\"description-taxonomy\">");
        assertThat(rendered).contains("<em>Trifolium incarnatum</em>");
        assertThat(rendered).contains("Fabaceae: Faboideae");
    }

    @Test
    void crimsonCloverUniversityRendersAtLeastThreeParagraphs() {
        String input = plantByName("trifolium-incarnatum").description().university();

        String rendered = renderer.render(input);

        long paragraphCount = rendered.lines().filter(line -> line.startsWith("<p>")).count();
        assertThat(paragraphCount).isGreaterThanOrEqualTo(3L);
    }

    @Test
    void crimsonCloverUniversityItalicisesBinomials() {
        String input = plantByName("trifolium-incarnatum").description().university();

        String rendered = renderer.render(input);

        assertThat(rendered).contains("<em>Trifolium incarnatum</em>");
        assertThat(rendered).contains("<em>Apis mellifera</em>");
        assertThat(rendered).contains("<em>T. pratense</em>");
    }

    // ── Worked-example: white-clover.university (numbered list) ──────────

    @Test
    void whiteCloverUniversityLiftsNumberedEnumerationIntoOrderedList() {
        String input = plantByName("trifolium-repens").description().university();

        String rendered = renderer.render(input);

        assertThat(rendered).contains("<ol>");
        assertThat(rendered).contains("</ol>");
        long itemCount = rendered.lines().filter(line -> line.contains("<li>")).count();
        assertThat(itemCount).isEqualTo(4L);
    }

    @Test
    void whiteCloverUniversityKeepsBinomialItalicsInsideListItems() {
        String input = plantByName("trifolium-repens").description().university();

        String rendered = renderer.render(input);

        // The fourth list item references Colias eurytheme and Lycaeides
        // melissa — both must be italicised inside their <li>.
        assertThat(rendered).contains("<em>Colias eurytheme</em>");
        assertThat(rendered).contains("<em>Lycaeides melissa</em>");
    }

    // ── Inputs without a header pass through ─────────────────────────────

    @Test
    void preschoolDescriptionsHaveNoHeaderChip() {
        // The preschool level is plain prose — no taxonomic header, so the
        // renderer should not emit the chip.
        for (PlantSpecies plant : plants) {
            String rendered = renderer.render(plant.description().preschool());
            assertThat(rendered)
                    .as("preschool/%s", plant.name().value())
                    .doesNotContain("description-taxonomy");
        }
    }

    // ── HTML escape ──────────────────────────────────────────────────────

    @Test
    void htmlSpecialCharactersAreEscapedInPlainText() {
        String rendered = renderer.render("A < B & C > D.");

        assertThat(rendered)
                .contains("&lt;")
                .contains("&amp;")
                .contains("&gt;")
                .doesNotContain(" < ")
                .doesNotContain(" & ")
                .doesNotContain(" > ");
    }

    @Test
    void nullAndBlankInputsRenderAsEmptyString() {
        assertThat(renderer.render(null)).isEmpty();
        assertThat(renderer.render("")).isEmpty();
        assertThat(renderer.render("   ")).isEmpty();
    }

    // ── Search-affordance wrap (M7') ─────────────────────────────────────

    @Test
    void fullBinomialIsWrappedInSearchAnchor() {
        String rendered = renderer.render("The vine Aristolochia californica is the larval host.");

        assertThat(rendered).contains(
                "<a href=\"/search?q=Aristolochia+californica\" class=\"discover\">"
                        + "<em>Aristolochia californica</em></a>");
    }

    @Test
    void abbreviatedBinomialIsWrappedInSearchAnchorWithItsOwnSurfaceForm() {
        // The abbreviated form has its own anchor pointing at the abbreviated
        // query — the renderer never expands "T. pratense" to "Trifolium
        // pratense"; the search page is responsible for resolving either form.
        String rendered = renderer.render("T. pratense leaves accumulate the toxin.");

        assertThat(rendered).contains(
                "<a href=\"/search?q=T.+pratense\" class=\"discover\">"
                        + "<em>T. pratense</em></a>");
    }

    @Test
    void unknownBinomialStillWrapsAsSearchAffordance() {
        // Under the routing model this would have stayed italics-only; under
        // search the empty-state lives on the search page, not in the
        // renderer.
        String rendered = renderer.render("Apis mellifera visits the flower.");

        assertThat(rendered).contains(
                "<a href=\"/search?q=Apis+mellifera\" class=\"discover\">"
                        + "<em>Apis mellifera</em></a>");
    }

    @Test
    void searchAnchorsCarryDiscoverClass() {
        // The class hook lets the layout style discovery affordances
        // distinctly from real links.
        String rendered = renderer.render("Trifolium pratense and Apis mellifera.");

        assertThat(rendered).contains("class=\"discover\"");
    }

    @Test
    void everyItalicisedBinomialIsAnAnchor() {
        // No italics-only graceful-degradation case survives M7'.
        for (PlantSpecies plant : plants) {
            String rendered = renderer.render(plant.description().university());
            int emCount = countOccurrences(rendered, "<em>");
            int anchorCount = countOccurrences(rendered, "<a href=\"/search?q=");
            assertThat(anchorCount)
                    .as("university/%s: every <em> binomial must sit inside a /search anchor",
                            plant.name().value())
                    .isEqualTo(emCount);
        }
    }

    // ── helpers ──────────────────────────────────────────────────────────

    private static int countOccurrences(String haystack, String needle) {
        int count = 0;
        int from = 0;
        while (true) {
            int idx = haystack.indexOf(needle, from);
            if (idx < 0) {
                return count;
            }
            count++;
            from = idx + needle.length();
        }
    }

    private PlantSpecies plantByName(String slug) {
        return plants.stream()
                .filter(p -> p.name().value().equals(slug))
                .findFirst()
                .orElseThrow(() -> new AssertionError("no plant with slug: " + slug));
    }
}
