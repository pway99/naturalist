package com.naturalist.insects;

import com.naturalist.data.NaturalistTestExtension;
import com.naturalist.naturalist.NaturalistName;
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

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Path variables and request parameters reach the handlers as named values (Spring converts them through
 * {@code NamedValueConversion}, handing each on as given). An endpoint observes a name that is not a valid slug,
 * or an observation id that is missing, unreadable or not a UUIDv7, counts it under its own name and says so,
 * without calling a query or command: a page answers 404 (as it does, counted, for a valid name the catalog does
 * not hold), and a post redirects with an error the target page shows.
 */
@WebMvcTest
class InsectsNamedValueEdgeWebMvcTest {

    @RegisterExtension
    static final NaturalistTestExtension database = InsectsControllerTestConfig.DATABASE;

    private static final String CURRENT_NATURALIST = "naturalist.currentNaturalistName";

    private static final NaturalistName ME = NaturalistName.of("pat-way");

    private static final InsectSpeciesName SPECIES = InsectSpeciesName.of("battus-philenor");

    private static final String NOT_A_SLUG = "Battus_Philenor";

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
        mockMvc.perform(get("/insects/" + NOT_A_SLUG))
                .andExpect(status().isNotFound())
                .andExpect(content().string(containsString("Not in the catalog")))
                .andExpect(content().string(containsString("href=\"/insects/species\"")));

        assertThat(violated("InsectsController.detail.name")).isEqualTo(1.0);
        assertThat(violated("InsectsController.detail.nameFound")).as("no query ran").isZero();
    }

    @Test
    void detail_unknownName_isNotFound_andIsCounted() throws Exception {
        mockMvc.perform(get("/insects/unobtainium-moth"))
                .andExpect(status().isNotFound())
                .andExpect(content().string(containsString("unobtainium-moth")));

        assertThat(violated("InsectsController.detail.nameFound")).isEqualTo(1.0);
        assertThat(violated("InsectsController.detail.name.")).isZero();
    }

    @Test
    void rankPages_nameNotASlug_areNotFound_andAreCounted() throws Exception {
        mockMvc.perform(get("/insects/families/Not_A_Family")).andExpect(status().isNotFound())
                .andExpect(content().string(containsString("href=\"/insects/families\"")));
        mockMvc.perform(get("/insects/orders/Not_An_Order")).andExpect(status().isNotFound())
                .andExpect(content().string(containsString("href=\"/insects/orders\"")));
        mockMvc.perform(get("/insects/genera/Not_A_Genus")).andExpect(status().isNotFound())
                .andExpect(content().string(containsString("href=\"/insects/genera\"")));
        mockMvc.perform(get("/insects/" + NOT_A_SLUG + "/life-stages")).andExpect(status().isNotFound());

        assertThat(violated("InsectsController.familyDetail.name")).isEqualTo(1.0);
        assertThat(violated("InsectsController.orderDetail.name")).isEqualTo(1.0);
        assertThat(violated("InsectsController.genusDetail.name")).isEqualTo(1.0);
        assertThat(violated("InsectsController.lifeStages.name")).isEqualTo(1.0);
    }

    @Test
    void observe_nameNotASlug_redirectsToTheListWithAnError_andRecordsNothing() throws Exception {
        mockMvc.perform(post("/insects/" + NOT_A_SLUG + "/observe").requestAttr(CURRENT_NATURALIST, ME.value()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/insects/species?error=name"));

        assertThat(violated("InsectsController.observe.name")).isEqualTo(1.0);
        assertThat(violated("InsectSightingCommandImpl.record")).as("the command was never called").isZero();
        mockMvc.perform(get("/insects/species?error=name"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("not the name of an insect in the catalog")));
    }

    @Test
    void updateNotes_malformedId_redirectsBack_andIsCounted() throws Exception {
        assertIdRefused("not-a-uuid");
    }

    @Test
    void updateNotes_nonV7Id_redirectsBack_andIsCounted() throws Exception {
        assertIdRefused(UUID.nameUUIDFromBytes(new byte[]{1}).toString());
    }

    @Test
    void updateNotes_missingId_redirectsBack_andIsCounted() throws Exception {
        mockMvc.perform(post("/insects/battus-philenor/notes")
                        .param("notes", "new")
                        .requestAttr(CURRENT_NATURALIST, ME.value()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/insects/battus-philenor?error=observation"));

        assertThat(violated("InsectsController.updateNotes.observationId")).isEqualTo(1.0);
    }

    @Test
    void updateNotes_validId_updatesTheNotes() throws Exception {
        InsectObservationId id = InsectsControllerTestConfig.INSECTS.insectCommand().sightings().record(SPECIES, ME, "old");

        mockMvc.perform(post("/insects/battus-philenor/notes")
                        .param("observationId", id.value().toString())
                        .param("notes", "new")
                        .requestAttr(CURRENT_NATURALIST, ME.value()))
                .andExpect(redirectedUrl("/insects/battus-philenor"));

        assertThat(InsectsControllerTestConfig.INSECTS.insectQuery().observations().getByName(id)
                .orElseThrow().notes()).isEqualTo("new");
        assertThat(violated("InsectsController.updateNotes.observationId")).isZero();
    }

    private void assertIdRefused(String observationId) throws Exception {
        mockMvc.perform(post("/insects/battus-philenor/notes")
                        .param("observationId", observationId)
                        .param("notes", "new")
                        .requestAttr(CURRENT_NATURALIST, ME.value()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/insects/battus-philenor?error=observation"));

        assertThat(violated("InsectsController.updateNotes.observationId")).isEqualTo(1.0);
        assertThat(violated("InsectSightingCommandImpl.updateNotes")).as("the command was never called").isZero();
    }

    @Test
    void rankPages_renderTheObservationError() throws Exception {
        mockMvc.perform(get("/insects/battus-philenor?error=observation").requestAttr(CURRENT_NATURALIST, ME.value()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("That observation could not be found; nothing was saved.")));
        // the message sits in the photo gallery, beside the notes form it answers; the seed family has no photos
        // at its own rank, so the page only has to render with the parameter
        mockMvc.perform(get("/insects/families/papilionidae?error=observation").requestAttr(CURRENT_NATURALIST, ME.value()))
                .andExpect(status().isOk());
    }

    /** Every violated check under {@code prefix}, summed: a refusal is tagged with the rule it broke. */
    private double violated(String prefix) {
        return meters.find("naturalist.invariant.check").tag("outcome", "violated").counters().stream()
                .filter(counter -> String.valueOf(counter.getId().getTag("name")).startsWith(prefix))
                .mapToDouble(Counter::count)
                .sum();
    }
}
