package com.naturalist.soil;

import com.naturalist.data.NaturalistDatabaseExtension;
import com.naturalist.soil.observation.AssessmentSource;
import com.naturalist.soil.observation.LabAnalysis;
import com.naturalist.soil.observation.NutrientCategory;
import com.naturalist.soil.observation.NutrientLine;
import com.naturalist.soil.observation.Nutrients;
import com.naturalist.soil.observation.OptimumComparison;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The Phase 7 presentation rules, asserted on the read model rather than on rendered HTML. Each
 * rule is a domain rule; the console only has to render what it is given.
 */
class SoilProfileViewTest {

    @RegisterExtension
    NaturalistDatabaseExtension db = NaturalistDatabaseExtension.create();

    private final SoilProfileQuery query = SoilsTestContextInternal.create(db).soilProfileQuery();

    private LabAnalysis box1() {
        return query.getBySoilProfileName(TestSoilIdentifiers.SoilProfiles.Box1.name)
                .orElseThrow().latestLabAnalysis().orElseThrow();
    }

    /**
     * A nutrient the lab did not run keeps its place in the table. A shorter table reads as a
     * complete one, which is how an unrun test becomes an invisible one.
     */
    @Test
    void everyCatalogedNutrientOfACategoryGetsALineWhetherMeasuredOrNot() {
        assertThat(box1().nutrientLines(NutrientCategory.PRIMARY))
                .extracting(NutrientLine::nutrientName)
                .containsExactlyElementsOf(Nutrients.of(NutrientCategory.PRIMARY));
        assertThat(box1().nutrientLines(NutrientCategory.SECONDARY)).hasSize(7);
        assertThat(box1().nutrientLines(NutrientCategory.MICRO)).hasSize(6);
    }

    @Test
    void aLinePairsTheReadingWithThePrintedOptimum() {
        NutrientLine nitrate = lineFor(NutrientCategory.PRIMARY, Nutrients.NITRATE_N);

        assertThat(nitrate.reading().orElseThrow().value()).isEqualByComparingTo("1.36");
        assertThat(nitrate.optimum().orElseThrow().range().printedForm()).isEqualTo("5.3 - 7.2");
        assertThat(nitrate.comparison().verdict()).isEqualTo(OptimumComparison.Verdict.BELOW);
    }

    /**
     * <b>The reason the source rule exists.</b> Box 1's zinc is 6.35 against a printed optimum of
     * 0.39–4.0, so our arithmetic says ABOVE — while FGL's own graphical bar for that row reads as
     * satisfactory. We do not store the lab's band boundaries and cannot reproduce its verdict, so
     * the comparison must be attributed to us. A page that showed this as the lab's judgment would
     * be misquoting the lab.
     */
    @Test
    void ourComparisonCanDisagreeWithTheLabAndSaysWhoseItIs() {
        NutrientLine zinc = lineFor(NutrientCategory.MICRO, Nutrients.ZINC);

        assertThat(zinc.reading().orElseThrow().value()).isEqualByComparingTo("6.35");
        assertThat(zinc.optimum().orElseThrow().range().printedForm()).isEqualTo("0.39 - 4.0");
        assertThat(zinc.comparison().verdict()).isEqualTo(OptimumComparison.Verdict.ABOVE);
        assertThat(zinc.comparison().source()).isEqualTo(AssessmentSource.DERIVED_FROM_PRINTED_RANGE);
    }

    /**
     * There is nowhere on a line to record a band, a bar position, or a percentage through a band.
     * The rule holds by construction: the type has no such component, so no template can render
     * one.
     */
    @Test
    void aLineCarriesAPositionRelativeToTheRangeAndNothingFiner() {
        assertThat(NutrientLine.class.getRecordComponents())
                .extracting(java.lang.reflect.RecordComponent::getName)
                .containsExactly("nutrientName", "reading", "optimum", "comparison");
        assertThat(OptimumComparison.Verdict.values())
                .containsExactly(OptimumComparison.Verdict.BELOW, OptimumComparison.Verdict.WITHIN,
                        OptimumComparison.Verdict.ABOVE, OptimumComparison.Verdict.NOT_COMPARABLE);
    }

    /** Soluble sodium is printed {@code < 19} and read 0.95 — within a ceiling that has no floor. */
    @Test
    void aCeilingOnlyOptimumComparesWithoutInventingAFloor() {
        NutrientLine sodium = lineFor(NutrientCategory.SECONDARY, Nutrients.SODIUM_SOLUBLE);

        assertThat(sodium.optimum().orElseThrow().range().printedForm()).isEqualTo("< 19");
        assertThat(sodium.comparison().verdict()).isEqualTo(OptimumComparison.Verdict.WITHIN);
    }

    private NutrientLine lineFor(NutrientCategory category,
                                 com.naturalist.soil.observation.NutrientName nutrientName) {
        List<NutrientLine> lines = box1().nutrientLines(category);
        return lines.stream()
                .filter(line -> line.nutrientName().equals(nutrientName))
                .findFirst()
                .orElseThrow();
    }
}
