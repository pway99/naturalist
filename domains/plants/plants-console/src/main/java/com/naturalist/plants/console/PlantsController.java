package com.naturalist.plants.console;

import com.naturalist.clades.Clade;
import com.naturalist.clades.CladeTraversal;
import com.naturalist.clades.Plantae;
import com.naturalist.data.NaturalistDatabase;
import com.naturalist.data.Page;
import com.naturalist.data.Pages;
import com.naturalist.data.PageRequest;
import com.naturalist.fieldnotes.Description;
import com.naturalist.fieldnotes.render.DescriptionRenderer;
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
import com.naturalist.resilience.Resilience;
import com.naturalist.resilience.Resilient;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/plants")
public class PlantsController {

    private static final String IMAGE_CONVERSION = "image.conversion";

    private final PlantQuery plantQuery;
    private final CultivarQuery cultivarQuery;
    private final SeedLineageQuery seedLineageQuery;
    private final PlantProgramQuery plantProgramQuery;
    private final PhytochemicalConstituentQuery phytochemicalConstituentQuery;
    private final DescriptionRenderer descriptionRenderer;
    private final Resilience resilience;
    private final PlantImageStorageService imageStorageService =
            new PlantImageStorageService(Path.of("data/images/plants"));
    private final Map<String, byte[]> jpegCache = new ConcurrentHashMap<>();

