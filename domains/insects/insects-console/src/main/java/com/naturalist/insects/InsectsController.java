package com.naturalist.insects;

import com.naturalist.clades.Clade;
import com.naturalist.exception.InvariantViolationException;
import com.naturalist.fieldnotes.Description;
import com.naturalist.naturalist.NaturalistName;
import com.naturalist.observability.Handling;
import com.naturalist.ddd.EntityName;
import com.naturalist.observability.Level;
import com.naturalist.observability.MethodObserver;
import com.naturalist.observability.Observer;
import com.naturalist.clades.CladeCatalog;
import com.naturalist.clades.CladeTraversal;
import com.naturalist.clades.Eukaryota;
import com.naturalist.clades.Insecta;
import com.naturalist.clades.Plantae;
import com.naturalist.data.FileName;
import com.naturalist.data.Page;
import com.naturalist.data.PageRequest;
import com.naturalist.data.Pages;
import com.naturalist.fieldnotes.render.DescriptionRenderer;
import com.naturalist.insects.render.InsectsParagraphCues;
import com.naturalist.library.CladeQuery;
import com.naturalist.library.CladeStep;
import com.naturalist.library.CladeView;
import com.naturalist.library.GlossaryLinker;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.Nullable;
import com.naturalist.resilience.BulkheadFullException;
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
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import com.naturalist.observation.GridCell;
import com.naturalist.observation.ImageRefusedException;
import com.naturalist.observation.ImageStore;
import com.naturalist.observation.ImageUpload;
import com.naturalist.observation.ImageVariant;
import com.naturalist.observation.StoredImage;
import com.naturalist.observation.OrganismObservation;

@Controller
@RequestMapping("/insects")
public class InsectsController {
    private static final String CSRF_REQUEST_ATTRIBUTE = "org.springframework.security.web.csrf.CsrfToken";

    // Written by the app's NaturalistHeaderInterceptor (same literal, by convention).
    private static final String CURRENT_NATURALIST_ATTRIBUTE = "naturalist.currentNaturalistName";

    private static java.util.Optional<NaturalistName> currentNaturalist(
            HttpServletRequest request) {
        Object slug = request.getAttribute(CURRENT_NATURALIST_ATTRIBUTE);
        return slug instanceof String s && !s.isBlank()
                ? java.util.Optional.of(NaturalistName.of(s))
                : java.util.Optional.empty();
    }

    // Session flag toggled by POST /insects/collection-lens; read on every insects browse request.
    private static final String COLLECTION_LENS_ATTRIBUTE = "insects.collectionLens";

    private static boolean collectionLensOn(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        return session != null && Boolean.TRUE.equals(session.getAttribute(COLLECTION_LENS_ATTRIBUTE));
    }

    private static String safeReturn(String returnTo) {
        return returnTo != null
                && returnTo.startsWith("/insects")
                && !returnTo.startsWith("/insects/collection-lens")
                ? returnTo : "/insects/species";
    }

    private final InsectQuery insectQuery;
    private final InsectViewQuery insectViewQuery;
    private final InsectCommand insectCommand;
    private final CladeQuery cladeQuery;
    private final DescriptionRenderer descriptionRenderer;
    private final ImageStore imageStore;
    private final GlossaryLinker glossaryLinker;

    InsectsController(InsectQuery insectQuery,
                      InsectViewQuery insectViewQuery,
                      InsectCommand insectCommand,
                      CladeQuery cladeQuery,
                      com.naturalist.library.GlossaryTermQuery glossaryTermQuery,
                      ImageStore imageStore) {
        this.insectQuery = insectQuery;
        this.insectViewQuery = insectViewQuery;
        this.insectCommand = insectCommand;
        this.cladeQuery = cladeQuery;
        this.descriptionRenderer = new DescriptionRenderer(InsectsParagraphCues.CUES);
        this.imageStore = imageStore;
        this.glossaryLinker = GlossaryLinker.of(glossaryTermQuery
                .findPage(PageRequest.first(PageRequest.MAX_PAGE_SIZE)).content());
    }

    /**
     * Builds an {@link AncestorIntro} from a rank label plus its
     * {@link com.naturalist.fieldnotes.Description}. The {@code storageKey}
     * is keyed by rank-type (not by specific entity name) so opening one
     * Order panel keeps every Order panel open across pages — the user is
     * signalling "I want rank-context", not "I want this specific order".
     */
    private AncestorIntro intro(String label, String storageKey,
                                Description d) {
        return new AncestorIntro(label, storageKey,
                descriptionRenderer.render(d.preschool()),
                descriptionRenderer.render(d.elementary()),
                descriptionRenderer.render(d.secondary()),
                descriptionRenderer.render(d.university()));
    }

    private AncestorIntro classInsectaIntro() {
        return intro("Class Insecta", "intro-class-open", new Insecta().description());
    }

    /**
     * Class Insecta only — the baseline ancestor stack for listing pages
     * and the order detail page (no Linnaean rank sits above Order).
     */
    private List<AncestorIntro> classOnlyIntros() {
        return List.of(classInsectaIntro());
    }

    private List<AncestorIntro> introsForFamily(InsectOrderInfo order) {
        return List.of(
                classInsectaIntro(),
                intro("Order " + order.order().value(), "intro-order-open", order.description()));
    }

    private List<AncestorIntro> introsForGenus(InsectOrderInfo order, InsectFamilyInfo family) {
        return List.of(
                classInsectaIntro(),
                intro("Order " + order.order().value(), "intro-order-open", order.description()),
                intro("Family " + family.family().value(), "intro-family-open", family.description()));
    }

    private List<AncestorIntro> introsForSpecies(InsectOrderInfo order, InsectFamilyInfo family,
                                                 InsectGenusInfo genus) {
        return List.of(
                classInsectaIntro(),
                intro("Order " + order.order().value(), "intro-order-open", order.description()),
                intro("Family " + family.family().value(), "intro-family-open", family.description()),
                intro("Genus " + genus.genus().value(), "intro-genus-open", genus.description()));
    }

