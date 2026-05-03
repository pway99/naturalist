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
 * End-to-end render + auth assertions for {@code /admin/catalog}
 * (admin-console plan view 3, M2).
 *
 * <p>Anchors the acceptance criteria from
 * {@code docs/plans/admin-console.md}: an ADMIN-authenticated GET renders
 * a 200 page that lists every {@code CatalogContribution} and
 * {@code EntityReferences} provider under its {@code DomainId} heading;
 * an anonymous GET is bounced to the login page by the existing security
 * chain. Plants is the only domain with a wired contribution and provider
 * today ({@code PlantCatalogContribution}, {@code PlantCompoundReferences}
 * for {@code CompoundName}); chemistry and insects are registered domains
 * with no current contribution or provider, so they exercise the
 * "registered domain renders an empty section" branch the milestone plan
 * calls out.
 */
@SpringBootTest
class AdminCatalogControllerWebMvcTest {

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
    void anonymous_catalog_redirectsToLogin() throws Exception {
        mockMvc.perform(get("/admin/catalog"))
                .andExpect(status().is3xxRedirection())
                .andExpect(result ->
                        assertThat(result.getResponse().getRedirectedUrl()).contains("/login"));
    }

    @Test
    void admin_catalog_rendersOk() throws Exception {
        mockMvc.perform(get("/admin/catalog").with(user("naturalist").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/html"));
    }

    @Test
    void admin_catalog_groupsByRegisteredDomainSlug() throws Exception {
        var html = render();

        var chemistryAt = html.indexOf(">chemistry</h2>");
        var insectsAt = html.indexOf(">insects</h2>");
        var plantsAt = html.indexOf(">plants</h2>");

        assertThat(chemistryAt).as("chemistry heading").isGreaterThan(-1);
        assertThat(insectsAt).as("insects heading, ordered after chemistry").isGreaterThan(chemistryAt);
        assertThat(plantsAt).as("plants heading, ordered after insects").isGreaterThan(insectsAt);
    }

    @Test
    void admin_catalog_listsContributionAndProviderUnderPlants() throws Exception {
        var html = render();

        var plantsAt = html.indexOf(">plants</h2>");
        assertThat(plantsAt).isGreaterThan(-1);
        var plantsSection = html.substring(plantsAt);

        assertThat(plantsSection)
                .contains("PlantCatalogContribution")
                .contains("com.naturalist.plants.catalog.PlantCatalogContribution");
        assertThat(plantsSection)
                .contains("PlantCompoundReferences")
                .contains("com.naturalist.plants.catalog.PlantCompoundReferences")
                .contains("CompoundName");
    }

    @Test
    void admin_catalog_rendersEmptySectionsForDomainsWithNeitherContributionNorProvider() throws Exception {
        var html = render();

        var chemistryAt = html.indexOf(">chemistry</h2>");
        var insectsAt = html.indexOf(">insects</h2>");
        var plantsAt = html.indexOf(">plants</h2>");

        assertThat(chemistryAt).isGreaterThan(-1);
        assertThat(insectsAt).isGreaterThan(chemistryAt);

        var chemistrySection = html.substring(chemistryAt, insectsAt);
        var insectsSection = html.substring(insectsAt, plantsAt);

        assertThat(chemistrySection)
                .contains("No contributions registered.")
                .contains("No reference providers registered.");
    }

    @Test
    void admin_catalog_navMarksCatalogAsCurrent() throws Exception {
        var html = render();

        assertThat(html).contains("href=\"/admin/catalog\" aria-current=\"page\"");
    }

    private String render() throws Exception {
        return mockMvc.perform(get("/admin/catalog")
                        .with(user("naturalist").roles("ADMIN")))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
    }
}
