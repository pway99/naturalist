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
 * The species detail page shows an "In your collection" badge for a species the
 * signed-in naturalist has observed, and omits it otherwise. A DB-free,
 * scan-free, security-free {@code @WebMvcTest} slice ({@link InsectsControllerTestConfig}):
 * the current naturalist is the {@code naturalist.currentNaturalistName} request
 * attribute, and {@code patrick-way}'s battus-philenor observation is seeded in
 * {@code field-observations.json}.
 */
@WebMvcTest
class CollectedIndicatorWebMvcTest {

    @RegisterExtension
    static final NaturalistTestExtension database = InsectsControllerTestConfig.DATABASE;

    private static final String CURRENT_NATURALIST = "naturalist.currentNaturalistName";

    @Autowired
    MockMvc mockMvc;

    @Test
    void detail_showsCollectedBadge_forObservedSpecies() throws Exception {
        mockMvc.perform(get("/insects/battus-philenor").requestAttr(CURRENT_NATURALIST, "patrick-way"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("In your collection")));
    }

    @Test
    void detail_omitsCollectedBadge_forUnobservedSpecies() throws Exception {
        mockMvc.perform(get("/insects/apis-mellifera").requestAttr(CURRENT_NATURALIST, "patrick-way"))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("In your collection"))));
    }
}
