package com.naturalist.plants;

import com.naturalist.clades.Clade;
import com.naturalist.clades.CladeTraversal;
import com.naturalist.clades.Plantae;
import com.naturalist.data.FileName;
import com.naturalist.ddd.EntityName;
import com.naturalist.observability.Handling;
import com.naturalist.observability.Level;
import com.naturalist.observability.MethodObserver;
import com.naturalist.observability.Observer;
import com.naturalist.data.Page;
import com.naturalist.data.Pages;
import com.naturalist.data.PageRequest;
import com.naturalist.fieldnotes.Description;
import com.naturalist.fieldnotes.render.DescriptionRenderer;
import com.naturalist.library.CladeQuery;
import com.naturalist.library.CladeStep;
import com.naturalist.library.CladeView;
import com.naturalist.observation.ImageStore;
import com.naturalist.observation.ImageVariant;
import com.naturalist.observation.StoredImage;
import com.naturalist.plants.render.PlantsParagraphCues;
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
import com.naturalist.resilience.BulkheadFullException;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.io.IOException;

@Controller
@RequestMapping("/plants")
public class PlantsController {

    private static final Observer observer = Observer.forClass(PlantsController.class);

    private final PlantQuery plantQuery;
    private final CultivarQuery cultivarQuery;
    private final SeedLineageQuery seedLineageQuery;
    private final PlantProgramQuery plantProgramQuery;
    private final PhytochemicalConstituentQuery phytochemicalConstituentQuery;
    private final CladeQuery cladeQuery;
    private final DescriptionRenderer descriptionRenderer;
    private final ImageStore imageStore;

