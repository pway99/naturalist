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
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class CollectionToggleWebMvcTest {

    @Autowired
    WebApplicationContext context;
    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    private static UsernamePasswordAuthenticationToken patrick() {
        var p = new NaturalistPrincipal(NaturalistName.of("patrick-way"), "Patrick", "{bcrypt}x");
        return new UsernamePasswordAuthenticationToken(p, "n/a", p.getAuthorities());
    }

    @Test
    void toggle_showsOnInsectsForNaturalist() throws Exception {
        mockMvc.perform(get("/insects/species").with(authentication(patrick())))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("action=\"/insects/collection-lens\"")))
                .andExpect(content().string(containsString("My collection")));
    }

    @Test
    void toggle_hiddenOffInsects() throws Exception {
        mockMvc.perform(get("/").with(authentication(patrick())))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("action=\"/insects/collection-lens\""))));
    }

    @Test
    void toggle_hiddenForAdmin() throws Exception {
        mockMvc.perform(get("/insects/species").with(user("test-admin").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("action=\"/insects/collection-lens\""))));
    }

    @Test
    void emptyCollection_showsNudge() throws Exception {
        var flora = new NaturalistPrincipal(NaturalistName.of("flora-mendez"), "Flora", "{bcrypt}x");
        var floraAuth = new UsernamePasswordAuthenticationToken(flora, "n/a", flora.getAuthorities());
        mockMvc.perform(get("/insects/species")
                        .sessionAttr("insects.collectionLens", true)
                        .with(authentication(floraAuth)))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("No insects in your collection yet")));
    }
}
