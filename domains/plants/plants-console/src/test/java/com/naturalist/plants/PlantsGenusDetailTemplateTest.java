package com.naturalist.plants;

import com.naturalist.spring.console.ConsoleSliceTemplates;

import com.naturalist.data.NaturalistTestExtension;
import gg.jte.output.StringOutput;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Smoke test for {@code plants/genera/detail.jte}. Renders every catalogued
 * genus, with and without a resolvable parent family, so the nullable
 * {@code family} branch executes.
 */
class PlantsGenusDetailTemplateTest {

    @RegisterExtension
    private final NaturalistTestExtension nte = NaturalistTestExtension.create();

    @Test
    void genusDetail_rendersEveryGenusWithItsFamily() {
        var template = ConsoleSliceTemplates.create();
        Map<String, PlantFamily> families = new HashMap<>();
        nte.getNamed(PlantFamilyTestEntitySource.class).entityStream()
                .forEach(f -> families.put(f.name().value(), f));

        for (PlantGenus genus : nte.getNamed(PlantGenusTestEntitySource.class).entityStream().toList()) {
            Map<String, Object> params = new HashMap<>();
            params.put("genus", genus);
            params.put("family", families.get(genus.familyName().value()));
            StringOutput output = new StringOutput();
            template.render("plants/genera/detail.jte", params, output);
            assertThat(output.toString())
                    .as("rendered output for %s", genus.name().value())
                    .isNotBlank();
        }
    }

    @Test
    void genusDetail_linksUpToItsParentFamily() {
        var template = ConsoleSliceTemplates.create();
        PlantGenus thymus = nte.getNamed(PlantGenusTestEntitySource.class).entityStream()
                .filter(g -> g.name().value().equals("thymus"))
                .findFirst()
                .orElseThrow();

        Map<String, Object> params = new HashMap<>();
        params.put("genus", thymus);
        params.put("family", null);
        StringOutput output = new StringOutput();
        template.render("plants/genera/detail.jte", params, output);

        // The upward link works off the genus's own typed FK, so it renders
        // even when the parent record was not loaded.
        assertThat(output.toString()).contains("/plants/families/lamiaceae");
    }

    @Test
    void genusDetail_rendersMemberSpeciesWhenPresent() {
        var template = ConsoleSliceTemplates.create();
        PlantGenus trifolium = nte.getNamed(PlantGenusTestEntitySource.class).entityStream()
                .filter(g -> g.name().value().equals("trifolium"))
                .findFirst()
                .orElseThrow();
        List<PlantTaxonView> children = nte.getNamed(PlantSpeciesTestEntitySource.class).entityStream()
                .filter(s -> trifolium.name().equals(s.genusName()))
                .map(s -> (PlantTaxonView) PlantTaxonView.SpeciesView.of(s))
                .toList();

        Map<String, Object> params = new HashMap<>();
        params.put("genus", trifolium);
        params.put("family", null);
        params.put("children", children);
        StringOutput output = new StringOutput();
        template.render("plants/genera/detail.jte", params, output);

        assertThat(output.toString())
                .contains("/plants/trifolium-incarnatum")
                .contains("/plants/trifolium-repens")
                .doesNotContain("No species catalogued");
    }
}
