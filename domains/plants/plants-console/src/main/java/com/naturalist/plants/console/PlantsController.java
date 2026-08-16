package com.naturalist.plants.console;

import com.naturalist.data.NaturalistDatabase;
import com.naturalist.data.Page;
import com.naturalist.data.PageRequest;
import com.naturalist.fieldnotes.Description;
import com.naturalist.fieldnotes.render.DescriptionRenderer;
import com.naturalist.plants.PlantSpecies;
import com.naturalist.plants.PlantFamily;
import com.naturalist.plants.PlantFamilyName;
import com.naturalist.plants.PlantGenus;
import com.naturalist.plants.PlantGenusName;
import com.naturalist.plants.PlantSpeciesName;
import com.naturalist.plants.PlantQuery;
import com.naturalist.plants.PlantsTestContext;
import com.naturalist.plants.console.render.PlantsParagraphCues;
import com.naturalist.plants.cultivar.Cultivar;
import com.naturalist.plants.cultivar.CultivarName;
import com.naturalist.plants.cultivar.CultivarQuery;
import com.naturalist.plants.heritage.SeedLineage;
import com.naturalist.plants.heritage.SeedLineageName;
import com.naturalist.plants.heritage.SeedLineageQuery;
import com.naturalist.plants.management.PlantProgram;
import com.naturalist.plants.management.PlantProgramName;
import com.naturalist.plants.management.PlantProgramQuery;
import com.naturalist.plants.phytochemistry.PhytochemicalConstituent;
import com.naturalist.plants.phytochemistry.PhytochemicalConstituentName;
import com.naturalist.plants.phytochemistry.PhytochemicalConstituentQuery;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Comparator;

@Controller
@RequestMapping("/plants")
public class PlantsController {

    private final PlantQuery plantQuery;
    private final CultivarQuery cultivarQuery;
    private final SeedLineageQuery seedLineageQuery;
    private final PlantProgramQuery plantProgramQuery;
    private final PhytochemicalConstituentQuery phytochemicalConstituentQuery;
    private final DescriptionRenderer descriptionRenderer;

    PlantsController() {
        //TODO:: This will eventually be a spring managed bean
        PlantsTestContext context = PlantsTestContext.create(NaturalistDatabase.create());
        this.plantQuery = context.plantQuery();
        this.cultivarQuery = context.cultivarQuery();
        this.seedLineageQuery = context.seedLineageQuery();
        this.plantProgramQuery = context.plantProgramQuery();
        this.phytochemicalConstituentQuery = context.phytochemicalConstituentQuery();
        this.descriptionRenderer = new DescriptionRenderer(PlantsParagraphCues.CUES);
    }

    // ── Plant catalog ────────────────────────────────────────────────────

    @GetMapping
    String list(@RequestParam(defaultValue = "0") int page, Model model) {
        Page<PlantSpecies> plantsPage = plantQuery.plants().findPage(PageRequest.console(Math.max(0, page)));
        model.addAttribute("plantsPage", plantsPage);
        return "plants/list";
    }

    @GetMapping("/{name}")
    String detail(@PathVariable String name, Model model) {
        var plantName = PlantSpeciesName.of(name);
        var plant = plantQuery.plants().getByName(plantName);
        if (plant.isEmpty()) {
            return "redirect:/plants";
        }
        var cultivars = cultivarQuery.cultivars().forPlantName(plantName).stream()
                .sorted(Comparator.comparing((Cultivar c) -> c.name().value()))
                .toList();
        var programs = plantProgramQuery.programs().forPlantName(plantName).stream()
                .sorted(Comparator.comparing((PlantProgram p) -> p.name().value()))
                .toList();
        var constituents = phytochemicalConstituentQuery.constituents().forPlantName(plantName).stream()
                .sorted(Comparator.comparing((PhytochemicalConstituent c) -> c.name().value()))
                .toList();
        model.addAttribute("plant", plant.get());
        model.addAttribute("cultivars", cultivars);
        model.addAttribute("programs", programs);
        model.addAttribute("constituents", constituents);
        addDescription(model, plant.get().description());
        return "plants/detail";
    }

    // ── Rank pages ────────────────────────────────────────────────────────

    @GetMapping("/families")
    String familyList(@RequestParam(defaultValue = "0") int page, Model model) {
        Page<PlantFamily> familiesPage =
                plantQuery.families().findPage(PageRequest.console(Math.max(0, page)));
        model.addAttribute("familiesPage", familiesPage);
        return "plants/families/list";
    }

