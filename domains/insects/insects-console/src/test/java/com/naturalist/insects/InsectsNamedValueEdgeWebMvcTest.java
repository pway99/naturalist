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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Path variables and request parameters reach the handlers as named values (Spring converts them through
 * {@code NamedValueConversion}, handing each on as given). An endpoint observes a name that is not a valid slug,
 * or an observation id that is missing, unreadable or not a UUIDv7, counts it under its own name, and redirects
 * as it does for an unknown one, without calling a query or command.
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
    void detail_nameNotASlug_redirectsToTheList_andIsCounted() throws Exception {
        mockMvc.perform(get("/insects/" + NOT_A_SLUG))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/insects"));

        assertThat(violated("InsectsController.detail.name")).isEqualTo(1.0);
    }

    @Test
    void rankPages_nameNotASlug_redirectToTheirList_andAreCounted() throws Exception {
        mockMvc.perform(get("/insects/families/Not_A_Family")).andExpect(redirectedUrl("/insects/families"));
        mockMvc.perform(get("/insects/orders/Not_An_Order")).andExpect(redirectedUrl("/insects/orders"));
        mockMvc.perform(get("/insects/genera/Not_A_Genus")).andExpect(redirectedUrl("/insects/genera"));
        mockMvc.perform(get("/insects/" + NOT_A_SLUG + "/life-stages")).andExpect(redirectedUrl("/insects"));

        assertThat(violated("InsectsController.familyDetail.name")).isEqualTo(1.0);
        assertThat(violated("InsectsController.orderDetail.name")).isEqualTo(1.0);
        assertThat(violated("InsectsController.genusDetail.name")).isEqualTo(1.0);
        assertThat(violated("InsectsController.lifeStages.name")).isEqualTo(1.0);
    }

    @Test
    void observe_nameNotASlug_redirectsToTheList_andRecordsNothing() throws Exception {
        mockMvc.perform(post("/insects/" + NOT_A_SLUG + "/observe").requestAttr(CURRENT_NATURALIST, ME.value()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/insects"));

        assertThat(violated("InsectsController.observe.name")).isEqualTo(1.0);
        assertThat(violated("InsectSightingCommandImpl.record")).as("the command was never called").isZero();
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
                .andExpect(redirectedUrl("/insects/battus-philenor"));

        assertThat(violated("InsectsController.updateNotes.id")).isEqualTo(1.0);
    }

    @Test
    void updateNotes_validId_updatesTheNotes() throws Exception {
        var id = InsectsControllerTestConfig.INSECTS.insectCommand().sightings().record(SPECIES, ME, "old");

        mockMvc.perform(post("/insects/battus-philenor/notes")
                        .param("observationId", id.value().toString())
                        .param("notes", "new")
                        .requestAttr(CURRENT_NATURALIST, ME.value()))
                .andExpect(redirectedUrl("/insects/battus-philenor"));

        assertThat(InsectsControllerTestConfig.INSECTS.insectQuery().observations().getByName(id)
                .orElseThrow().notes()).isEqualTo("new");
        assertThat(violated("InsectsController.updateNotes.id")).isZero();
    }

    private void assertIdRefused(String observationId) throws Exception {
        mockMvc.perform(post("/insects/battus-philenor/notes")
                        .param("observationId", observationId)
                        .param("notes", "new")
                        .requestAttr(CURRENT_NATURALIST, ME.value()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/insects/battus-philenor"));

        assertThat(violated("InsectsController.updateNotes.id")).isEqualTo(1.0);
        assertThat(violated("InsectSightingCommandImpl.updateNotes")).as("the command was never called").isZero();
    }

    /** Every violated check under {@code prefix}, summed: a refusal is tagged with the rule it broke. */
    private double violated(String prefix) {
        return meters.find("naturalist.invariant.check").tag("outcome", "violated").counters().stream()
                .filter(counter -> String.valueOf(counter.getId().getTag("name")).startsWith(prefix))
                .mapToDouble(Counter::count)
                .sum();
    }
}