    private List<AncestorIntro> introsForLifeStages(InsectOrderInfo order, InsectFamilyInfo family,
                                                    InsectGenusInfo genus, InsectSpeciesInfo species) {
        String binomial = genus.genus().value() + " " + species.epithet().value();
        return List.of(
                classInsectaIntro(),
                intro("Order " + order.order().value(), "intro-order-open", order.description()),
                intro("Family " + family.family().value(), "intro-family-open", family.description()),
                intro("Genus " + genus.genus().value(), "intro-genus-open", genus.description()),
                intro("Species " + binomial, "intro-species-open", species.description()));
    }

    /**
     * Linnaean rank label for each Insecta-ancestry clade that has one.
     * The kernel deliberately keeps clades and ranks as separate concepts
     * (clades = evolutionary tree, ranks = Linnaean scheme); the controller
     * supplies the rank label at the breadcrumb construction site for those
     * clades that happen to coincide with a named rank.
     */
    private static final Map<String, String> CLADE_RANK_LABEL = Map.of(
            "animalia", "Kingdom",
            "arthropoda", "Phylum",
            "insecta", "Class");

    /**
     * URL overrides for clades that own a landing page elsewhere in this
     * console. Insecta points at the catalog root (the orders listing) so its
     * breadcrumb segment is a click-back-to-the-top affordance. Every other
     * clade falls back to its in-console clade page at {@code /insects/clades/{slug}} —
     * never the shared tree-of-life browser.
     */
    private static final Map<String, String> CLADE_URL = Map.of(
            "insecta", "/insects/orders");

    private List<BreadcrumbSegment> cladePrefix() {
        return CladeTraversal.ancestry(new Insecta()).reversed().stream()
                .filter(c -> !(c instanceof Eukaryota))
                .map(c -> {
                    String rank = CLADE_RANK_LABEL.get(c.slug());
                    String url = CLADE_URL.getOrDefault(c.slug(), "/insects/clades/" + c.slug());
                    return BreadcrumbSegment.link(c.displayName(), url, rank);
                })
                .toList();
    }

    /**
     * The taxonomic breadcrumb for a clade page: the clade's <em>ranked</em> ancestors
     * (Kingdom Animalia → Phylum Arthropoda → Class Insecta …) and no further. A clade can
     * hold taxa placed at several ranks at once — Hemiptera carries both orders and genera —
     * so there is no single rank path to descend into; the ladder simply stops at the last
     * rank the clade unambiguously sits below.
     */
    private List<BreadcrumbSegment> cladeTaxonomicPrefix(Clade clade) {
        return CladeTraversal.ancestry(clade).reversed().stream()
                .filter(c -> CLADE_RANK_LABEL.containsKey(c.slug()))
                .map(c -> {
                    String rank = CLADE_RANK_LABEL.get(c.slug());
                    String url = CLADE_URL.getOrDefault(c.slug(), "/insects/clades/" + c.slug());
                    return BreadcrumbSegment.link(c.displayName(), url, rank);
                })
                .toList();
    }

    private List<BreadcrumbSegment> breadcrumbToOrder(InsectOrderInfo order) {
        ArrayList<BreadcrumbSegment> segments = new ArrayList<>(cladePrefix());
        segments.add(BreadcrumbSegment.current(order.order().value(), "Order"));
        return segments;
    }

    private List<BreadcrumbSegment> breadcrumbToFamily(InsectFamilyInfo family, InsectOrderInfo order) {
        ArrayList<BreadcrumbSegment> segments = new ArrayList<>(cladePrefix());
        segments.add(BreadcrumbSegment.link(order.order().value(),
                "/insects/orders/" + order.name().value(), "Order"));
        segments.add(BreadcrumbSegment.current(family.family().value(), "Family"));
        return segments;
    }

    private List<BreadcrumbSegment> breadcrumbToGenus(InsectGenusInfo genus,
                                                      InsectFamilyInfo family,
                                                      InsectOrderInfo order) {
        ArrayList<BreadcrumbSegment> segments = new ArrayList<>(cladePrefix());
        segments.add(BreadcrumbSegment.link(order.order().value(),
                "/insects/orders/" + order.name().value(), "Order"));
        segments.add(BreadcrumbSegment.link(family.family().value(),
                "/insects/families/" + family.name().value(), "Family"));
        segments.add(BreadcrumbSegment.current(genus.genus().value(), "Genus"));
        return segments;
    }

    private List<BreadcrumbSegment> breadcrumbToSpecies(InsectSpeciesInfo species,
                                                        InsectGenusInfo genus,
                                                        InsectFamilyInfo family,
                                                        InsectOrderInfo order) {
        ArrayList<BreadcrumbSegment> segments = new ArrayList<>(cladePrefix());
        segments.add(BreadcrumbSegment.link(order.order().value(),
                "/insects/orders/" + order.name().value(), "Order"));
        segments.add(BreadcrumbSegment.link(family.family().value(),
                "/insects/families/" + family.name().value(), "Family"));
        segments.add(BreadcrumbSegment.link(genus.genus().value(),
                "/insects/genera/" + genus.name().value(), "Genus"));
        segments.add(BreadcrumbSegment.current(
                genus.genus().value() + " " + species.epithet().value(), "Species"));
        return segments;
    }

    private CladeTrail cladeTrail(List<InsectCladeAnchors.LineageEntry> lineage) {
        MethodObserver methodObserver = observer.forMethod("cladeTrail");
        methodObserver.arguments(i -> i
                .notNull(lineage, "lineage"))
                .throwWhenInvalid();
        InsectCladeAnchors.Anchor anchor = InsectCladeAnchors.resolve(lineage);
        Optional<CladeView> found = cladeQuery.getBySlug(anchor.cladeSlug());
        methodObserver.state(s -> s
                .isPresent(found, "anchorClade"))
                .throwWhenInvalid();
        CladeView view = found.orElseThrow();
        List<CladeStep> steps = new ArrayList<>(view.ancestry());
        steps.add(view.subject());
        return new CladeTrail(steps, anchor.gapLabel());
    }

