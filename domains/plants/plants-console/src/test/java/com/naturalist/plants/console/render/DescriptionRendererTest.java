package com.naturalist.plants.console.render;

import com.naturalist.plants.Plant;
import com.naturalist.plants.PlantTestEntitySource;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Acceptance for M6 ({@code kernels/atlas/PLAN.md}). The renderer is
 * unit-tested with two complementary lenses:
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
 * </ol>
 */
class DescriptionRendererTest {

    private final DescriptionRenderer renderer = new DescriptionRenderer();
    private final List<Plant> plants = new PlantTestEntitySource().entityStream().toList();

    // ── Catalog smoke test ───────────────────────────────────────────────

    @Test
    void rendersEveryPlantAtEveryLevelWithoutThrowing() {
        for (Plant plant : plants) {
            renderer.render(plant.description().preschool());
            renderer.render(plant.description().elementary());
            renderer.render(plant.description().secondary());
            renderer.render(plant.description().university());
        }
    }

    @Test
    void everyPlantAtEveryLevelEmitsNonBlankOutput() {
        for (Plant plant : plants) {
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
        for (Plant plant : plants) {
            assertOutputGrowsOrEquals(plant, "preschool", plant.description().preschool());
            assertOutputGrowsOrEquals(plant, "elementary", plant.description().elementary());
            assertOutputGrowsOrEquals(plant, "secondary", plant.description().secondary());
            assertOutputGrowsOrEquals(plant, "university", plant.description().university());
        }
    }

    private void assertOutputGrowsOrEquals(Plant plant, String level, String input) {
        String rendered = renderer.render(input);
        assertThat(rendered.length())
                .as("%s/%s length", level, plant.name().value())
                .isGreaterThanOrEqualTo(input.length());
    }

    // ── Worked-example: crimson-clover.university ────────────────────────

    @Test
    void crimsonCloverUniversityEmitsTaxonomicHeaderChip() {
        String input = plantByName("crimson-clover").description().university();

        String rendered = renderer.render(input);

        assertThat(rendered).startsWith("<header class=\"description-taxonomy\">");
        assertThat(rendered).contains("<em>Trifolium incarnatum</em>");
        assertThat(rendered).contains("Fabaceae: Faboideae");
    }

    @Test
    void crimsonCloverUniversityRendersAtLeastThreeParagraphs() {
        String input = plantByName("crimson-clover").description().university();

        String rendered = renderer.render(input);

        long paragraphCount = rendered.lines().filter(line -> line.startsWith("<p>")).count();
        assertThat(paragraphCount).isGreaterThanOrEqualTo(3L);
    }

    @Test
    void crimsonCloverUniversityItalicisesBinomials() {
        String input = plantByName("crimson-clover").description().university();

        String rendered = renderer.render(input);

        assertThat(rendered).contains("<em>Trifolium incarnatum</em>");
        assertThat(rendered).contains("<em>Apis mellifera</em>");
        assertThat(rendered).contains("<em>T. pratense</em>");
    }

    // ── Worked-example: white-clover.university (numbered list) ──────────

    @Test
    void whiteCloverUniversityLiftsNumberedEnumerationIntoOrderedList() {
        String input = plantByName("white-clover").description().university();

        String rendered = renderer.render(input);

        assertThat(rendered).contains("<ol>");
        assertThat(rendered).contains("</ol>");
        long itemCount = rendered.lines().filter(line -> line.contains("<li>")).count();
        assertThat(itemCount).isEqualTo(4L);
    }

    @Test
    void whiteCloverUniversityKeepsBinomialItalicsInsideListItems() {
        String input = plantByName("white-clover").description().university();

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
        for (Plant plant : plants) {
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

    // ── helpers ──────────────────────────────────────────────────────────

    private Plant plantByName(String slug) {
        return plants.stream()
                .filter(p -> p.name().value().equals(slug))
                .findFirst()
                .orElseThrow(() -> new AssertionError("no plant with slug: " + slug));
    }
}
