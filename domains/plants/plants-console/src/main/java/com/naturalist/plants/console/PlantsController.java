package com.naturalist.plants.console;

import com.naturalist.clades.Clade;
import com.naturalist.clades.CladeCatalog;
import com.naturalist.clades.CladeTraversal;
import com.naturalist.clades.Eukaryota;
import com.naturalist.clades.Plantae;
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
        Page<PlantSpecies> plantsPage = plantQuery.species().findPage(PageRequest.console(Math.max(0, page)));
        model.addAttribute("plantsPage", plantsPage);
        // Roles are a separate cross-rank record, so the badges need a lookup rather than
        // an accessor. Keyed by slug because the template holds a PlantSpecies, not a rank name.
        model.addAttribute("ecologicalRoles", Pages.stream(1000, plantQuery.ecologicalRoles()::findPage)
                .collect(Collectors.toMap(r -> r.plantName().value(), r -> r, (a, b) -> a)));
        model.addAttribute("breadcrumb", plantaeRoot());
        model.addAttribute("cladeTrail", catalogCladeRoot());
        return "plants/list";
    }

    @GetMapping("/{name}")
    String detail(@PathVariable String name, Model model) {
        var plantName = PlantSpeciesName.of(name);
        var plant = plantQuery.species().getByName(plantName);
        if (plant.isEmpty()) {
            return "redirect:/plants";
        }
        var cultivars = cultivarQuery.forPlantName(plantName).stream()
                .sorted(Comparator.comparing((Cultivar c) -> c.name().value()))
                .toList();
        var programs = plantProgramQuery.forPlantName(plantName).stream()
                .sorted(Comparator.comparing((PlantProgram p) -> p.name().value()))
                .toList();
        var constituents = phytochemicalConstituentQuery.forPlantName(plantName).stream()
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

    /**
     * The clade-row counterpart to {@link #plantaeRoot()} for a kingdom-level catalog
     * landing (all orders, all families, all species). Rather than stopping at the plant
     * kingdom, the Tree of Life row descends to the deepest clade shared by every
     * catalogued order — for a catalogue of flowering plants that is
     * {@code Plantae › Angiosperms}. It shortens automatically if a lineage outside that
     * clade is ever added (a conifer, say, would pull the shared ancestor back up to
     * Plantae). Eukaryota, the shared root above Plantae, is dropped to keep the row in
     * the plant world.
     */
    private List<Clade> catalogCladeRoot() {
        List<Clade> orderClades = Pages.stream(1000, plantQuery.orders()::findPage)
                .map(PlantOrder::placedIn)
                .filter(clade -> clade != null)
                .toList();
        Clade sharedAncestor = lowestCommonAncestor(orderClades);
        if (sharedAncestor == null) {
            return List.of(new Plantae());
        }
        return CladeTraversal.ancestry(sharedAncestor).reversed().stream()
                .filter(clade -> !(clade instanceof Eukaryota))
                .toList();
    }

    /**
     * The deepest clade shared by every clade in {@code clades} — their lowest common
     * ancestor — or {@code null} when the set is empty. Walks the first clade's ancestry
     * from the node upward and returns the first ancestor present in every clade's
     * lineage. Package-private for test.
     */
    static Clade lowestCommonAncestor(List<Clade> clades) {
        if (clades.isEmpty()) {
            return null;
        }
        for (Clade candidate : CladeTraversal.ancestry(clades.getFirst())) {
            boolean sharedByAll = clades.stream()
                    .allMatch(clade -> CladeTraversal.ancestry(clade).contains(candidate));
            if (sharedByAll) {
                return candidate;
            }
        }
        return null;
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
        model.addAttribute("cladeTrail", catalogCladeRoot());
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
        model.addAttribute("cladeTrail", cladeTrailFor(order.get()));
        addDescription(model, order.get().description());
        return "plants/orders/detail";
    }

    /**
     * The phylogenetic tree-of-life row for an order: the clade lineage from the
     * plant kingdom (Plantae) down to the order's {@code placedIn} clade. Empty when
     * the order carries no placement — plant clades are supra-ordinal, so lower ranks
     * would resolve this by walking up to their order (deferred).
     * <p>
     * The row opens at Plantae to match the taxonomic breadcrumb above it, which also
     * roots at Plantae. Eukaryota — the shared root where the plant and animal lineages
     * meet — sits one click up from the Plantae node and in the full tree at {@code /clades},
     * so it is omitted here to keep the row within the plant world.
     */
    private List<Clade> cladeTrailFor(PlantOrder order) {
        Clade placedIn = order.placedIn();
        if (placedIn == null) {
            return List.of();
        }
        return CladeTraversal.ancestry(placedIn).reversed().stream()
                .filter(clade -> !(clade instanceof Eukaryota))
                .toList();
    }

    // ── Clade pages (the tree-of-life driving plant queries) ─────────────

    /**
     * A clade's page in the plant catalog: its four-level description plus the plant
     * orders placed in it, and its narrower child clades to drill further in. This is
     * the plants-domain counterpart to the shared {@code /clades/{slug}} tree-of-life
     * page — where that one lists child clades, this one lists plant orders — so the
     * Tree of Life breadcrumb navigates plant records and never leaves the console.
     * Non-plant clades (and unknown slugs) redirect back to the order catalog.
     */
    @GetMapping("/clades/{slug}")
    String cladeDetail(@PathVariable String slug, Model model) {
        Clade clade;
        try {
            clade = Clade.of(slug);
        } catch (IllegalArgumentException notAClade) {
            return "redirect:/plants/orders";
        }
        if (!isPlantClade(clade)) {
            return "redirect:/plants/orders";
        }
        List<Clade> childClades = CladeCatalog.childrenOf(clade).stream()
                .filter(child -> !ordersInClade(child).isEmpty())
                .toList();
        model.addAttribute("clade", clade);
        model.addAttribute("orders", ordersInClade(clade));
        model.addAttribute("childClades", childClades);
        model.addAttribute("breadcrumb", plantaeRoot());
        model.addAttribute("cladeTrail", CladeTraversal.ancestry(clade).reversed().stream()
                .filter(node -> !(node instanceof Eukaryota))
                .toList());
        addDescription(model, clade.description());
        return "plants/clades/detail";
    }

    /** A clade belongs to the plant catalog iff its lineage passes through Plantae. */
    private static boolean isPlantClade(Clade clade) {
        return CladeTraversal.ancestry(clade).stream().anyMatch(node -> node instanceof Plantae);
    }

    /** Every catalogued order whose clade placement lies within {@code clade}. */
    private List<PlantOrder> ordersInClade(Clade clade) {
        return Pages.stream(1000, plantQuery.orders()::findPage)
                .filter(order -> order.placedIn() != null
                        && CladeTraversal.ancestry(order.placedIn()).contains(clade))
                .sorted(Comparator.comparing((PlantOrder order) -> order.name().value()))
                .toList();
    }

    @GetMapping("/families")
    String familyList(@RequestParam(defaultValue = "0") int page, Model model) {
        Page<PlantFamily> familiesPage =
                plantQuery.families().findPage(PageRequest.console(Math.max(0, page)));
        model.addAttribute("familiesPage", familiesPage);
        model.addAttribute("breadcrumb", plantaeRoot());
        model.addAttribute("cladeTrail", catalogCladeRoot());
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
        var species = plantQuery.species().forGenusName(genusName).stream()
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
        var cultivar = cultivarQuery.getByName(cultivarName);
        if (cultivar.isEmpty()) {
            return "redirect:/plants";
        }
        var lineages = seedLineageQuery.forCultivarName(cultivarName).stream()
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
        var lineage = seedLineageQuery.getByName(lineageName);
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
        var program = plantProgramQuery.getByName(programName);
        if (program.isEmpty()) {
            return "redirect:/plants";
        }
        model.addAttribute("program", program.get());
        return "plants/programs/detail";
    }

    // ── Phytochemistry ───────────────────────────────────────────────────

    @GetMapping("/phytochemistry")
    String phytochemistryList(@RequestParam(defaultValue = "0") int page, Model model) {
        Page<PhytochemicalConstituent> constituentsPage = phytochemicalConstituentQuery
                .findPage(PageRequest.console(Math.max(0, page)));
        model.addAttribute("constituentsPage", constituentsPage);
        return "plants/phytochemistry/list";
    }

    @GetMapping("/phytochemistry/{name}")
    String constituentDetail(@PathVariable String name, Model model) {
        var constituentName = PhytochemicalConstituentName.of(name);
        var constituent = phytochemicalConstituentQuery.getByName(constituentName);
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
