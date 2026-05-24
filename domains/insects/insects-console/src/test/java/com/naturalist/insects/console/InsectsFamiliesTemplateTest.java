package com.naturalist.insects.console;

import com.naturalist.data.NaturalistDatabase;
import com.naturalist.data.Page;
import com.naturalist.data.PageRequest;
import com.naturalist.insects.InsectFamily;
import com.naturalist.insects.InsectFamilyTestEntitySource;
import com.naturalist.insects.InsectSpecies;
import com.naturalist.insects.InsectSpeciesTestEntitySource;
import com.naturalist.insects.TestInsectsIdentifiers;
import gg.jte.output.StringOutput;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class InsectsFamiliesTemplateTest {

    @Test
    void families_rendersWithoutError() {
        NaturalistDatabase database = NaturalistDatabase.create();
        Page<InsectFamily> familyPage = new InsectFamilyTestEntitySource(database)
                .pageOf(PageRequest.console(0));
        StringOutput output = new StringOutput();

        TestTemplateEngine.create().render(
                "insects/families.jte",
                Map.of("familyPage", familyPage),
                output);

        assertThat(output.toString()).isNotBlank();
    }

    @Test
    void family_withUnderIdentifiedSpecies_rendersFamilyRankSection() {
        NaturalistDatabase database = NaturalistDatabase.create();
        InsectFamily tachinidae = new InsectFamilyTestEntitySource(database).entityStream()
                .filter(f -> f.name().equals(TestInsectsIdentifiers.InsectFamily.Tachinidae.name))
                .findFirst().orElseThrow();
        InsectSpecies tachinidFly = new InsectSpeciesTestEntitySource(database).entityStream()
                .filter(s -> s.name().equals(TestInsectsIdentifiers.InsectSpecies.TachinidFly.name))
                .findFirst().orElseThrow();
        StringOutput output = new StringOutput();

        TestTemplateEngine.create().render(
                "insects/family.jte",
                Map.of(
                        "family", tachinidae,
                        "genera", List.of(),
                        "species", List.of(tachinidFly),
                        "descriptionPreschool", "p",
                        "descriptionElementary", "e",
                        "descriptionSecondary", "s",
                        "descriptionUniversity", "u"),
                output);

        String rendered = output.toString();
        assertThat(rendered).contains("Species attached at family rank");
        assertThat(rendered).contains("/insects/" + tachinidFly.name().value());
    }

    @Test
    void family_rendersWithoutError() {
        NaturalistDatabase database = NaturalistDatabase.create();
        InsectFamily anyFamily = new InsectFamilyTestEntitySource(database).entityStream()
                .findFirst().orElseThrow();
        StringOutput output = new StringOutput();

        TestTemplateEngine.create().render(
                "insects/family.jte",
                Map.of(
                        "family", anyFamily,
                        "genera", List.of(),
                        "species", List.of(),
                        "descriptionPreschool", "p",
                        "descriptionElementary", "e",
                        "descriptionSecondary", "s",
                        "descriptionUniversity", "u"),
                output);

        assertThat(output.toString()).isNotBlank();
    }
}
