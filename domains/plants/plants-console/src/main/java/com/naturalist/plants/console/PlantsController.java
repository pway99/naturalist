package com.naturalist.plants.console;

import com.naturalist.data.NaturalistDatabase;
import com.naturalist.plants.Plant;
import com.naturalist.plants.PlantName;
import com.naturalist.plants.PlantQuery;
import com.naturalist.plants.PlantsTestContext;
import com.naturalist.plants.cultivar.Cultivar;
import com.naturalist.plants.cultivar.CultivarName;
import com.naturalist.plants.cultivar.CultivarQuery;
import com.naturalist.plants.heritage.SeedLineage;
import com.naturalist.plants.heritage.SeedLineageName;
import com.naturalist.plants.heritage.SeedLineageQuery;
import com.naturalist.plants.management.PlantProgram;
import com.naturalist.plants.management.PlantProgramName;
import com.naturalist.plants.management.PlantProgramQuery;
import com.naturalist.plants.console.render.DescriptionRenderer;
import com.naturalist.plants.phytochemistry.PhytochemicalConstituent;
import com.naturalist.plants.phytochemistry.PhytochemicalConstituentName;
import com.naturalist.plants.phytochemistry.PhytochemicalConstituentQuery;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.Comparator;
import java.util.stream.Collectors;

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
        this.descriptionRenderer = new DescriptionRenderer();
    }

    // ── Plant catalog ────────────────────────────────────────────────────

    @GetMapping
    String list(Model model) {
        var nameSet = plantQuery.plants().allPlantNames().stream().collect(Collectors.toSet());
        var plants = plantQuery.plants().findByNameSet(nameSet).stream()
                .sorted(Comparator.comparing((Plant p) -> p.name().value()))
                .toList();
        model.addAttribute("plants", plants);
        return "plants/list";
    }

    @GetMapping("/{name}")
    String detail(@PathVariable String name, Model model) {
        var plantName = PlantName.of(name);
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
        var description = plant.get().description();
        model.addAttribute("plant", plant.get());
        model.addAttribute("cultivars", cultivars);
        model.addAttribute("programs", programs);
        model.addAttribute("constituents", constituents);
        model.addAttribute("descriptionPreschool", descriptionRenderer.render(description.preschool()));
        model.addAttribute("descriptionElementary", descriptionRenderer.render(description.elementary()));
        model.addAttribute("descriptionSecondary", descriptionRenderer.render(description.secondary()));
        model.addAttribute("descriptionUniversity", descriptionRenderer.render(description.university()));
        return "plants/detail";
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
    String phytochemistryList(Model model) {
        var nameSet = phytochemicalConstituentQuery.constituents()
                .allPhytochemicalConstituentNames().stream().collect(Collectors.toSet());
        var constituents = phytochemicalConstituentQuery.constituents().findByNameSet(nameSet).stream()
                .sorted(Comparator.comparing((PhytochemicalConstituent c) -> c.name().value()))
                .toList();
        model.addAttribute("constituents", constituents);
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
}
