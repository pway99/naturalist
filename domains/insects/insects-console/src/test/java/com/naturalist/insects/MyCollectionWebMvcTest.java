package com.naturalist.insects;

import com.naturalist.data.NaturalistTestExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The species listing filters to the signed-in naturalist's observed species when
 * the collection lens is on, and shows the full catalog otherwise. A DB-free,
 * scan-free, security-free {@code @WebMvcTest} slice ({@link InsectsControllerTestConfig});
 * the current naturalist is the {@code naturalist.currentNaturalistName} request
 * attribute. {@code patrick-way} has observed battus-philenor but not apis-mellifera.
 */
@WebMvcTest
class MyCollectionWebMvcTest {

    @RegisterExtension
    static final NaturalistTestExtension database = InsectsControllerTestConfig.DATABASE;

    private static final String CURRENT_NATURALIST = "naturalist.currentNaturalistName";

    @Autowired
    MockMvc mockMvc;

    @Test
    void lensOn_filtersToObservedSpecies() throws Exception {
        mockMvc.perform(get("/insects/species")
                        .sessionAttr("insects.collectionLens", true)
                        .requestAttr(CURRENT_NATURALIST, "patrick-way"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("battus-philenor")))
                .andExpect(content().string(not(containsString("apis-mellifera"))));
    }

    @Test
    void lensOff_showsFullCatalog() throws Exception {
        mockMvc.perform(get("/insects/species").requestAttr(CURRENT_NATURALIST, "patrick-way"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("apis-mellifera")));
    }

    @Test
    void mineParam_isIgnored_afterRetirement() throws Exception {
        // ?mine=true must no longer filter -- only the session lens does.
        mockMvc.perform(get("/insects/species").param("mine", "true")
                        .requestAttr(CURRENT_NATURALIST, "patrick-way"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("apis-mellifera")));
    }

    @Test
    void emptyCollection_showsNudge() throws Exception {
        // A naturalist with no seeded observations, lens on -> the empty-collection nudge.
        mockMvc.perform(get("/insects/species")
                        .sessionAttr("insects.collectionLens", true)
                        .requestAttr(CURRENT_NATURALIST, "empty-collector"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("No insects in your collection yet")));
    }
}