    // ── Clade pages (the tree of life, kept in-console) ──────────────────

    /**
     * A clade's page in the insect catalog: its description, the insect taxa placed directly
     * at it grouped by Linnaean rank (Order / Family / Genus / Species — insects place clades
     * at every rank, unlike the supra-ordinal plant clades), and its narrower child clades.
     * The insect-side counterpart to the plants console's {@code /plants/clades/{slug}}, so the
     * tree-of-life breadcrumb navigates in-console and never strands the naturalist in the
     * shared browser. Plant clades bounce to the plants console; unknown slugs to the catalog.
     */
    @GetMapping("/clades/{slug}")
    String cladeDetail(@PathVariable String slug, Model model) {
        Optional<Clade> found = Clade.find(slug);
        if (found.isEmpty()) {
            return "redirect:/insects/orders";
        }
        Clade clade = found.get();
        if (CladeTraversal.ancestry(clade).stream().anyMatch(node -> node instanceof Plantae)) {
            return "redirect:/plants/clades/" + slug;
        }
        Description description = clade.description();
        model.addAttribute("clade", clade);
        model.addAttribute("orders", ordersPlacedAt(clade));
        model.addAttribute("families", familiesPlacedAt(clade));
        model.addAttribute("genera", generaPlacedAt(clade));
        model.addAttribute("species", speciesPlacedAt(clade));
        model.addAttribute("childClades", CladeCatalog.childrenOf(clade));
        model.addAttribute("breadcrumb", cladeTaxonomicPrefix(clade));
        model.addAttribute("cladeTrail", cladeTrailAt(slug));
        model.addAttribute("descriptionPreschool", descriptionRenderer.render(description.preschool()));
        model.addAttribute("descriptionElementary", descriptionRenderer.render(description.elementary()));
        model.addAttribute("descriptionSecondary", descriptionRenderer.render(description.secondary()));
        model.addAttribute("descriptionUniversity", descriptionRenderer.render(description.university()));
        return "insects/clades/detail";
    }

    private CladeTrail cladeTrailAt(String slug) {
        Optional<CladeView> found = cladeQuery.getBySlug(slug);
        observer.state("cladeTrailAt", s -> s
                .isPresent(found, "clade"))
                .throwWhenInvalid();
        CladeView view = found.orElseThrow();
        List<CladeStep> steps = new ArrayList<>(view.ancestry());
        steps.add(view.subject());
        return new CladeTrail(steps, null);
    }

    private List<CladeTaxonCard> ordersPlacedAt(Clade clade) {
        return Pages.stream(1000, insectQuery.orders()::findPage)
                .filter(o -> clade.equals(o.placedIn()))
                .sorted(Comparator.comparing((InsectOrderInfo o) -> o.order().value()))
                .map(o -> new CladeTaxonCard("/insects/orders/" + o.name().value(), o.order().value()))
                .toList();
    }

    private List<CladeTaxonCard> familiesPlacedAt(Clade clade) {
        return Pages.stream(1000, insectQuery.families()::findPage)
                .filter(f -> clade.equals(f.placedIn()))
                .sorted(Comparator.comparing((InsectFamilyInfo f) -> f.family().value()))
                .map(f -> new CladeTaxonCard("/insects/families/" + f.name().value(), f.family().value()))
                .toList();
    }

    private List<CladeTaxonCard> generaPlacedAt(Clade clade) {
        return Pages.stream(1000, insectQuery.genera()::findPage)
                .filter(g -> clade.equals(g.placedIn()))
                .sorted(Comparator.comparing((InsectGenusInfo g) -> g.genus().value()))
                .map(g -> new CladeTaxonCard("/insects/genera/" + g.name().value(), g.genus().value()))
                .toList();
    }

    private List<CladeTaxonCard> speciesPlacedAt(Clade clade) {
        List<InsectSpeciesInfo> placedSpecies = Pages.stream(1000, insectQuery.species()::findPage)
                .filter(s -> clade.equals(s.placedIn()))
                .toList();
        java.util.Set<InsectGenusName> genusNameSet = placedSpecies.stream()
                .map(InsectSpeciesInfo::genusName)
                .collect(java.util.stream.Collectors.toSet());
        Map<InsectGenusName, InsectGenusInfo> genusByName = new LinkedHashMap<>();
        insectQuery.genera().findByNameSet(genusNameSet).stream()
                .forEach(genus -> genusByName.put(genus.name(), genus));
        return placedSpecies.stream()
                .map(s -> {
                    InsectGenusInfo genus = genusByName.get(s.genusName());
                    String genusEpithet = genus != null ? genus.genus().value() : "";
                    return new CladeTaxonCard("/insects/" + s.name().value(),
                            (genusEpithet + " " + s.epithet().value()).trim());
                })
                .sorted(Comparator.comparing(CladeTaxonCard::name))
                .toList();
    }

    private List<InsectCladeAnchors.LineageEntry> lineageToOrder(InsectOrderInfo order) {
        return List.of(new InsectCladeAnchors.LineageEntry(
                order.name().value(), order.order().value()));
    }

    private List<InsectCladeAnchors.LineageEntry> lineageToFamily(InsectFamilyInfo family, InsectOrderInfo order) {
        return List.of(
                new InsectCladeAnchors.LineageEntry(family.name().value(), family.family().value()),
                new InsectCladeAnchors.LineageEntry(order.name().value(), order.order().value()));
    }