    PlantsController(Resilience resilience) {
        //TODO:: This will eventually be a spring managed bean
        PlantsTestContext context = PlantsTestContext.create(NaturalistDatabase.create());
        this.plantQuery = context.plantQuery();
        this.cultivarQuery = context.cultivarQuery();
        this.seedLineageQuery = context.seedLineageQuery();
        this.plantProgramQuery = context.plantProgramQuery();
        this.phytochemicalConstituentQuery = context.phytochemicalConstituentQuery();
        this.descriptionRenderer = new DescriptionRenderer(PlantsParagraphCues.CUES);
        this.resilience = resilience;
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
        var plant = plantQuery.getByName(plantName);
        if (plant.isEmpty() || plant.get().species() == null) {
            return "redirect:/plants";
        }
        PlantSpecies species = plant.get().species().species();
        var cultivars = cultivarQuery.forPlantName(plantName).stream()
                .sorted(Comparator.comparing((Cultivar c) -> c.name().value()))
                .toList();
        var programs = plantProgramQuery.forPlantName(plantName).stream()
                .sorted(Comparator.comparing((PlantProgram p) -> p.name().value()))
                .toList();
        var constituents = phytochemicalConstituentQuery.forPlantName(plantName).stream()
                .sorted(Comparator.comparing((PhytochemicalConstituent c) -> c.name().value()))
                .toList();
        model.addAttribute("plant", species);
        model.addAttribute("ecologicalRole", plant.get().role());
        model.addAttribute("cultivars", cultivars);
        model.addAttribute("programs", programs);
        model.addAttribute("constituents", constituents);
        model.addAttribute("features", plant.get().features());
        model.addAttribute("images", plant.get().images().stream().toList());
        model.addAttribute("breadcrumb", breadcrumbToSpecies(species));
        model.addAttribute("cladeTrail", cladeTrailForSpecies(species));
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
        var segments = new ArrayList<BreadcrumbSegment>();
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
    private List<Clade> catalogCladeRoot() {
        List<Clade> orderClades = Pages.stream(1000, plantQuery.orders()::findPage)
                .map(PlantOrder::placedIn)
                .filter(clade -> clade != null)
                .toList();
        Clade sharedAncestor = lowestCommonAncestor(orderClades);
        if (sharedAncestor == null) {
            return List.of(new Plantae());
        }
        return CladeTraversal.ancestry(sharedAncestor).reversed();
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
        var plant = plantQuery.getByName(orderName);
        if (plant.isEmpty() || plant.get().order() == null) {
            return "redirect:/plants";
        }
        PlantOrder order = plant.get().order().order();
        model.addAttribute("order", order);
        model.addAttribute("children", plant.get().children());
        model.addAttribute("features", plant.get().features());
        model.addAttribute("images", plant.get().images().stream().toList());
        model.addAttribute("breadcrumb", breadcrumbToOrder(order));
        model.addAttribute("cladeTrail", cladeTrailFor(order));
        addDescription(model, order.description());
        return "plants/orders/detail";
    }

    /**
     * The phylogenetic tree-of-life row for an order: the clade lineage from the
     * plant kingdom (Plantae) down to the order's {@code placedIn} clade. Empty when
     * the order carries no placement — plant clades are supra-ordinal, so lower ranks
     * would resolve this by walking up to their order (deferred).
     * <p>
     * The row runs up to Eukaryota, the shared root where the plant and animal lineages
     * meet — the crossover point into the insect side. Plant clades link to their
     * in-console pages; Eukaryota links to the shared cross-domain tree-of-life browser.
     */
    private List<Clade> cladeTrailFor(PlantOrder order) {
        Clade placedIn = order.placedIn();
        if (placedIn == null) {
            return List.of();
        }
        // Root → placement, inclusive of Eukaryota: the shared root is the crossover
        // into the animal kingdom, so a plant naturalist can reach the insect side.
        return CladeTraversal.ancestry(placedIn).reversed();
    }

    // Lower ranks carry no clade of their own — plant clades are supra-ordinal — so a
    // family/genus/species resolves its trail by walking up to its order's placement.

    private List<Clade> cladeTrailForFamily(PlantFamily family) {
        return orderOf(family).map(this::cladeTrailFor).orElseGet(List::of);
    }

    private List<Clade> cladeTrailForGenus(PlantGenus genus) {
        return familyOf(genus).flatMap(this::orderOf).map(this::cladeTrailFor).orElseGet(List::of);
    }

    private List<Clade> cladeTrailForSpecies(PlantSpecies species) {
        return genusOf(species).flatMap(this::familyOf).flatMap(this::orderOf)
                .map(this::cladeTrailFor).orElseGet(List::of);
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
        if (PlantCladeTree.isAnimal(clade)) {
            return "redirect:/insects/clades/" + slug;
        }
        model.addAttribute("clade", clade);
        model.addAttribute("orders", ordersPlacedAt(clade));
        model.addAttribute("childClades", PlantCladeTree.narrower(clade));
        model.addAttribute("breadcrumb", plantaeRoot());
        model.addAttribute("cladeTrail", CladeTraversal.ancestry(clade).reversed());
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
    String familyDetail(@PathVariable String name, Model model) {
        var familyName = PlantFamilyName.of(name);
        var plant = plantQuery.getByName(familyName);
        if (plant.isEmpty() || plant.get().family() == null) {
            return "redirect:/plants";
        }
        PlantFamily family = plant.get().family().family();
        model.addAttribute("family", family);
        model.addAttribute("children", plant.get().children());
        model.addAttribute("features", plant.get().features());
        model.addAttribute("images", plant.get().images().stream().toList());
        model.addAttribute("breadcrumb", breadcrumbToFamily(family));
        model.addAttribute("cladeTrail", cladeTrailForFamily(family));
        addDescription(model, family.description());
        return "plants/families/detail";
    }

    @GetMapping("/genera/{name}")
    String genusDetail(@PathVariable String name, Model model) {
        var genusName = PlantGenusName.of(name);
        var plant = plantQuery.getByName(genusName);
        if (plant.isEmpty() || plant.get().genus() == null) {
            return "redirect:/plants";
        }
        PlantGenus genus = plant.get().genus().genus();
        model.addAttribute("genus", genus);
        model.addAttribute("family",
                plantQuery.families().getByName(genus.familyName()).orElse(null));
        model.addAttribute("children", plant.get().children());
        model.addAttribute("features", plant.get().features());
        model.addAttribute("images", plant.get().images().stream().toList());
        model.addAttribute("breadcrumb", breadcrumbToGenus(genus));
        model.addAttribute("cladeTrail", cladeTrailForGenus(genus));
        addDescription(model, genus.description());
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

    // ── Images ───────────────────────────────────────────────────────────

    @GetMapping("/images/{filename}")
    @Resilient(name = IMAGE_CONVERSION)
    ResponseEntity<byte[]> image(@PathVariable String filename) throws IOException {
        byte[] jpeg = jpegCache.get(filename);
        if (jpeg != null) {
            return jpegResponse(jpeg);
        }

        var resource = new ClassPathResource("plants/images/" + filename);
        if (!resource.exists()) {
            return ResponseEntity.notFound().build();
        }

        Path heicTemp = Files.createTempFile("plant-", ".heic");
        Path jpegTemp = Files.createTempFile("plant-", ".jpg");
        try {
            return resilience.timeout(IMAGE_CONVERSION).execute(() ->
                    convert(filename, resource, heicTemp, jpegTemp));
        } finally {
            Files.deleteIfExists(heicTemp);
            Files.deleteIfExists(jpegTemp);
        }
    }

    @GetMapping("/uploads/{filename:.+}")
    void serveUpload(@PathVariable String filename, HttpServletResponse response) throws IOException {
        var path = imageStorageService.resolve(filename);
        if (!Files.exists(path)) {
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }
        var contentType = Files.probeContentType(path);
        if (contentType == null) contentType = "application/octet-stream";
        response.setContentType(contentType);
        var safeName = path.getFileName().toString();
        response.setHeader("Content-Disposition", "inline; filename=\"" + safeName + "\"");
        response.setHeader("Cache-Control", "public, max-age=31536000, immutable");
        Files.copy(path, response.getOutputStream());
    }

    private ResponseEntity<byte[]> convert(String filename,
                                           ClassPathResource resource,
                                           Path heicTemp,
                                           Path jpegTemp) {
        try {
            Files.copy(resource.getInputStream(), heicTemp, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            var process = new ProcessBuilder("sips", "-s", "format", "jpeg",
                    "-s", "formatOptions", "80",
                    heicTemp.toString(), "--out", jpegTemp.toString())
                    .redirectErrorStream(true)
                    .start();
            int exit = process.waitFor();
            if (exit != 0) {
                return ResponseEntity.unprocessableEntity().build();
            }
            byte[] jpeg = Files.readAllBytes(jpegTemp);
            jpegCache.put(filename, jpeg);
            return jpegResponse(jpeg);
        } catch (IOException e) {
            throw new java.io.UncheckedIOException(e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }

    private ResponseEntity<byte[]> jpegResponse(byte[] jpeg) {
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_JPEG)
                .cacheControl(CacheControl.maxAge(1, TimeUnit.HOURS))
                .body(jpeg);
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
