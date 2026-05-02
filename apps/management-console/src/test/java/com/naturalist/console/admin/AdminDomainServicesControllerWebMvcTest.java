package com.naturalist.console.admin;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end render + auth assertions for {@code /admin/domain-services}
 * (admin-console plan view 2, M2).
 *
 * <p>Anchors the acceptance criteria from
 * {@code docs/plans/admin-console.md}: an ADMIN-authenticated GET renders
 * a 200 page that lists every {@code @DomainService} bean under its
 * domain heading; an anonymous GET is bounced to the login page by the
 * existing security chain. Picks {@code ChemistryDomain},
 * {@code InsectsDomain}, and {@code PlantsDomain} as the
 * representative-bean-per-domain probe — each is a stable, plan-anchored
 * {@code DomainId} record under {@code com.naturalist.<domain>}, so the
 * grouping is exercised end-to-end. Asserts absence of an
 * {@code infrastructure} heading to confirm the kernel marker class
 * itself is not mistaken for a domain.
 */
@SpringBootTest
class AdminDomainServicesControllerWebMvcTest {

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
    void anonymous_domainServices_redirectsToLogin() throws Exception {
        mockMvc.perform(get("/admin/domain-services"))
                .andExpect(status().is3xxRedirection())
                .andExpect(result ->
                        assertThat(result.getResponse().getRedirectedUrl()).contains("/login"));
    }

    @Test
    void admin_domainServices_rendersOk() throws Exception {
        mockMvc.perform(get("/admin/domain-services").with(user("naturalist").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/html"));
    }

    @Test
    void admin_domainServices_listsRepresentativeBeanUnderItsDomain() throws Exception {
        var html = mockMvc.perform(get("/admin/domain-services")
                        .with(user("naturalist").roles("ADMIN")))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        var chemistryAt = html.indexOf(">chemistry</h2>");
        var insectsAt = html.indexOf(">insects</h2>");
        var plantsAt = html.indexOf(">plants</h2>");

        assertThat(chemistryAt).as("chemistry heading").isGreaterThan(-1);
        assertThat(insectsAt).as("insects heading, ordered after chemistry").isGreaterThan(chemistryAt);
        assertThat(plantsAt).as("plants heading, ordered after insects").isGreaterThan(insectsAt);

        var chemistrySection = html.substring(chemistryAt, insectsAt);
        var insectsSection = html.substring(insectsAt, plantsAt);
        var plantsSection = html.substring(plantsAt);

        assertThat(chemistrySection)
                .contains("ChemistryDomain")
                .contains("com.naturalist.chemistry.ChemistryDomain");
        assertThat(insectsSection)
                .contains("InsectsDomain")
                .contains("com.naturalist.insects.InsectsDomain");
        assertThat(plantsSection)
                .contains("PlantsDomain")
                .contains("com.naturalist.plants.PlantsDomain");
    }

    @Test
    void admin_domainServices_omitsHeadingForPackagesWithNoDomainServices() throws Exception {
        var html = mockMvc.perform(get("/admin/domain-services")
                        .with(user("naturalist").roles("ADMIN")))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        // The kernel @DomainService marker lives under com.naturalist.infrastructure
        // but no class in that package carries the marker. The grouping logic must
        // not invent a heading from the marker's own package.
        assertThat(html).doesNotContain(">infrastructure</h2>");
        assertThat(html).doesNotContain(">framework</h2>");
        assertThat(html).doesNotContain(">console</h2>");
    }
}