    private List<InsectCladeAnchors.LineageEntry> lineageToGenus(InsectGenusInfo genus,
                                                                 InsectFamilyInfo family,
                                                                 InsectOrderInfo order) {
        return List.of(
                new InsectCladeAnchors.LineageEntry(genus.name().value(), genus.genus().value()),
                new InsectCladeAnchors.LineageEntry(family.name().value(), family.family().value()),
                new InsectCladeAnchors.LineageEntry(order.name().value(), order.order().value()));
    }

    private List<InsectCladeAnchors.LineageEntry> lineageToSpecies(InsectSpeciesInfo species,
                                                                   InsectGenusInfo genus,
                                                                   InsectFamilyInfo family,
                                                                   InsectOrderInfo order) {
        String binomial = genus.genus().value() + " " + species.epithet().value();
        return List.of(
                new InsectCladeAnchors.LineageEntry(species.name().value(), binomial),
                new InsectCladeAnchors.LineageEntry(genus.name().value(), genus.genus().value()),
                new InsectCladeAnchors.LineageEntry(family.name().value(), family.family().value()),
                new InsectCladeAnchors.LineageEntry(order.name().value(), order.order().value()));
    }

    @GetMapping
    String index() {
        return "redirect:/insects/orders";
    }

    @GetMapping("/identify")
    String identifyForm(@RequestParam(name = "identified", required = false) String identified,
                        @RequestParam(name = "limit", required = false) String limit,
                        @RequestParam(name = "error", required = false) String error,
                        HttpServletRequest request, Model model) {
        Object csrf = request.getAttribute(CSRF_REQUEST_ATTRIBUTE);
        if (csrf != null) {
            model.addAttribute("_csrf", csrf);
        }
        if (StringUtils.isNotBlank(identified)) {
            model.addAttribute("identified", identified);
        }
        if (StringUtils.isNotBlank(limit)) {
            model.addAttribute("limit", limit);
        }
        if ("image".equals(error)) {
            model.addAttribute("imageError", true);
        }
        if ("area".equals(error)) {
            model.addAttribute("areaError", true);
        }
        if ("notes".equals(error)) {
            model.addAttribute("notesError", true);
        }
        return "insects/identify";
    }

    @PostMapping("/identify")
    String identify(@RequestParam("image") MultipartFile imageFile,
                    @RequestParam(name = "location", required = false) String location,
                    @RequestParam(name = "capturedAt", required = false) String capturedAt,
                    @RequestParam(name = "notes", required = false) String notes,
                    @RequestParam(name = "areaLat", required = false) String areaLat,
                    @RequestParam(name = "areaLon", required = false) String areaLon,
                    HttpServletRequest request) throws IOException {
        Optional<NaturalistName> me = currentNaturalist(request);
        if (me.isEmpty()) {
            return "redirect:/insects/identify";
        }
        MethodObserver methodObserver = observer.forMethod("identify");
        if (notesTooLong(methodObserver, notes)) {
            return "redirect:/insects/identify?error=notes";
        }

        Instant capturedInstant = null;
        if (StringUtils.isNotBlank(capturedAt)) {
            try {
                capturedInstant = Instant.parse(capturedAt);
            }
            catch (java.time.format.DateTimeParseException ignored) {
            }
        }

        GridCell area = GridCell.of(areaLat, areaLon);
        if (areaOffTheLattice(methodObserver, area)) {
            // A claimed cell that is unreadable or off the lattice -- the image is not read, nothing is stored.
            return "redirect:/insects/identify?error=area";
        }

        ImageUpload upload = ImageUpload.of(imageFile.getBytes());
        if (!upload.isValid()) {
            // The upload is not a JPEG/PNG/WebP (or is oversize) -- nothing is stored.
            return "redirect:/insects/identify?error=image";
        }

        try {
            imageStore.requireStorable(upload);
        } catch (InvariantViolationException unstorable) {
            // A valid JPEG/PNG/WebP the store cannot use (undecodable, or a declared pixel bomb) -- nothing is stored.
            return "redirect:/insects/identify?error=image";
        }

        try {
            InsectRankName rankName = insectCommand.identification().identify(
                    upload,
                    new com.naturalist.vision.ImageMetadata(location, capturedInstant),
                    me.get(), notes, area);
            return "redirect:/insects/" + rankName.value();
        } catch (com.naturalist.identification.IdentificationRefused refused) {
            // The rate gate, the bulkheads and the budget all live behind insectCommand.identification()
            // (InsectIdentificationCommandImpl), ahead of any vision call; a full store-write bulkhead is BUSY.
            // "RATE" is a UI-only query-param string, not a usage.LimitKind; a budget refusal's
            // detail is the exhausted LimitKind's name (DAILY / MONTHLY ...).
            return switch (refused.reason()) {
                case RATE_LIMITED -> "redirect:/insects/identify?limit=RATE";
                case BUDGET_EXHAUSTED -> "redirect:/insects/identify?limit=" + refused.detail();
                case BUSY -> "redirect:/insects/identify?limit=BUSY";
            };
        } catch (ImageRefusedException unstorable) {
            // the store refused the photo's body (counted there), before any budget was reserved -- nothing is stored
            return "redirect:/insects/identify?error=image";
        }
    }

    @GetMapping("/species")
    String list(@RequestParam(defaultValue = "0") int page,
                @RequestParam(name = "error", required = false) String error,
                HttpServletRequest request, Model model) {
        java.util.Optional<NaturalistName> viewer = currentNaturalist(request);
        boolean mine = collectionLensOn(request) && viewer.isPresent();
        PageRequest pageRequest = PageRequest.console(Math.max(0, page));
        Page<InsectViews.SpeciesCard> cards = mine
                ? insectViewQuery.collectedBy(viewer.get(), pageRequest)
                : insectViewQuery.speciesCards(pageRequest, viewer.orElse(null));
        model.addAttribute("cards", cards);
        model.addAttribute("mine", mine);
        model.addAttribute("nameError", "name".equals(error));
        model.addAttribute("ancestorIntros", classOnlyIntros());
        model.addAttribute("breadcrumb", cladePrefix());
        model.addAttribute("cladeTrail", cladeTrail(List.of()));
        return "insects/list";
    }

