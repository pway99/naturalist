package com.naturalist.garden;

import com.naturalist.data.NaturalistDatabase;
import com.naturalist.data.PageRequest;
import gg.jte.output.StringOutput;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class GardenConsoleTemplateTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 8, 14);

    private static GardenTestContext context() {
        return GardenTestContext.create(NaturalistDatabase.create());
    }

    private static String render(String template, Map<String, Object> params) {
        StringOutput output = new StringOutput();
        TestTemplateEngine.create().render(template, params, output);
        return output.toString();
    }

    @Test
    void list_rendersEveryZoneSomethingWasPlantedIn() {
        GardenTestContext ctx = context();
        List<PlantedZone> plantedZones = ctx.plantingQuery().findPage(PageRequest.console(0)).content()
                .stream()
                .map(Planting::zoneName)
                .distinct()
                .map(zoneName -> ctx.plantedZoneQuery().getByZoneName(zoneName).orElseThrow())
                .toList();

        String html = render("garden/list.jte", Map.of("plantedZones", plantedZones, "today", TODAY));

        assertThat(html).contains("backyard");
        assertThat(html).contains("box-1");
        assertThat(html).contains("/garden/planted-zones/backyard");
    }

    @Test
    void plantedZone_rendersEveryPlantingWithItsVarietyAndDates() {
        PlantedZone backyard = context().plantedZoneQuery()
                .getByZoneName(com.naturalist.zone.ZoneName.of("backyard")).orElseThrow();

        String html = render("garden/plantedZone.jte", Map.of("planted", backyard, "today", TODAY));

        assertThat(html).contains("solanum-lycopersicum");
        assertThat(html).contains("amish-paste");
        assertThat(html).contains("2026-04-06");
        assertThat(html).contains("2026-08-10");
        // The eggplant shares the south row with the tomatoes — a mixed row, rendered as such.
        assertThat(html).contains("solanum-melongena");
        assertThat(html).contains("/garden/planted-zones/backyard/backyard-south");
    }

    /**
     * A planting whose variety was never recorded reads as "not recorded", visibly distinct from a
     * variety that exists — the same absent-is-not-a-value rule the soil console follows.
     */
    @Test
    void plantedZone_rendersAnUnrecordedVarietyAsAbsentRatherThanBlank() {
        PlantedZone box1 = context().plantedZoneQuery()
                .getByZoneName(com.naturalist.zone.ZoneName.of("box-1")).orElseThrow();

        String html = render("garden/plantedZone.jte", Map.of("planted", box1, "today", TODAY));

        assertThat(html).contains("raphanus-sativus");
        assertThat(html).contains("not recorded");
        assertThat(html).contains("never recorded");   // the title explaining the distinction
        assertThat(html).contains("still growing");    // the basil and parsley are in
    }

    /** A row view narrows to one subdivision and says which zone it belongs to. */
    @Test
    void row_rendersOnlyThatRow() {
        PlantedZone southRow = context().plantedZoneQuery()
                .getBySubZoneName(com.naturalist.zone.ZoneName.of("backyard"),
                        com.naturalist.zone.subzone.SubZoneName.of("backyard-south"))
                .orElseThrow();

        String html = render("garden/plantedZone.jte", Map.of("planted", southRow, "today", TODAY));

        assertThat(html).contains("backyard-south");
        assertThat(html).contains("solanum-melongena");
        // ...and not the north row's San Marzano.
        assertThat(html).doesNotContain("san-marzano-f2");
    }
}
