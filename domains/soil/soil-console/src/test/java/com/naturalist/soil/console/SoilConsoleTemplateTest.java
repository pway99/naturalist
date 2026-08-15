package com.naturalist.soil.console;

import com.naturalist.data.NaturalistDatabase;
import com.naturalist.data.PageRequest;
import com.naturalist.library.LibraryTestContext;
import com.naturalist.library.console.GlossaryLinker;
import com.naturalist.soil.CropName;
import com.naturalist.soil.SoilProfile;
import com.naturalist.soil.SoilProfileInfo;
import com.naturalist.soil.SoilProfileName;
import com.naturalist.soil.SoilTestContext;
import com.naturalist.soil.observation.LabAnalysis;
import com.naturalist.soil.observation.LabAnalysisId;
import com.naturalist.soil.observation.LabAnalysisInfo;
import com.naturalist.soil.observation.MeasurementUnit;
import com.naturalist.soil.observation.MicroNutrients;
import com.naturalist.soil.observation.NutrientPanel;
import com.naturalist.soil.observation.NutrientReading;
import com.naturalist.soil.observation.NutrientReadingId;
import com.naturalist.soil.observation.Nutrients;
import com.naturalist.soil.observation.PrimaryNutrients;
import com.naturalist.soil.observation.ReportedOptimumCollection;
import com.naturalist.soil.observation.ReportedRecommendationCollection;
import com.naturalist.soil.observation.SecondaryNutrients;
import com.naturalist.zone.ZoneName;
import gg.jte.output.StringOutput;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class SoilConsoleTemplateTest {

    private static SoilTestContext context() {
        return SoilTestContext.create(NaturalistDatabase.create());
    }

    @Test
    void list_rendersProfilesWithZoneAndCrop() {
        SoilTestContext ctx = context();
        List<SoilProfile> profiles = ctx.soilProfileInfoQuery()
                .findPage(PageRequest.console(0)).content().stream()
                .map(info -> ctx.soilProfileQuery().getBySoilProfileName(info.name()).orElseThrow())
                .toList();
        StringOutput output = new StringOutput();

        TestTemplateEngine.create().render("soil/list.jte", Map.of("profiles", profiles), output);

        String html = output.toString();
        assertThat(html).contains("box1");
        assertThat(html).contains("tomato");
        assertThat(html).contains("/soil/profiles/box1");
    }

    @Test
    void profile_rendersPanelAndCharacteristics() {
        NaturalistDatabase db = NaturalistDatabase.create();
        SoilProfile box1 = SoilTestContext.create(db).soilProfileQuery()
                .getBySoilProfileName(SoilProfileName.of("box1")).orElseThrow();
        GlossaryLinker glossaryLinker = GlossaryLinker.of(LibraryTestContext.create(db).glossaryTermQuery()
                .findPage(PageRequest.first(PageRequest.MAX_PAGE_SIZE)).content());
        StringOutput output = new StringOutput();

        TestTemplateEngine.create().render("soil/profile.jte",
                Map.of("profile", box1, "glossaryLinker", glossaryLinker), output);

        String html = output.toString();
        assertThat(html).contains("Nitrate-N");
        assertThat(html).contains("6.99");           // soluble calcium
        assertThat(html).contains("lbs/1000 ft²");    // MeasurementUnit.symbol()
        assertThat(html).contains("7.2");             // pH
        assertThat(html).contains("44.9");            // CEC
        assertThat(html).contains("SAR (sodium adsorption ratio)");
        // The optimum the lab printed, in the lab's own notation, beside each value.
        assertThat(html).contains("5.3 - 7.2");
        assertThat(html).contains("&lt; 19");           // soluble sodium's ceiling, no invented floor
        // Any verdict on the page says whose it is.
        assertThat(html).contains("our comparison against the lab's printed range");
        assertThat(html).contains("as printed by the lab");
        // ...and never claims a band or a position within one.
        assertThat(html).doesNotContain("Moderately");
        assertThat(html).doesNotContain("Very Low");
        // Hydrogen was below the lab's detection limit — shown as a bound, and the sum as a range.
        assertThat(html).contains("&lt; 1.00");
        assertThat(html).contains("hydrogen was below the detection limit");
    }

    /**
     * The lab's advice renders with "apply none" distinguishable from a row that is simply
     * missing, and the censored gypsum requirement as a bound rather than a measurement.
     */
    @Test
    void profile_rendersRecommendationsWithNoneDistinctFromAbsent() {
        NaturalistDatabase db = NaturalistDatabase.create();
        SoilProfile box1 = SoilTestContext.create(db).soilProfileQuery()
                .getBySoilProfileName(SoilProfileName.of("box1")).orElseThrow();
        StringOutput output = new StringOutput();

        TestTemplateEngine.create().render("soil/profile.jte",
                Map.of("profile", box1, "glossaryLinker", GlossaryLinker.none()), output);

        String html = output.toString();
        assertThat(html).contains("What the lab recommended");
        assertThat(html).contains("11.2");                       // potassium, an actual application
        assertThat(html).contains("advised-none");               // "apply none" is advice, styled as such
        assertThat(html).contains("recommended applying none");
        assertThat(html).contains("&lt; 0.50");                  // the censored gypsum requirement
        assertThat(html).contains("tons/AF");
    }

    @Test
    void profile_linksGlossaryTermsWithDefinitionPopovers() {
        NaturalistDatabase db = NaturalistDatabase.create();
        SoilProfile box1 = SoilTestContext.create(db).soilProfileQuery()
                .getBySoilProfileName(SoilProfileName.of("box1")).orElseThrow();
        GlossaryLinker glossaryLinker = GlossaryLinker.of(LibraryTestContext.create(db).glossaryTermQuery()
                .findPage(PageRequest.first(PageRequest.MAX_PAGE_SIZE)).content());
        StringOutput output = new StringOutput();

        TestTemplateEngine.create().render("soil/profile.jte",
                Map.of("profile", box1, "glossaryLinker", glossaryLinker), output);

        String html = output.toString();
        // Physical labels become definition popovers linking back to /glossary.
        assertThat(html).contains("class=\"glossary-link\"");
        assertThat(html).contains(">CEC</button>");
        assertThat(html).contains("/glossary/cec");
        assertThat(html).contains("/glossary/electrical-conductivity");
        // The one-time fraction legend links exchangeable and soluble once each.
        assertThat(html).contains(">exchangeable</button>");
        assertThat(html).contains(">soluble</button>");
    }

    /**
     * A nutrient the lab never reported must not read as a measurement. The panel below carries a
     * genuine zero for nitrate-N and nothing at all for every other row; the page has to say those
     * are different, or a reader will treat an unrun test as a depleted soil.
     */
    @Test
    void profile_rendersUnreportedNutrientDistinctlyFromReportedZero() {
        StringOutput output = new StringOutput();

        TestTemplateEngine.create().render("soil/profile.jte",
                Map.of("profile", shortPanelProfile(), "glossaryLinker", GlossaryLinker.none()), output);

        String html = output.toString();
        assertThat(html).contains("0.00");            // nitrate-N really was measured at zero
        assertThat(html).contains("not reported");    // ...every other row was not measured at all
        assertThat(html).contains("Phosphorus");      // the absent row keeps its label
        // No physical row on this analysis either — stated, not crashed on.
        assertThat(html).contains("No physical or derived measurements on record");
        assertThat(html).doesNotContain("meq/100g");
    }

    /** Synthetic — a structural fixture, not Oak Vista chemistry. */
    private static SoilProfile shortPanelProfile() {
        LabAnalysisId id = LabAnalysisId.create();
        NutrientPanel panel = new NutrientPanel(
                new PrimaryNutrients(
                        Optional.of(new NutrientReading(NutrientReadingId.create(), Nutrients.NITRATE_N,
                                id, new BigDecimal("0.00"), MeasurementUnit.LBS_PER_1000_SQFT)),
                        Optional.empty(), Optional.empty(), Optional.empty()),
                new SecondaryNutrients(Optional.empty(), Optional.empty(), Optional.empty(),
                        Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty()),
                new MicroNutrients(Optional.empty(), Optional.empty(), Optional.empty(),
                        Optional.empty(), Optional.empty(), Optional.empty()));
        LabAnalysisInfo info = new LabAnalysisInfo(id, SoilProfileName.of("box1"), CropName.of("lettuce"),
                LocalDate.of(2026, 8, 17), "SYNTHETIC LAB", "XX 0000000-000", null, null, null);
        return new SoilProfile(
                new SoilProfileInfo(SoilProfileName.of("box1"), ZoneName.of("box-1"), null),
                List.of(new LabAnalysis(info, panel, Optional.empty(),
                        ReportedOptimumCollection.empty(), ReportedRecommendationCollection.empty())));
    }
}