    @GetMapping("/families/{name}")
    String familyDetail(@PathVariable String name, Model model) {
        var familyName = PlantFamilyName.of(name);
        var family = plantQuery.families().getByName(familyName);
        if (family.isEmpty()) {
            return "redirect:/plants";
        }
        var genera = plantQuery.genera().forFamilyName(familyName).stream()
                .sorted(Comparator.comparing((PlantGenus g) -> g.name().value()))
                .toList();
        model.addAttribute("family", family.get());
        model.addAttribute("genera", genera);
        addDescription(model, family.get().description());
        return "plants/families/detail";
    }

    @GetMapping("/genera/{name}")
    String genusDetail(@PathVariable String name, Model model) {
        var genusName = PlantGenusName.of(name);
        var genus = plantQuery.genera().getByName(genusName);
        if (genus.isEmpty()) {
            return "redirect:/plants";
        }
        // The genus → plants rollup needs PlantSpecies.genusName, which does not exist
        // yet (M2b/M2f of the plants consistency plan). Until then the page
        // renders the genus itself and its parent family; the member-plants
        // section appears once the typed FK lands.
        model.addAttribute("genus", genus.get());
        model.addAttribute("family",
                plantQuery.families().getByName(genus.get().familyName()).orElse(null));
        addDescription(model, genus.get().description());
        return "plants/genera/detail";
    }

    // ── Cultivars ─────────────────────────────────────────────────────────

    @GetMapping("/cultivars/{name}")
    String cultivarDetail(@PathVariable String name, Model model) {
        var cultivarName = CultivarName.of(name);
        var cultivar = cultivarQuery.cultivars().getByName(cultivarName);
        if (cultivar.isEmpty()) {
            return "redirect:/plants";
        }
        var lineages = seedLineageQuery.lineages().forCultivarName(cultivarName).stream()
                .sorted(Comparator.comparing((SeedLineage s) -> s.name().value()))
                .toList();
        model.addAttribute("cultivar", cultivar.get());
        model.addAttribute("lineages", lineages);
        return "plants/cultivars/detail";
    }

    // ── Seed lineages ────────────────────────────────────────────────────

    @GetMapping("/lineages/{name}")
    String lineageDetail(@PathVariable String name, Model model) {
        var lineageName = SeedLineageName.of(name);
        var lineage = seedLineageQuery.lineages().getByName(lineageName);
        if (lineage.isEmpty()) {
            return "redirect:/plants";
        }
        model.addAttribute("lineage", lineage.get());
        return "plants/lineages/detail";
    }

    // ── Plant programs ───────────────────────────────────────────────────

    @GetMapping("/programs/{name}")
    String programDetail(@PathVariable String name, Model model) {
        var programName = PlantProgramName.of(name);
        var program = plantProgramQuery.programs().getByName(programName);
        if (program.isEmpty()) {
            return "redirect:/plants";
        }
        model.addAttribute("program", program.get());
        return "plants/programs/detail";
    }

    // ── Phytochemistry ───────────────────────────────────────────────────

    @GetMapping("/phytochemistry")
    String phytochemistryList(@RequestParam(defaultValue = "0") int page, Model model) {
        Page<PhytochemicalConstituent> constituentsPage = phytochemicalConstituentQuery.constituents()
                .findPage(PageRequest.console(Math.max(0, page)));
        model.addAttribute("constituentsPage", constituentsPage);
        return "plants/phytochemistry/list";
    }

    @GetMapping("/phytochemistry/{name}")
    String constituentDetail(@PathVariable String name, Model model) {
        var constituentName = PhytochemicalConstituentName.of(name);
        var constituent = phytochemicalConstituentQuery.constituents().getByName(constituentName);
        if (constituent.isEmpty()) {
            return "redirect:/plants/phytochemistry";
        }
        model.addAttribute("constituent", constituent.get());
        return "plants/phytochemistry/detail";
    }

    // ── Shared ───────────────────────────────────────────────────────────

    /**
     * Renders the four Durrell levels onto the model under the attribute names
     * {@code components/description.jte} expects.
     */
    private void addDescription(Model model, Description description) {
        model.addAttribute("descriptionPreschool", descriptionRenderer.render(description.preschool()));
        model.addAttribute("descriptionElementary", descriptionRenderer.render(description.elementary()));
        model.addAttribute("descriptionSecondary", descriptionRenderer.render(description.secondary()));
        model.addAttribute("descriptionUniversity", descriptionRenderer.render(description.university()));
    }
}
