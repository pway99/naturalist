package com.naturalist.soil.console;

import com.naturalist.data.NaturalistDatabase;
import com.naturalist.data.PageRequest;
import com.naturalist.soil.SoilProfile;
import com.naturalist.soil.SoilProfileName;
import com.naturalist.soil.SoilTestContext;
import gg.jte.output.StringOutput;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

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
        SoilProfile box1 = context().soilProfileQuery()
                .getBySoilProfileName(SoilProfileName.of("box1")).orElseThrow();
        StringOutput output = new StringOutput();

        TestTemplateEngine.create().render("soil/profile.jte", Map.of("profile", box1), output);

        String html = output.toString();
        assertThat(html).contains("Nitrate-N");
        assertThat(html).contains("6.99");           // soluble calcium
        assertThat(html).contains("lbs/1000 ft²");    // MeasurementUnit.symbol()
        assertThat(html).contains("7.2");             // pH
        assertThat(html).contains("44.9");            // CEC
    }
}
