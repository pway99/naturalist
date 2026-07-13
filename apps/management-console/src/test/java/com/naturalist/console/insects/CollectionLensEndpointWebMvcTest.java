package com.naturalist.console.insects;

import com.naturalist.naturalist.NaturalistName;
import com.naturalist.console.auth.NaturalistPrincipal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class CollectionLensEndpointWebMvcTest {

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
    void toggleOn_persistsInSession_andFiltersNextRequest() throws Exception {
        MockHttpSession session = new MockHttpSession();
        mockMvc.perform(post("/insects/collection-lens").param("on", "true")
                        .param("return", "/insects/species")
                        .session(session).with(authentication(patrick())).with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/insects/species"));

        mockMvc.perform(get("/insects/species").session(session).with(authentication(patrick())))
                .andExpect(content().string(not(containsString("apis-mellifera"))));
    }

    @Test
    void toggle_rejectsOffsiteReturn() throws Exception {
        mockMvc.perform(post("/insects/collection-lens").param("on", "true")
                        .param("return", "https://evil.example/phish")
                        .with(authentication(patrick())).with(csrf()))
                .andExpect(redirectedUrl("/insects/species"));
    }
}
