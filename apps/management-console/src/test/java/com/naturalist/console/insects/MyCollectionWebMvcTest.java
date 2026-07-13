package com.naturalist.console.insects;

import com.naturalist.naturalist.NaturalistName;
import com.naturalist.console.auth.NaturalistPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class MyCollectionWebMvcTest {

    @Autowired
    WebApplicationContext context;
    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    private static UsernamePasswordAuthenticationToken as(String slug, String given) {
        var p = new NaturalistPrincipal(NaturalistName.of(slug), given, "{bcrypt}x");
        return new UsernamePasswordAuthenticationToken(p, "n/a", p.getAuthorities());
    }

    @Test
    void lensOn_filtersToObservedSpecies() throws Exception {
        mockMvc.perform(get("/insects/species")
                        .sessionAttr("insects.collectionLens", true)
                        .with(authentication(as("patrick-way", "Patrick"))))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("battus-philenor")))
                .andExpect(content().string(not(containsString("apis-mellifera"))));
    }

    @Test
    void lensOff_showsFullCatalog() throws Exception {
        mockMvc.perform(get("/insects/species").with(authentication(as("patrick-way", "Patrick"))))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("apis-mellifera")));
    }

    @Test
    void mineParam_isIgnored_afterRetirement() throws Exception {
        // ?mine=true must no longer filter — only the session lens does.
        mockMvc.perform(get("/insects/species").param("mine", "true")
                        .with(authentication(as("patrick-way", "Patrick"))))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("apis-mellifera")));
    }
}
