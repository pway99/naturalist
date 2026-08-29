package com.naturalist.insects;

import com.naturalist.data.NaturalistTestExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The collection-lens toggle endpoint persists the lens in the session and
 * constrains its {@code return} target to this console. A DB-free, scan-free,
 * security-free {@code @WebMvcTest} slice ({@link InsectsControllerTestConfig});
 * the current naturalist is supplied through the
 * {@code naturalist.currentNaturalistName} request attribute (no Spring Security,
 * so no CSRF filter).
 */
@WebMvcTest
class CollectionLensEndpointWebMvcTest {

    @RegisterExtension
    static final NaturalistTestExtension database = InsectsControllerTestConfig.DATABASE;

    private static final String CURRENT_NATURALIST = "naturalist.currentNaturalistName";

    @Autowired
    MockMvc mockMvc;

    @Test
    void toggleOn_persistsInSession_andFiltersNextRequest() throws Exception {
        MockHttpSession session = new MockHttpSession();
        mockMvc.perform(post("/insects/collection-lens").param("on", "true")
                        .param("return", "/insects/species")
                        .session(session).requestAttr(CURRENT_NATURALIST, "patrick-way"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/insects/species"));

        mockMvc.perform(get("/insects/species").session(session)
                        .requestAttr(CURRENT_NATURALIST, "patrick-way"))
                .andExpect(content().string(not(containsString("apis-mellifera"))));
    }

    @Test
    void toggle_rejectsOffsiteReturn() throws Exception {
        mockMvc.perform(post("/insects/collection-lens").param("on", "true")
                        .param("return", "https://evil.example/phish")
                        .requestAttr(CURRENT_NATURALIST, "patrick-way"))
                .andExpect(redirectedUrl("/insects/species"));
    }
}