    @GetMapping("/families")
    String families(@RequestParam(defaultValue = "0") int page, Model model) {
        model.addAttribute("cards", insectViewQuery.familyCards(PageRequest.console(Math.max(0, page))));
        model.addAttribute("ancestorIntros", classOnlyIntros());
        model.addAttribute("breadcrumb", cladePrefix());
        model.addAttribute("cladeTrail", cladeTrail(List.of()));
        return "insects/families";
    }

    @GetMapping("/families/{name}")
    String familyDetail(@PathVariable InsectFamilyName name,
                        @RequestParam(name = "error", required = false) String error,
                        HttpServletRequest request, HttpServletResponse response, Model model) {
        MethodObserver methodObserver = observer.forMethod("familyDetail");
        if (methodObserver.arguments(c -> c
                .entityName(name, "name"))
                .observe(Level.INFO)
                .isNotValid()) {
            return notFound(response, model, name, "/insects/families");
        }
        Optional<InsectViews.RankPage> rankPage = insectViewQuery.rankPage(name, currentNaturalist(request).orElse(null));
        return methodObserver.state(c -> c
                .isPresent(rankPage, "nameFound"))
                .observe(Level.INFO)
                .whenNotValid(() -> notFound(response, model, name, "/insects/families"))
                .otherwise(() -> familyPage(name, rankPage.get(), error, response, model));
    }

    private String familyPage(InsectFamilyName name, InsectViews.RankPage rankPage, @Nullable String error,
                              HttpServletResponse response, Model model) {
        if (observer.arguments("familyPage", c -> c
                .readModel(rankPage, "rankPage")
                .isTrue(rankPage.insect(), Insect::hasFamily, "hasFamily"))
                .observe(Level.WARN)
                .isNotValid()) {
            // the catalog holds it, but the read model is invalid: counted to fix, answered as not found
            return notFound(response, model, name, "/insects/families");
        }
        // Insect.invariants() requires the order whenever the family is present
        Insect insect = rankPage.insect();
        InsectFamilyInfo family = insect.family().info();
        InsectOrderInfo order = insect.order().info();
        Description description = family.description();
        model.addAttribute("page", rankPage);
        model.addAttribute("glossaryLinker", glossaryLinker);
        model.addAttribute("descriptionPreschool", descriptionRenderer.render(description.preschool()));
        model.addAttribute("descriptionElementary", descriptionRenderer.render(description.elementary()));
        model.addAttribute("descriptionSecondary", descriptionRenderer.render(description.secondary()));
        model.addAttribute("descriptionUniversity", descriptionRenderer.render(description.university()));
        model.addAttribute("breadcrumb", breadcrumbToFamily(family, order));
        model.addAttribute("cladeTrail", cladeTrail(lineageToFamily(family, order)));
        model.addAttribute("ancestorIntros", introsForFamily(order));
        if ("notes".equals(error)) {
            model.addAttribute("notesError", true);
        }
        if ("observation".equals(error)) {
            model.addAttribute("observationError", true);
        }
        return "insects/family";
    }

    @GetMapping("/orders")
    String orders(@RequestParam(defaultValue = "0") int page, Model model) {
        model.addAttribute("cards", insectViewQuery.orderCards(PageRequest.console(Math.max(0, page))));
        model.addAttribute("ancestorIntros", classOnlyIntros());
        model.addAttribute("breadcrumb", cladePrefix());
        model.addAttribute("cladeTrail", cladeTrail(List.of()));
        return "insects/orders";
    }

    @GetMapping("/orders/{name}")
    String orderDetail(@PathVariable InsectOrderName name,
                       @RequestParam(name = "error", required = false) String error,
                       HttpServletRequest request, HttpServletResponse response, Model model) {
        MethodObserver methodObserver = observer.forMethod("orderDetail");
        if (methodObserver.arguments(c -> c
                .entityName(name, "name"))
                .observe(Level.INFO)
                .isNotValid()) {
            return notFound(response, model, name, "/insects/orders");
        }
        Optional<InsectViews.RankPage> rankPage = insectViewQuery.rankPage(name, currentNaturalist(request).orElse(null));
        return methodObserver.state(c -> c
                .isPresent(rankPage, "nameFound"))
                .observe(Level.INFO)
                .whenNotValid(() -> notFound(response, model, name, "/insects/orders"))
                .otherwise(() -> orderPage(name, rankPage.get(), error, response, model));
    }

    private String orderPage(InsectOrderName name, InsectViews.RankPage rankPage, @Nullable String error,
                             HttpServletResponse response, Model model) {
        if (observer.arguments("orderPage", c -> c
                .readModel(rankPage, "rankPage")
                .isTrue(rankPage.insect(), Insect::hasOrder, "hasOrder"))
                .observe(Level.WARN)
                .isNotValid()) {
            // the catalog holds it, but the read model is invalid: counted to fix, answered as not found
            return notFound(response, model, name, "/insects/orders");
        }
        InsectOrderInfo order = rankPage.insect().order().info();
        Description description = order.description();
        model.addAttribute("page", rankPage);
        model.addAttribute("glossaryLinker", glossaryLinker);
        model.addAttribute("descriptionPreschool", descriptionRenderer.render(description.preschool()));
        model.addAttribute("descriptionElementary", descriptionRenderer.render(description.elementary()));
        model.addAttribute("descriptionSecondary", descriptionRenderer.render(description.secondary()));
        model.addAttribute("descriptionUniversity", descriptionRenderer.render(description.university()));
        model.addAttribute("breadcrumb", breadcrumbToOrder(order));
        model.addAttribute("cladeTrail", cladeTrail(lineageToOrder(order)));
        model.addAttribute("ancestorIntros", classOnlyIntros());
        if ("notes".equals(error)) {
            model.addAttribute("notesError", true);
        }
        if ("observation".equals(error)) {
            model.addAttribute("observationError", true);
        }
        return "insects/order";
    }

