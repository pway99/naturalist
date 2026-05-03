package com.naturalist.console;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end smoke test for the {@code /search} discovery surface
 * (M-Search-UI-B in {@code docs/plans/catalog-kernel-redirect.md}).
 */
@SpringBootTest
class SearchControllerWebMvcTest {

    @Autowired
    WebApplicationContext context;

    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    @Test
    void search_withoutQuery_rendersFormWithoutHits() throws Exception {
        mockMvc.perform(get("/search").with(user("naturalist").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/html"))
                .andExpect(content().string(containsString("<form action=\"/search\"")))
                .andExpect(content().string(not(containsString("search-empty-state"))))
                .andExpect(content().string(not(containsString("search-domain-group"))));
    }

    @Test
    void search_forKnownGenus_returnsPlantHits() throws Exception {
        mockMvc.perform(get("/search").param("q", "aristolochia")
                        .with(user("naturalist").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Plants")))
                .andExpect(content().string(containsString("/plants/california-pipevine")));
    }

    @Test
    void search_forAmbiguousGenus_returnsBothTrifoliumSpecies() throws Exception {
        mockMvc.perform(get("/search").param("q", "trifolium")
                        .with(user("naturalist").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("/plants/crimson-clover")))
                .andExpect(content().string(containsString("/plants/white-clover")));
    }

    @Test
    void search_forUnknownTerm_rendersEmptyState() throws Exception {
        mockMvc.perform(get("/search").param("q", "zzz-no-such-thing")
                        .with(user("naturalist").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("search-empty-state")))
                .andExpect(content().string(containsString("zzz-no-such-thing")));
    }

    @Test
    void search_anonymous_redirectsToLogin() throws Exception {
        mockMvc.perform(get("/search").param("q", "aristolochia"))
                .andExpect(status().is3xxRedirection())
                .andExpect(result ->
                        assertThat(result.getResponse().getRedirectedUrl()).contains("/login"));
    }
}
