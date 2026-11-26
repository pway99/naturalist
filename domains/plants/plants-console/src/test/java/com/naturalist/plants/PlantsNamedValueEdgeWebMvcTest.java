package com.naturalist.plants;

import com.naturalist.data.NaturalistTestExtension;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Path variables reach the handlers as named values (Spring converts them through {@code NamedValueConversion},
 * handing each on as given). A page whose name is not a valid slug is observed, counted under the parameter's
 * name ({@code plantName}, {@code orderName}, ...) and answered 404 without calling a query; a valid name the catalog does not hold is a 404 too,
 * counted under the looked-up value's own name ({@code plant}, {@code cultivar}, ...). Both are INFO: a signal that
 * a record has not been added, not a fault to fix.
 */
@WebMvcTest
class PlantsNamedValueEdgeWebMvcTest {

    @RegisterExtension
    static final NaturalistTestExtension database = PlantsControllerTestConfig.DATABASE;

    private static final String NOT_A_SLUG = "Aristolochia_Californica";

    @Autowired
    MockMvc mockMvc;

    SimpleMeterRegistry meters;

    @BeforeEach
    void addMeters() {
        meters = new SimpleMeterRegistry();
        Metrics.addRegistry(meters);
    }

    @AfterEach
    void removeMeters() {
        Metrics.removeRegistry(meters);
    }

    @Test
    void detail_nameNotASlug_isNotFound_andIsCounted() throws Exception {
        mockMvc.perform(get("/plants/" + NOT_A_SLUG))
                .andExpect(status().isNotFound())
                .andExpect(content().string(containsString("Not in the catalog")))
                .andExpect(content().string(containsString("href=\"/plants/species\"")));

        assertThat(violated("PlantsController.detail.plantName")).isEqualTo(1.0);
        assertThat(levelOf("PlantsController.detail.plantName")).as("a malformed name is a signal, not a fault")
                .isEqualTo("info");
        assertThat(violated("PlantsController.detail.plant")).as("no query ran").isZero();
    }

    @Test
    void detail_unknownName_isNotFound_andIsCounted() throws Exception {
        mockMvc.perform(get("/plants/unobtainium-vine"))
                .andExpect(status().isNotFound())
                .andExpect(content().string(containsString("unobtainium-vine")));

        assertThat(violated("PlantsController.detail.plant")).isEqualTo(1.0);
        assertThat(levelOf("PlantsController.detail.plant")).as("a plant not added yet is a signal, not a fault")
                .isEqualTo("info");
        assertThat(violated("PlantsController.detail.plantName")).isZero();
    }

    @Test
    void detail_missingSpeciesOfAKnownGenus_redirectsToTheGenus() throws Exception {
        mockMvc.perform(get("/plants/aristolochia-tomentosa"))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/plants/genera/aristolochia?missing=aristolochia-tomentosa"));
    }

    @Test
    void genusDetail_missing_saysTheSpeciesIsNotAddedYet() throws Exception {
        mockMvc.perform(get("/plants/genera/aristolochia").param("missing", "aristolochia-tomentosa"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("No record for")))
                .andExpect(content().string(containsString("aristolochia-tomentosa")));
    }

    @Test
    void genusDetail_missingNotASlug_isDropped() throws Exception {
        mockMvc.perform(get("/plants/genera/aristolochia").param("missing", NOT_A_SLUG))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("No record for"))));

        assertThat(violated("PlantsController.genusDetail.missing")).isEqualTo(1.0);
        assertThat(levelOf("PlantsController.genusDetail.missing")).as("a dropped parameter is a signal, not a fault")
                .isEqualTo("info");
    }

    @Test
    void genusDetail_withoutMissing_showsNoNote() throws Exception {
        mockMvc.perform(get("/plants/genera/aristolochia"))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("No record for"))));
    }

    @Test
    void detail_knownName_rendersThePage_andCountsNothing() throws Exception {
        mockMvc.perform(get("/plants/aristolochia-californica"))
                .andExpect(status().isOk());

        assertThat(violated("PlantsController.detail.plantName")).isZero();
        assertThat(violated("PlantsController.detail.plant")).isZero();
    }

    @Test
    void rankPages_nameNotASlug_areNotFound_andAreCounted() throws Exception {
        assertRefused("/plants/orders/Not_An_Order", "orderDetail", "plant", "/plants/orders");
        assertRefused("/plants/families/Not_A_Family", "familyDetail", "plant", "/plants/families");
        assertRefused("/plants/genera/Not_A_Genus", "genusDetail", "plant", "/plants/orders");
    }

    @Test
    void recordPages_nameNotASlug_areNotFound_andAreCounted() throws Exception {
        assertRefused("/plants/cultivars/Not_A_Cultivar", "cultivarDetail", "cultivar", "/plants/species");
        assertRefused("/plants/lineages/Not_A_Lineage", "lineageDetail", "lineage", "/plants/species");
        assertRefused("/plants/programs/Not_A_Program", "programDetail", "program", "/plants/species");
        assertRefused("/plants/phytochemistry/Not_A_Constituent", "constituentDetail", "constituent",
                "/plants/phytochemistry");
    }

    @Test
    void everyPage_unknownName_isNotFound_andIsCounted() throws Exception {
        assertUnknown("/plants/orders/unobtainium-ales", "orderDetail", "plant");
        assertUnknown("/plants/families/unobtainium-aceae", "familyDetail", "plant");
        assertUnknown("/plants/genera/unobtainium", "genusDetail", "plant");
        assertUnknown("/plants/cultivars/unobtainium-paste", "cultivarDetail", "cultivar");
        assertUnknown("/plants/lineages/unobtainium-line", "lineageDetail", "lineage");
        assertUnknown("/plants/programs/unobtainium-program", "programDetail", "program");
        assertUnknown("/plants/phytochemistry/unobtainium-oxide", "constituentDetail", "constituent");
    }

    private void assertRefused(String path, String endpoint, String found, String back) throws Exception {
        mockMvc.perform(get(path))
                .andExpect(status().isNotFound())
                .andExpect(content().string(containsString("href=\"" + back + "\"")));

        assertThat(violated("PlantsController." + endpoint + "." + endpoint.replace("Detail", "Name"))).as(endpoint).isEqualTo(1.0);
        assertThat(violated("PlantsController." + endpoint + "." + found)).as(endpoint + ": no query ran").isZero();
    }

    private void assertUnknown(String path, String endpoint, String found) throws Exception {
        mockMvc.perform(get(path)).andExpect(status().isNotFound());

        assertThat(violated("PlantsController." + endpoint + "." + found)).as(endpoint).isEqualTo(1.0);
    }

    /** The {@code level} tag the violated check {@code check} was counted under. */
    private String levelOf(String check) {
        return meters.find("naturalist.invariant.check").tag("name", check).tag("outcome", "violated")
                .counter().getId().getTag("level");
    }

    /**
     * Every violated check named {@code check} or nested under it ({@code check.<rule>}), summed: a refusal is tagged
     * with the rule it broke. Segment-bounded, so {@code detail.plant} does not count {@code detail.plantName}.
     */
    private double violated(String check) {
        return meters.find("naturalist.invariant.check").tag("outcome", "violated").counters().stream()
                .filter(counter -> {
                    String name = String.valueOf(counter.getId().getTag("name"));
                    return name.equals(check) || name.startsWith(check + ".");
                })
                .mapToDouble(Counter::count)
                .sum();
    }
}
