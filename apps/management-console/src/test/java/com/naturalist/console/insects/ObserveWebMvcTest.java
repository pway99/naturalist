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

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class ObserveWebMvcTest {

    @Autowired
    WebApplicationContext context;
    MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    private static UsernamePasswordAuthenticationToken flora() {
        var p = new NaturalistPrincipal(NaturalistName.of("flora-mendez"), "Flora", "{bcrypt}x");
        return new UsernamePasswordAuthenticationToken(p, "n/a", p.getAuthorities());
    }

    @Test
    void observe_thenMine_includesTheSpecies() throws Exception {
        mockMvc.perform(post("/insects/battus-philenor/observe").param("notes", "seen today")
                        .with(authentication(flora())).with(csrf()))
                .andExpect(status().is3xxRedirection());

        mockMvc.perform(get("/insects/species").sessionAttr("insects.collectionLens", true)
                        .with(authentication(flora())))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("battus-philenor")));
    }
}
