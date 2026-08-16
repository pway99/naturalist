package com.naturalist.plants.console;

import com.naturalist.data.NaturalistDatabase;
import com.naturalist.data.Page;
import com.naturalist.data.Pages;
import com.naturalist.data.PageRequest;
import com.naturalist.fieldnotes.Description;
import com.naturalist.fieldnotes.render.DescriptionRenderer;
import com.naturalist.plants.PlantEcologicalRole;
import com.naturalist.plants.PlantSpecies;
import com.naturalist.plants.PlantFamily;
import com.naturalist.plants.PlantFamilyName;
import com.naturalist.plants.PlantGenus;
import com.naturalist.plants.PlantGenusName;
import com.naturalist.plants.PlantOrder;
import com.naturalist.plants.PlantOrderName;
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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
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
        this.descriptionRenderer = new DescriptionRenderer(PlantsParagraphCues.CUES);
    }

    // ── Plant catalog ────────────────────────────────────────────────────

    // The catalog opens at the top of the rank chain, matching insects
    // (GET /insects -> /insects/orders). Most identifications land above species,
    // so a rank browse is the honest front door; the flat species list moves to
    // /plants/species.
    @GetMapping
    String index() {
        return "redirect:/plants/orders";
    }

    @GetMapping("/species")
    String species(@RequestParam(defaultValue = "0") int page, Model model) {
        Page<PlantSpecies> plantsPage = plantQuery.plants().findPage(PageRequest.console(Math.max(0, page)));
        model.addAttribute("plantsPage", plantsPage);
        // Roles are a separate cross-rank record, so the badges need a lookup rather than
        // an accessor. Keyed by slug because the template holds a PlantSpecies, not a rank name.
        model.addAttribute("ecologicalRoles", Pages.stream(1000, plantQuery.ecologicalRoles()::findPage)
                .collect(Collectors.toMap(r -> r.plantName().value(), r -> r, (a, b) -> a)));
        model.addAttribute("breadcrumb", plantaeRoot());
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
        model.addAttribute("ecologicalRole",
                plantQuery.ecologicalRoles().forPlantName(plantName).orElse(null));
        model.addAttribute("cultivars", cultivars);
        model.addAttribute("programs", programs);
        model.addAttribute("constituents", constituents);
        model.addAttribute("breadcrumb", breadcrumbToSpecies(plant.get()));
        addDescription(model, plant.get().description());
        return "plants/detail";
    }

    // ── Taxonomic breadcrumb ───────────────────────────────────────────────
    // Mirrors the insects console's rank-ladder header. Plants carry no clade in
    // the kernel yet, so the ladder is anchored at the kingdom by hand — Plantae
    // is the plant analogue of the insects breadcrumb's Insecta segment, and its
    // link back to /plants/orders is the click-to-catalog affordance. Ancestry
    // resolution tolerates a missing parent: a gap shortens the trail rather than
    // failing the page, though the fixture FK constraints make gaps unlikely.

    private static final String CATALOG_ROOT = "/plants/orders";

    private static List<BreadcrumbSegment> plantaeRoot() {
        var segments = new ArrayList<BreadcrumbSegment>();
        segments.add(BreadcrumbSegment.link("Plantae", CATALOG_ROOT, "Kingdom"));
        return segments;
    }

    private static BreadcrumbSegment orderLink(PlantOrder order) {
        return BreadcrumbSegment.link(order.order().value(),
                "/plants/orders/" + order.name().value(), "Order");
    }

    private static BreadcrumbSegment familyLink(PlantFamily family) {
        return BreadcrumbSegment.link(family.family().value(),
                "/plants/families/" + family.name().value(), "Family");
    }

    private static BreadcrumbSegment genusLink(PlantGenus genus) {
        return BreadcrumbSegment.link(genus.genus().value(),
                "/plants/genera/" + genus.name().value(), "Genus");
    }

    private List<BreadcrumbSegment> breadcrumbToOrder(PlantOrder order) {
        var segments = plantaeRoot();
        segments.add(BreadcrumbSegment.current(order.order().value(), "Order"));
        return segments;
    }

    private List<BreadcrumbSegment> breadcrumbToFamily(PlantFamily family) {
        var segments = plantaeRoot();
        orderOf(family).ifPresent(o -> segments.add(orderLink(o)));
        segments.add(BreadcrumbSegment.current(family.family().value(), "Family"));
        return segments;
    }

    private List<BreadcrumbSegment> breadcrumbToGenus(PlantGenus genus) {
        var segments = plantaeRoot();
        var family = familyOf(genus);
        family.flatMap(this::orderOf).ifPresent(o -> segments.add(orderLink(o)));
        family.ifPresent(f -> segments.add(familyLink(f)));
        segments.add(BreadcrumbSegment.current(genus.genus().value(), "Genus"));
        return segments;
    }

    private List<BreadcrumbSegment> breadcrumbToSpecies(PlantSpecies species) {
        var segments = plantaeRoot();
        var genus = genusOf(species);
        var family = genus.flatMap(this::familyOf);
        family.flatMap(this::orderOf).ifPresent(o -> segments.add(orderLink(o)));
        family.ifPresent(f -> segments.add(familyLink(f)));
        genus.ifPresent(g -> segments.add(genusLink(g)));
        String label = genus.map(g -> g.genus().value() + " ").orElse("") + species.epithet().value();
        segments.add(BreadcrumbSegment.current(label, "Species"));
        return segments;
    }

    private Optional<PlantOrder> orderOf(PlantFamily family) {
        return plantQuery.orders().getByName(family.orderName());
    }

    private Optional<PlantFamily> familyOf(PlantGenus genus) {
        return plantQuery.families().getByName(genus.familyName());
    }

    private Optional<PlantGenus> genusOf(PlantSpecies species) {
        return plantQuery.genera().getByName(species.genusName());
    }

    // ── Rank pages ────────────────────────────────────────────────────────

    @GetMapping("/orders")
    String orderList(@RequestParam(defaultValue = "0") int page, Model model) {
        Page<PlantOrder> ordersPage =
                plantQuery.orders().findPage(PageRequest.console(Math.max(0, page)));
        model.addAttribute("ordersPage", ordersPage);
        model.addAttribute("breadcrumb", plantaeRoot());
        return "plants/orders/list";
    }

    @GetMapping("/orders/{name}")
    String orderDetail(@PathVariable String name, Model model) {
        var orderName = PlantOrderName.of(name);
        var order = plantQuery.orders().getByName(orderName);
        if (order.isEmpty()) {
            return "redirect:/plants";
        }
        var families = plantQuery.families().forOrderName(orderName).stream()
                .sorted(Comparator.comparing((PlantFamily f) -> f.name().value()))
                .toList();
        model.addAttribute("order", order.get());
        model.addAttribute("families", families);
        model.addAttribute("breadcrumb", breadcrumbToOrder(order.get()));
        addDescription(model, order.get().description());
        return "plants/orders/detail";
    }

    @GetMapping("/families")
    String familyList(@RequestParam(defaultValue = "0") int page, Model model) {
        Page<PlantFamily> familiesPage =
                plantQuery.families().findPage(PageRequest.console(Math.max(0, page)));
        model.addAttribute("familiesPage", familiesPage);
        model.addAttribute("breadcrumb", plantaeRoot());
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
        model.addAttribute("breadcrumb", breadcrumbToFamily(family.get()));
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
        var species = plantQuery.plants().forGenusName(genusName).stream()
                .sorted(Comparator.comparing((PlantSpecies s) -> s.name().value()))
                .toList();
        model.addAttribute("genus", genus.get());
        model.addAttribute("family",
                plantQuery.families().getByName(genus.get().familyName()).orElse(null));
        model.addAttribute("species", species);
        model.addAttribute("breadcrumb", breadcrumbToGenus(genus.get()));
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