    @GetMapping("/genera")
    String genera(@RequestParam(defaultValue = "0") int page, Model model) {
        model.addAttribute("cards", insectViewQuery.genusCards(PageRequest.console(Math.max(0, page))));
        model.addAttribute("ancestorIntros", classOnlyIntros());
        model.addAttribute("breadcrumb", cladePrefix());
        model.addAttribute("cladeTrail", cladeTrail(List.of()));
        return "insects/genera";
    }

    @GetMapping("/genera/{name}")
    String genusDetail(@PathVariable InsectGenusName name,
                       @RequestParam(name = "error", required = false) String error,
                       HttpServletRequest request, HttpServletResponse response, Model model) {
        MethodObserver methodObserver = observer.forMethod("genusDetail");
        if (methodObserver.arguments(c -> c
                .entityName(name, "name"))
                .observe(Level.INFO)
                .isNotValid()) {
            return notFound(response, model, name, "/insects/genera");
        }
        Optional<InsectViews.RankPage> rankPage = insectViewQuery.rankPage(name, currentNaturalist(request).orElse(null));
        return methodObserver.state(c -> c
                .isPresent(rankPage, "nameFound"))
                .observe(Level.INFO)
                .whenNotValid(() -> notFound(response, model, name, "/insects/genera"))
                .otherwise(() -> genusPage(name, rankPage.get(), error, response, model));
    }

    private String genusPage(InsectGenusName name, InsectViews.RankPage rankPage, @Nullable String error,
                             HttpServletResponse response, Model model) {
        if (observer.arguments("genusPage", c -> c
                .readModel(rankPage, "rankPage")
                .isTrue(rankPage.insect(), Insect::hasGenus, "hasGenus"))
                .observe(Level.WARN)
                .isNotValid()) {
            // the catalog holds it, but the read model is invalid: counted to fix, answered as not found
            return notFound(response, model, name, "/insects/genera");
        }
        // Insect.invariants() requires the family and order whenever the genus is present
        Insect insect = rankPage.insect();
        InsectGenusInfo genus = insect.genus().info();
        InsectFamilyInfo family = insect.family().info();
        InsectOrderInfo order = insect.order().info();
        Description description = genus.description();
        model.addAttribute("page", rankPage);
        model.addAttribute("glossaryLinker", glossaryLinker);
        model.addAttribute("descriptionPreschool", descriptionRenderer.render(description.preschool()));
        model.addAttribute("descriptionElementary", descriptionRenderer.render(description.elementary()));
        model.addAttribute("descriptionSecondary", descriptionRenderer.render(description.secondary()));
        model.addAttribute("descriptionUniversity", descriptionRenderer.render(description.university()));
        model.addAttribute("breadcrumb", breadcrumbToGenus(genus, family, order));
        model.addAttribute("cladeTrail", cladeTrail(lineageToGenus(genus, family, order)));
        model.addAttribute("ancestorIntros", introsForGenus(order, family));
        if ("notes".equals(error)) {
            model.addAttribute("notesError", true);
        }
        if ("observation".equals(error)) {
            model.addAttribute("observationError", true);
        }
        return "insects/genus";
    }

    @GetMapping("/guild/{guild}")
    String guild(@PathVariable String guild, Model model) {
        FunctionalGuild selected;
        try {
            selected = FunctionalGuild.valueOf(guild.toUpperCase());
        } catch (IllegalArgumentException e) {
            return "redirect:/insects";
        }
        List<InsectFunctionalRole> roles = insectQuery.functionalRoles().getByGuild(selected).sortedByParentName().stream().toList();
        model.addAttribute("selectedGuild", selected);
        model.addAttribute("guilds", FunctionalGuild.values());
        model.addAttribute("roles", roles);
        model.addAttribute("ancestorIntros", classOnlyIntros());
        model.addAttribute("breadcrumb", cladePrefix());
        model.addAttribute("cladeTrail", cladeTrail(List.of()));
        return "insects/guild";
    }

    @GetMapping("/{name}")
    String detail(@PathVariable InsectSpeciesName name,
                  @RequestParam(name = "error", required = false) String error,
                  HttpServletRequest request, HttpServletResponse response, Model model) {
        MethodObserver methodObserver = observer.forMethod("detail");
        if (methodObserver.arguments(c -> c
                .entityName(name, "name"))
                .observe(Level.INFO)
                .isNotValid()) {
            return notFound(response, model, name, "/insects/species");
        }
        Optional<NaturalistName> viewer = currentNaturalist(request);
        Optional<InsectViews.RankPage> rankPage = insectViewQuery.rankPage(name, viewer.orElse(null));
        return methodObserver.state(c -> c
                .isPresent(rankPage, "nameFound"))
                .observe(Level.INFO)
                .whenNotValid(() -> notFound(response, model, name, "/insects/species"))
                .otherwise(() -> speciesPage(name, rankPage.get(), viewer.isPresent(), error, request, response,
                        model));
    }

