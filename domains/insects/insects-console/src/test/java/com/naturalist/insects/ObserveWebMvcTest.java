package com.naturalist.insects;

import com.naturalist.data.NaturalistTestExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Recording a sighting (no photo) adds the species to the naturalist's
 * collection: a subsequent lens-on listing includes it. A DB-free, scan-free,
 * security-free {@code @WebMvcTest} slice ({@link InsectsControllerTestConfig});
 * the current naturalist is the {@code naturalist.currentNaturalistName} request
 * attribute (no Spring Security, so no CSRF filter). {@code flora-mendez} starts
 * with no seeded observations, so the write is the only source of the species.
 */
@WebMvcTest
class ObserveWebMvcTest {

    @RegisterExtension
    static final NaturalistTestExtension database = InsectsControllerTestConfig.DATABASE;

    private static final String CURRENT_NATURALIST = "naturalist.currentNaturalistName";

    @Autowired
    MockMvc mockMvc;

    @Test
    void observe_thenMine_includesTheSpecies() throws Exception {
        mockMvc.perform(post("/insects/battus-philenor/observe").param("notes", "seen today")
                        .requestAttr(CURRENT_NATURALIST, "flora-mendez"))
                .andExpect(status().is3xxRedirection());

        mockMvc.perform(get("/insects/species").sessionAttr("insects.collectionLens", true)
                        .requestAttr(CURRENT_NATURALIST, "flora-mendez"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("battus-philenor")));
    }
}