    PlantsController(PlantQuery plantQuery,
                     CultivarQuery cultivarQuery,
                     SeedLineageQuery seedLineageQuery,
                     PlantProgramQuery plantProgramQuery,
                     PhytochemicalConstituentQuery phytochemicalConstituentQuery,
                     CladeQuery cladeQuery,
                     ImageStore imageStore) {
        this.plantQuery = plantQuery;
        this.cultivarQuery = cultivarQuery;
        this.seedLineageQuery = seedLineageQuery;
        this.plantProgramQuery = plantProgramQuery;
        this.phytochemicalConstituentQuery = phytochemicalConstituentQuery;
        this.cladeQuery = cladeQuery;
        this.descriptionRenderer = new DescriptionRenderer(PlantsParagraphCues.CUES);
        this.imageStore = imageStore;
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
    String detail(@PathVariable("name") PlantSpeciesName plantName,
                  HttpServletResponse response, Model model) {
        MethodObserver methodObserver = observer.forMethod("detail");
        if (methodObserver.arguments(c -> c
                .entityName(plantName, "plantName"))
                .observe(Level.INFO)
                .isNotValid()) {
            return notInCatalog(response, model, plantName, "/plants/species");
        }
        Optional<Plant> plant = plantQuery.getByName(plantName);
        return methodObserver.state(c -> c
                .isPresent(plant, "plant"))
                .observe(Level.INFO)
                .whenNotValid(() -> notInCatalog(response, model, plantName, "/plants/species"))
                .otherwise(() -> speciesPage(plantName, plant.get(), model, response));
    }

    private String speciesPage(PlantSpeciesName plantName, Plant plant, Model model, HttpServletResponse response) {
        if (observer.arguments("speciesPage", c -> c
                .readModel(plant, "plant")
                .isTrue(plant, Plant::hasSpecies, "hasSpecies"))
                .observe(Level.WARN)
                .isNotValid()) {
            // the catalog holds it, but the stored record is invalid: counted to fix, answered as not found
            return notInCatalog(response, model, plantName, "/plants/species");
        }
        PlantSpecies species = plant.species().species();
        List<Cultivar> cultivars = plant.cultivars().stream()
                .sorted(Comparator.comparing((Cultivar c) -> c.name().value()))
                .toList();
        List<PlantProgram> programs = plant.programs().stream()
                .sorted(Comparator.comparing((PlantProgram p) -> p.name().value()))
                .toList();
        List<PhytochemicalConstituent> constituents = plant.constituents().stream()
                .sorted(Comparator.comparing((PhytochemicalConstituent c) -> c.name().value()))
                .toList();
        model.addAttribute("plant", species);
        model.addAttribute("ecologicalRole", plant.role());
        model.addAttribute("cultivars", cultivars);
        model.addAttribute("programs", programs);
        model.addAttribute("constituents", constituents);
        model.addAttribute("features", plant.features());
        model.addAttribute("images", plant.images().stream().toList());
        model.addAttribute("breadcrumb", breadcrumbFor(plant));
        model.addAttribute("cladeTrail", cladeTrailFor(plant));
        addDescription(model, species.description());
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
        ArrayList<BreadcrumbSegment> segments = new ArrayList<BreadcrumbSegment>();
        segments.add(BreadcrumbSegment.link("Plantae", CATALOG_ROOT, "Kingdom"));
        return segments;
    }

    /**
     * The clade-row counterpart to {@link #plantaeRoot()} for a kingdom-level catalog
     * landing (all orders, all families, all species). Rather than stopping at the plant
     * kingdom, the Tree of Life row descends to the deepest clade shared by every
     * catalogued order — for a catalogue of flowering plants that is
     * {@code Eukaryota › Plantae › Angiosperms}. It shortens automatically if a lineage
     * outside that clade is ever added (a conifer, say, would pull the shared ancestor
     * back up to Plantae). The row runs to Eukaryota, the shared root, so the naturalist
     * can cross into the animal kingdom there.
     */
    private List<CladeStep> catalogCladeRoot() {
        List<Clade> orderClades = Pages.stream(1000, plantQuery.orders()::findPage)
                .map(PlantOrder::placedIn)
                .filter(clade -> clade != null)
                .toList();
        Clade sharedAncestor = lowestCommonAncestor(orderClades);
        return stepsFor(sharedAncestor == null ? new Plantae() : sharedAncestor);
    }

    /** Root→subject clade steps for a clade, sourced from the shared library read model. */
    private List<CladeStep> stepsFor(Clade clade) {
        CladeView view = cladeQuery.getBySlug(clade.slug()).orElseThrow(
                () -> new IllegalStateException("no clade view for slug " + clade.slug()));
        ArrayList<CladeStep> steps = new ArrayList<CladeStep>(view.ancestry());
        steps.add(view.subject());
        return steps;
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

    /**
     * The taxonomic breadcrumb for a resolved {@link Plant}, read straight off its rank
     * chain — Plantae root, a link per ancestor, the subject rank as the current segment.
     * Replaces the former per-rank re-query helpers now that the read model carries the
     * ancestry. A missing ancestor (null view) shortens the trail rather than failing.
     */
    static List<BreadcrumbSegment> breadcrumbFor(Plant plant) {
        List<BreadcrumbSegment> segments = plantaeRoot();
        boolean speciesSubject = plant.species() != null;
        boolean genusSubject = !speciesSubject && plant.genus() != null;
        boolean familySubject = !speciesSubject && !genusSubject && plant.family() != null;

        boolean orderIsAncestor = plant.family() != null || plant.genus() != null || plant.species() != null;
        if (plant.order() != null && orderIsAncestor) {
            segments.add(orderLink(plant.order().order()));
        }
        if (plant.family() != null && !familySubject) {
            segments.add(familyLink(plant.family().family()));
        }
        if (plant.genus() != null && !genusSubject) {
            segments.add(genusLink(plant.genus().genus()));
        }
        if (speciesSubject) {
            String genusEpithet = plant.genus() != null ? plant.genus().genus().genus().value() + " " : "";
            segments.add(BreadcrumbSegment.current(
                    genusEpithet + plant.species().species().epithet().value(), "Species"));
        } else if (genusSubject) {
            segments.add(BreadcrumbSegment.current(plant.genus().genus().genus().value(), "Genus"));
        } else if (familySubject) {
            segments.add(BreadcrumbSegment.current(plant.family().family().family().value(), "Family"));
        } else if (plant.order() != null) {
            segments.add(BreadcrumbSegment.current(plant.order().order().order().value(), "Order"));
        }
        return segments;
    }

    /**
     * The tree-of-life clade row for a resolved {@link Plant}: the lineage from the plant
     * kingdom down to the order's {@code placedIn} clade. Read off {@code plant.order()}
     * (present for every rank via the resolved ancestry); empty when the order carries no
     * placement. Plant clades are supra-ordinal, so lower ranks resolve through the order.
     */
    private List<CladeStep> cladeTrailFor(Plant plant) {
        if (plant.order() == null) {
            return List.of();
        }
        Clade placedIn = plant.order().order().placedIn();
        if (placedIn == null) {
            return List.of();
        }
        return stepsFor(placedIn);
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
    String orderDetail(@PathVariable("name") PlantOrderName orderName,
                       HttpServletResponse response, Model model) {
        MethodObserver methodObserver = observer.forMethod("orderDetail");
        if (methodObserver.arguments(c -> c
                .entityName(orderName, "orderName"))
                .observe(Level.INFO)
                .isNotValid()) {
            return notInCatalog(response, model, orderName, "/plants/orders");
        }
        Optional<Plant> plant = plantQuery.getByName(orderName);
        return methodObserver.state(c -> c
                .isPresent(plant, "plant"))
                .observe(Level.INFO)
                .whenNotValid(() -> notInCatalog(response, model, orderName, "/plants/orders"))
                .otherwise(() -> orderPage(orderName, plant.get(), model, response));
    }

    private String orderPage(PlantOrderName orderName, Plant plant, Model model, HttpServletResponse response) {
        if (observer.arguments("orderPage", c -> c
                .readModel(plant, "plant")
                .isTrue(plant, Plant::hasOrder, "hasOrder"))
                .observe(Level.WARN)
                .isNotValid()) {
            // the catalog holds it, but the stored record is invalid: counted to fix, answered as not found
            return notInCatalog(response, model, orderName, "/plants/orders");
        }
        PlantOrder order = plant.order().order();
        model.addAttribute("order", order);
        model.addAttribute("children", plant.children());
        model.addAttribute("features", plant.features());
        model.addAttribute("images", plant.images().stream().toList());
        model.addAttribute("breadcrumb", breadcrumbFor(plant));
        model.addAttribute("cladeTrail", cladeTrailFor(plant));
        addDescription(model, order.description());
        return "plants/orders/detail";
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
        Optional<Clade> found = Clade.find(slug);
        if (found.isEmpty()) {
            return "redirect:/plants/orders";
        }
        Clade clade = found.get();
        if (PlantCladeTree.isAnimal(clade)) {
            return "redirect:/insects/clades/" + slug;
        }
        model.addAttribute("clade", clade);
        model.addAttribute("orders", ordersPlacedAt(clade));
        model.addAttribute("childClades", PlantCladeTree.narrower(clade));
        model.addAttribute("breadcrumb", plantaeRoot());
        model.addAttribute("cladeTrail", stepsFor(clade));
        addDescription(model, clade.description());
        return "plants/clades/detail";
    }

    /**
     * The catalogued orders placed directly at {@code clade} (its exact placement).
     * Internal clades hold none — their orders live in the narrower clades below,
     * reached through the breadcrumb dropdowns.
     */
    private List<PlantOrder> ordersPlacedAt(Clade clade) {
        return Pages.stream(1000, plantQuery.orders()::findPage)
                .filter(order -> clade.equals(order.placedIn()))
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
    String familyDetail(@PathVariable("name") PlantFamilyName familyName,
                        HttpServletResponse response, Model model) {
        MethodObserver methodObserver = observer.forMethod("familyDetail");
        if (methodObserver.arguments(c -> c
                .entityName(familyName, "familyName"))
                .observe(Level.INFO)
                .isNotValid()) {
            return notInCatalog(response, model, familyName, "/plants/families");
        }
        Optional<Plant> plant = plantQuery.getByName(familyName);
        return methodObserver.state(c -> c
                .isPresent(plant, "plant"))
                .observe(Level.INFO)
                .whenNotValid(() -> notInCatalog(response, model, familyName, "/plants/families"))
                .otherwise(() -> familyPage(familyName, plant.get(), model, response));
    }

    private String familyPage(PlantFamilyName familyName, Plant plant, Model model, HttpServletResponse response) {
        if (observer.arguments("familyPage", c -> c
                .readModel(plant, "plant")
                .isTrue(plant, Plant::hasFamily, "hasFamily"))
                .observe(Level.WARN)
                .isNotValid()) {
            // the catalog holds it, but the stored record is invalid: counted to fix, answered as not found
            return notInCatalog(response, model, familyName, "/plants/families");
        }
        PlantFamily family = plant.family().family();
        model.addAttribute("family", family);
        model.addAttribute("children", plant.children());
        model.addAttribute("features", plant.features());
        model.addAttribute("images", plant.images().stream().toList());
        model.addAttribute("breadcrumb", breadcrumbFor(plant));
        model.addAttribute("cladeTrail", cladeTrailFor(plant));
        addDescription(model, family.description());
        return "plants/families/detail";
    }

    @GetMapping("/genera/{name}")
    String genusDetail(@PathVariable("name") PlantGenusName genusName,
                       HttpServletResponse response, Model model) {
        MethodObserver methodObserver = observer.forMethod("genusDetail");
        if (methodObserver.arguments(c -> c
                .entityName(genusName, "genusName"))
                .observe(Level.INFO)
                .isNotValid()) {
            return notInCatalog(response, model, genusName, "/plants/orders");
        }
        Optional<Plant> plant = plantQuery.getByName(genusName);
        return methodObserver.state(c -> c
                .isPresent(plant, "plant"))
                .observe(Level.INFO)
                .whenNotValid(() -> notInCatalog(response, model, genusName, "/plants/orders"))
                .otherwise(() -> genusPage(genusName, plant.get(), model, response));
    }

    private String genusPage(PlantGenusName genusName, Plant plant, Model model, HttpServletResponse response) {
        if (observer.arguments("genusPage", c -> c
                .readModel(plant, "plant")
                .isTrue(plant, Plant::hasGenus, "hasGenus"))
                .observe(Level.WARN)
                .isNotValid()) {
            // the catalog holds it, but the stored record is invalid: counted to fix, answered as not found
            return notInCatalog(response, model, genusName, "/plants/orders");
        }
        PlantGenus genus = plant.genus().genus();
        model.addAttribute("genus", genus);
        model.addAttribute("family",
                plant.family() == null ? null : plant.family().family());
        model.addAttribute("children", plant.children());
        model.addAttribute("features", plant.features());
        model.addAttribute("images", plant.images().stream().toList());
        model.addAttribute("breadcrumb", breadcrumbFor(plant));
        model.addAttribute("cladeTrail", cladeTrailFor(plant));
        addDescription(model, genus.description());
        return "plants/genera/detail";
    }

    // ── Cultivars ─────────────────────────────────────────────────────────

    @GetMapping("/cultivars/{name}")
    String cultivarDetail(@PathVariable("name") CultivarName cultivarName,
                          HttpServletResponse response, Model model) {
        MethodObserver methodObserver = observer.forMethod("cultivarDetail");
        if (methodObserver.arguments(c -> c
                .entityName(cultivarName, "cultivarName"))
                .observe(Level.INFO)
                .isNotValid()) {
            return notInCatalog(response, model, cultivarName, "/plants/species");
        }
        Optional<Cultivar> cultivar = cultivarQuery.getByName(cultivarName);
        return methodObserver.state(c -> c
                .isPresent(cultivar, "cultivar"))
                .observe(Level.INFO)
                .whenNotValid(() -> notInCatalog(response, model, cultivarName, "/plants/species"))
                .otherwise(() -> cultivarPage(cultivarName, cultivar.get(), model, response));
    }

    private String cultivarPage(CultivarName cultivarName, Cultivar cultivar,
                                Model model, HttpServletResponse response) {
        if (observer.arguments("cultivarPage", c -> c
                .namedEntity(cultivar, "cultivar"))
                .observe(Level.WARN)
                .isNotValid()) {
            // the catalog holds it, but the stored record is invalid: counted to fix, answered as not found
            return notInCatalog(response, model, cultivarName, "/plants/species");
        }
        List<SeedLineage> lineages = seedLineageQuery.forCultivarName(cultivar.name()).stream()
                .sorted(Comparator.comparing((SeedLineage s) -> s.name().value()))
                .toList();
        model.addAttribute("cultivar", cultivar);
        model.addAttribute("lineages", lineages);
        return "plants/cultivars/detail";
    }

    // ── Seed lineages ────────────────────────────────────────────────────

    @GetMapping("/lineages/{name}")
    String lineageDetail(@PathVariable("name") SeedLineageName lineageName,
                         HttpServletResponse response, Model model) {
        MethodObserver methodObserver = observer.forMethod("lineageDetail");
        if (methodObserver.arguments(c -> c
                .entityName(lineageName, "lineageName"))
                .observe(Level.INFO)
                .isNotValid()) {
            return notInCatalog(response, model, lineageName, "/plants/species");
        }
        Optional<SeedLineage> lineage = seedLineageQuery.getByName(lineageName);
        return methodObserver.state(c -> c
                .isPresent(lineage, "lineage"))
                .observe(Level.INFO)
                .whenNotValid(() -> notInCatalog(response, model, lineageName, "/plants/species"))
                .otherwise(() -> lineagePage(lineageName, lineage.get(), model, response));
    }

    private String lineagePage(SeedLineageName lineageName, SeedLineage lineage,
                               Model model, HttpServletResponse response) {
        if (observer.arguments("lineagePage", c -> c
                .namedEntity(lineage, "lineage"))
                .observe(Level.WARN)
                .isNotValid()) {
            // the catalog holds it, but the stored record is invalid: counted to fix, answered as not found
            return notInCatalog(response, model, lineageName, "/plants/species");
        }
        model.addAttribute("lineage", lineage);
        return "plants/lineages/detail";
    }

    // ── Plant programs ───────────────────────────────────────────────────

    @GetMapping("/programs/{name}")
    String programDetail(@PathVariable("name") PlantProgramName programName,
                         HttpServletResponse response, Model model) {
        MethodObserver methodObserver = observer.forMethod("programDetail");
        if (methodObserver.arguments(c -> c
                .entityName(programName, "programName"))
                .observe(Level.INFO)
                .isNotValid()) {
            return notInCatalog(response, model, programName, "/plants/species");
        }
        Optional<PlantProgram> program = plantProgramQuery.getByName(programName);
        return methodObserver.state(c -> c
                .isPresent(program, "program"))
                .observe(Level.INFO)
                .whenNotValid(() -> notInCatalog(response, model, programName, "/plants/species"))
                .otherwise(() -> programPage(programName, program.get(), model, response));
    }

    private String programPage(PlantProgramName programName, PlantProgram program,
                               Model model, HttpServletResponse response) {
        if (observer.arguments("programPage", c -> c
                .namedEntity(program, "program"))
                .observe(Level.WARN)
                .isNotValid()) {
            // the catalog holds it, but the stored record is invalid: counted to fix, answered as not found
            return notInCatalog(response, model, programName, "/plants/species");
        }
        model.addAttribute("program", program);
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
    String constituentDetail(@PathVariable("name") PhytochemicalConstituentName constituentName,
                             HttpServletResponse response, Model model) {
        MethodObserver methodObserver = observer.forMethod("constituentDetail");
        if (methodObserver.arguments(c -> c
                .entityName(constituentName, "constituentName"))
                .observe(Level.INFO)
                .isNotValid()) {
            return notInCatalog(response, model, constituentName, "/plants/phytochemistry");
        }
        Optional<PhytochemicalConstituent> constituent = phytochemicalConstituentQuery.getByName(constituentName);
        return methodObserver.state(c -> c
                .isPresent(constituent, "constituent"))
                .observe(Level.INFO)
                .whenNotValid(() -> notInCatalog(response, model, constituentName, "/plants/phytochemistry"))
                .otherwise(() -> constituentPage(constituentName, constituent.get(), model, response));
    }

    private String constituentPage(PhytochemicalConstituentName constituentName, PhytochemicalConstituent constituent,
                                   Model model, HttpServletResponse response) {
        if (observer.arguments("constituentPage", c -> c
                .namedEntity(constituent, "constituent"))
                .observe(Level.WARN)
                .isNotValid()) {
            // the catalog holds it, but the stored record is invalid: counted to fix, answered as not found
            return notInCatalog(response, model, constituentName, "/plants/phytochemistry");
        }
        model.addAttribute("constituent", constituent);
        return "plants/phytochemistry/detail";
    }

    // ── Images ───────────────────────────────────────────────────────────

    /**
     * Streams a stored photo from the store to the client, never buffering it whole (PL-16), under
     * the media type the store sniffs. Names are unique, so the response is cacheable forever. When the store's read cap is full
     * ({@link BulkheadFullException}) the answer is an uncached 503 with {@code Retry-After},
     * so the browser retries the one photo rather than the page failing (PL-16).
     */
    @GetMapping("/images/{filename:.+}")
    ResponseEntity<Resource> image(@PathVariable FileName filename) {
        return serve("plants", filename, ImageVariant.PHOTO);
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

    /**
     * Streams one variant of a stored photo. A missing variant is an uncached 404: a tile never
     * falls back to the full photo, and the 404 must not be remembered once the tile is written.
     */
    private ResponseEntity<Resource> serve(String space, FileName filename, ImageVariant variant) {
        MethodObserver methodObserver = observer.forMethod("serve");
        // A name with a directory part names no stored photo: answered here, before the store refuses it.
        if (methodObserver.arguments(i -> i
                .namedValue(filename, "filename"))
                .observe(Level.WARN)
                .isNotValid()) {
            return notFound();
        }
        Optional<StoredImage> found;
        try {
            found = imageStore.read(space, filename, variant);
        } catch (BulkheadFullException busy) {
            // the store's read cap is full: the browser retries this one photo
            methodObserver.noticeError("read", busy, Handling.SWALLOWED);
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .header(HttpHeaders.RETRY_AFTER, "1")
                    .cacheControl(CacheControl.noStore())
                    .build();
        }
        // a broken link of ours, or someone probing names: the caller is told, and the miss is counted
        if (!methodObserver.state(i -> i
                .isTrue(found.isPresent(), "found"))
                .observe(Level.WARN)
                .violations().isEmpty()) {
            return notFound();
        }
        StoredImage stored = found.orElseThrow();
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(stored.mediaType()))
                .contentLength(stored.size())
                .cacheControl(CacheControl.maxAge(365, TimeUnit.DAYS).cachePublic().immutable())
                .body(new InputStreamResource(() -> {
                    try {
                        return stored.content().open();
                    } catch (IOException opened) {
                        // the photo went between the lookup and the stream: counted before Spring answers 500
                        methodObserver.noticeError("opened", opened, Handling.RETHROWN);
                        throw opened;
                    }
                }));
    }

    /**
     * Answers a request for a name the catalog does not hold with the not-found page and a 404, linking back to
     * the list {@code back}. The caller has already observed why (invalid slug or unknown name).
     */
    private static String notInCatalog(HttpServletResponse response, Model model, EntityName name, String back) {
        response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        model.addAttribute("name", name.value());
        model.addAttribute("back", back);
        return "plants/not-found";
    }

    /** An uncached 404: the photo may be written later, so the miss must not be remembered. */
    private static ResponseEntity<Resource> notFound() {
        return ResponseEntity.notFound()
                .cacheControl(CacheControl.noStore())
                .build();
    }

    /**
     * Streams the stored tile of a photo, the scaled-down copy the galleries show. A photo stored
     * before tiles existed has none until it is conformed, and that is a 404, never the full photo.
     */
    @GetMapping("/images/tile/{filename:.+}")
    ResponseEntity<Resource> tile(@PathVariable FileName filename) {
        return serve("plants", filename, ImageVariant.TILE);
    }
}