    private String speciesPage(InsectSpeciesName name, InsectViews.RankPage rankPage, boolean signedIn,
                               @Nullable String error, HttpServletRequest request, HttpServletResponse response,
                               Model model) {
        if (observer.arguments("speciesPage", c -> c
                .readModel(rankPage, "rankPage")
                .isTrue(rankPage.insect(), Insect::hasSpecies, "hasSpecies"))
                .observe(Level.WARN)
                .isNotValid()) {
            // the catalog holds it, but the read model is invalid: counted to fix, answered as not found
            return notFound(response, model, name, "/insects/species");
        }
        // Insect.invariants() requires the genus, family and order whenever the species is present
        Insect i = rankPage.insect();
        InsectSpeciesInfo s = i.species().info();
        InsectGenusInfo genus = i.genus().info();
        InsectFamilyInfo family = i.family().info();
        InsectOrderInfo order = i.order().info();
        Description description = s.description();
        if ("image".equals(error)) {
            model.addAttribute("imageError", true);
        }
        if ("area".equals(error)) {
            model.addAttribute("areaError", true);
        }
        if ("notes".equals(error)) {
            model.addAttribute("notesError", true);
        }
        if ("observation".equals(error)) {
            model.addAttribute("observationError", true);
        }
        if ("busy".equals(error)) {
            model.addAttribute("busyError", true);
        }
        model.addAttribute("page", rankPage);
        model.addAttribute("lens", collectionLensOn(request) && signedIn);
        model.addAttribute("glossaryLinker", glossaryLinker);
        model.addAttribute("descriptionPreschool", descriptionRenderer.render(description.preschool()));
        model.addAttribute("descriptionElementary", descriptionRenderer.render(description.elementary()));
        model.addAttribute("descriptionSecondary", descriptionRenderer.render(description.secondary()));
        model.addAttribute("descriptionUniversity", descriptionRenderer.render(description.university()));
        model.addAttribute("breadcrumb", breadcrumbToSpecies(s, genus, family, order));
        model.addAttribute("cladeTrail", cladeTrail(lineageToSpecies(s, genus, family, order)));
        model.addAttribute("ancestorIntros", introsForSpecies(order, family, genus));
        return "insects/detail";
    }

    @PostMapping("/collection-lens")
    String collectionLens(@RequestParam("on") boolean on,
                          @RequestParam(name = "return", required = false) String returnTo,
                          HttpServletRequest request) {
        if (currentNaturalist(request).isPresent()) {
            request.getSession(true).setAttribute(COLLECTION_LENS_ATTRIBUTE, on);
        }
        return "redirect:" + safeReturn(returnTo);
    }

    @PostMapping("/{name}/observe")
    String observe(@PathVariable InsectSpeciesName name,
                   @RequestParam(name = "notes", required = false) String notes,
                   HttpServletRequest request) {
        MethodObserver methodObserver = observer.forMethod("observe");
        if (methodObserver.arguments(c -> c
                .entityName(name, "name"))
                .observe(Level.INFO)
                .isNotValid()) {
            return "redirect:/insects/species?error=name";
        }
        java.util.Optional<NaturalistName> me = currentNaturalist(request);
        if (me.isEmpty()) {
            return "redirect:/insects/" + name;
        }
        if (notesTooLong(methodObserver, notes)) {
            return "redirect:/insects/" + name + "?error=notes";
        }
        insectCommand.sightings().record(name, me.get(), notes);
        return "redirect:/insects/" + name;
    }

    @PostMapping("/{name}/images")
    String addImage(@PathVariable InsectSpeciesName name,
                    @RequestParam("image") MultipartFile imageFile,
                    @RequestParam(name = "location", required = false) String location,
                    @RequestParam(name = "notes", required = false) String notes,
                    @RequestParam(name = "areaLat", required = false) String areaLat,
                    @RequestParam(name = "areaLon", required = false) String areaLon,
                    HttpServletRequest request) throws IOException {
        MethodObserver methodObserver = observer.forMethod("addImage");
        if (methodObserver.arguments(c -> c
                .entityName(name, "name"))
                .observe(Level.INFO)
                .isNotValid()) {
            return "redirect:/insects/species?error=name";
        }
        if (notesTooLong(methodObserver, notes)) {
            return "redirect:/insects/" + name + "?error=notes";
        }
        Optional<NaturalistName> me = currentNaturalist(request);
        GridCell area = GridCell.of(areaLat, areaLon);
        if (areaOffTheLattice(methodObserver, area)) {
            // A claimed cell that is unreadable or off the lattice -- the image is not read, nothing is stored.
            return "redirect:/insects/" + name + "?error=area";
        }
        ImageUpload upload = ImageUpload.of(imageFile.getBytes());
        if (!upload.isValid()) {
            return "redirect:/insects/" + name + "?error=image";
        }

        try {
            imageStore.requireStorable(upload);
        } catch (InvariantViolationException unstorable) {
            // A valid JPEG/PNG/WebP the store cannot use (undecodable, or a declared pixel bomb) -- nothing is stored.
            return "redirect:/insects/" + name + "?error=image";
        }
        try {
            insectCommand.photos().add(name, upload, me.orElse(null), notes, location, area);
        } catch (ImageRefusedException unstorable) {
            // the store refused the photo's body (counted there) -- nothing is stored
            return "redirect:/insects/" + name + "?error=image";
        } catch (BulkheadFullException busy) {
            // the store's write cap is full -- a refusal like identify's notBusy: nothing is stored, the naturalist tries again
            methodObserver.state(i -> i
                    .isTrue(false, "notBusy"))
                    .observe(Level.WARN);
            return "redirect:/insects/" + name + "?error=busy";
        }
        return "redirect:/insects/" + name;
    }

    /**
     * Stops a request whose notes the observation would refuse before it reaches the command: observes
     * the refusal through the endpoint's {@code methodObserver} and answers true, so the caller
     * redirects with {@code ?error=notes}.
     */
    private static boolean notesTooLong(MethodObserver methodObserver, @Nullable String notes) {
        return !methodObserver.arguments(i -> i
                .maxLength(notes, OrganismObservation.MAX_NOTES_LENGTH, "notes"))
                .observe(Level.INFO)
                .violations().isEmpty();
    }

    /**
     * Answers a request for a name the catalog does not hold with the not-found page and a 404, linking back to
     * the list {@code back}. The caller has already observed why (invalid slug or unknown name).
     */
    private static String notFound(HttpServletResponse response, Model model, EntityName name, String back) {
        response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        model.addAttribute("name", name.value());
        model.addAttribute("back", back);
        return "insects/not-found";
    }

    /**
     * Stops a request whose claimed {@code area} is unreadable or off the lattice before the image is
     * read: observes the refusal through the endpoint's {@code methodObserver} and answers true, so the
     * caller redirects with {@code ?error=area}. No claimed cell ({@code null}) is not a refusal.
     */
    private static boolean areaOffTheLattice(MethodObserver methodObserver, @Nullable GridCell area) {
        return !methodObserver.state(i -> i
                .valueObjectOrNull(area, "area"))
                .observe(Level.INFO)
                .violations().isEmpty();
    }

    @PostMapping("/{name}/notes")
    String updateNotes(@PathVariable String name,
                       @RequestParam(name = "observationId", required = false) @Nullable InsectObservationId observationId,
                       @RequestParam("notes") String notes,
                       @RequestParam(value = "returnPath", required = false) String returnPath,
                       HttpServletRequest request) {
        String destination = safeReturnPath(returnPath, "/insects/" + name);
        MethodObserver methodObserver = observer.forMethod("updateNotes");
        if (notesTooLong(methodObserver, notes)) {
            return "redirect:" + destination + (destination.contains("?") ? "&" : "?") + "error=notes";
        }
        if (!methodObserver.arguments(i -> i
                .entityId(observationId, "observationId"))
                .observe(Level.INFO)
                .violations().isEmpty()) {
            // a missing, malformed or non-v7 observationId (Spring hands an unreadable one on as null). It names no
            // observation, so saying so leaks nothing about who owns one (a non-owner's edit stays silent below).
            return "redirect:" + destination + (destination.contains("?") ? "&" : "?") + "error=observation";
        }
        // The sighting port owns the ownership rule: a missing id or a non-owner changes nothing
        // and looks identical to the caller (no 403, so nothing leaks about who owns what).
        currentNaturalist(request).ifPresent(viewer ->
                insectCommand.sightings().updateNotes(observationId, viewer, notes));
        return "redirect:" + destination;
    }

    /**
     * Constrains a caller-supplied redirect target to this console's own insect
     * pages. A form field is untrusted input; without the prefix check it is an
     * open redirect.
     */
    private static String safeReturnPath(@Nullable String candidate, String fallback) {
        if (StringUtils.isBlank(candidate)) {
            return fallback;
        }
        if (!candidate.startsWith("/insects/") || candidate.contains("//")) {
            return fallback;
        }
        // Defence in depth: Tomcat rejects raw control characters (e.g. CR/LF) in
        // header values at the container layer, but this validator shouldn't rely
        // on behaviour it doesn't own.
        for (int i = 0; i < candidate.length(); i++) {
            if (candidate.charAt(i) < 0x20) {
                return fallback;
            }
        }
        return candidate;
    }

    @GetMapping("/{name}/life-stages")
    String lifeStages(@PathVariable InsectSpeciesName name, HttpServletResponse response, Model model) {
        MethodObserver methodObserver = observer.forMethod("lifeStages");
        if (methodObserver.arguments(c -> c
                .entityName(name, "name"))
                .observe(Level.INFO)
                .isNotValid()) {
            return notFound(response, model, name, "/insects/species");
        }
        Optional<Insect> insect = insectQuery.getByName(name);
        return methodObserver.state(c -> c
                .isPresent(insect, "nameFound"))
                .observe(Level.INFO)
                .whenNotValid(() -> notFound(response, model, name, "/insects/species"))
                .otherwise(() -> lifeStagesPage(name, insect.get(), response, model));
    }

    private String lifeStagesPage(InsectSpeciesName name, Insect i, HttpServletResponse response, Model model) {
        if (observer.arguments("lifeStagesPage", c -> c
                .readModel(i, "insect")
                .isTrue(i, Insect::hasSpecies, "hasSpecies"))
                .observe(Level.WARN)
                .isNotValid()) {
            // the catalog holds it, but the read model is invalid: counted to fix, answered as not found
            return notFound(response, model, name, "/insects/species");
        }
        // Insect.invariants() requires the genus, family and order whenever the species is present
        InsectSpeciesInfo s = i.species().info();
        InsectGenusInfo genus = i.genus().info();
        InsectFamilyInfo family = i.family().info();
        InsectOrderInfo order = i.order().info();
        model.addAttribute("species", s);
        model.addAttribute("genus", genus);
        model.addAttribute("family", family);
        model.addAttribute("order", order);
        model.addAttribute("stages", i.lifeStages().stream().toList());
        model.addAttribute("ancestorIntros", introsForLifeStages(order, family, genus, s));
        model.addAttribute("breadcrumb", breadcrumbToSpecies(s, genus, family, order));
        model.addAttribute("cladeTrail", cladeTrail(lineageToSpecies(s, genus, family, order)));
        return "insects/life-stages";
    }

    /**
     * Streams a stored photo from the store to the client, never buffering it whole (PL-16), under
     * the media type the store sniffs. Names are unique, so the response is cacheable forever. When the store's read cap is full
     * ({@link BulkheadFullException}) the answer is an uncached 503 with {@code Retry-After},
     * so the browser retries the one photo rather than the page failing (PL-16).
     */
    @GetMapping("/images/{filename:.+}")
    ResponseEntity<Resource> image(@PathVariable FileName filename) {
        return serve("insects", filename, ImageVariant.PHOTO);
    }

    /**
     * Streams one variant of a stored photo. A missing variant is an uncached 404: a tile never
     * falls back to the full photo, and the 404 must not be remembered once the tile is written.
     */
    private ResponseEntity<Resource> serve(String space, FileName filename, ImageVariant variant) {
        MethodObserver methodObserver = observer.forMethod("serve");
        // A name with a directory part names no stored photo: answered here, before the store refuses it.
        if (!methodObserver.arguments(i -> i
                .namedValue(filename, "filename"))
                .observe(Level.INFO)
                .violations().isEmpty()) {
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
                .observe(Level.INFO)
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
        return serve("insects", filename, ImageVariant.TILE);
    }

    private static final Observer observer = Observer.forClass(InsectsController.class);

}
