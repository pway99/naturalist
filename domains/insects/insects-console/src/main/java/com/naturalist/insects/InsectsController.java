package com.naturalist.insects;

import com.naturalist.clades.Clade;
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
import java.util.concurrent.TimeUnit;
import com.naturalist.observation.GridCell;
import com.naturalist.observation.ImageStore;
import com.naturalist.observation.ImageUpload;

@Controller
@RequestMapping("/insects")
public class InsectsController {
    private static final String CSRF_REQUEST_ATTRIBUTE = "org.springframework.security.web.csrf.CsrfToken";

    // Written by the app's NaturalistHeaderInterceptor (same literal, by convention).
    private static final String CURRENT_NATURALIST_ATTRIBUTE = "naturalist.currentNaturalistName";

    private static java.util.Optional<com.naturalist.naturalist.NaturalistName> currentNaturalist(
            HttpServletRequest request) {
        Object slug = request.getAttribute(CURRENT_NATURALIST_ATTRIBUTE);
        return slug instanceof String s && !s.isBlank()
                ? java.util.Optional.of(com.naturalist.naturalist.NaturalistName.of(s))
                : java.util.Optional.empty();
    }

    // Session flag toggled by POST /insects/collection-lens; read on every insects browse request.
    private static final String COLLECTION_LENS_ATTRIBUTE = "insects.collectionLens";

    private static boolean collectionLensOn(HttpServletRequest request) {
        var session = request.getSession(false);
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
                                com.naturalist.fieldnotes.Description d) {
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
        var binomial = genus.genus().value() + " " + species.epithet().value();
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
        var segments = new ArrayList<>(cladePrefix());
        segments.add(BreadcrumbSegment.current(order.order().value(), "Order"));
        return segments;
    }

    private List<BreadcrumbSegment> breadcrumbToFamily(InsectFamilyInfo family, InsectOrderInfo order) {
        var segments = new ArrayList<>(cladePrefix());
        segments.add(BreadcrumbSegment.link(order.order().value(),
                "/insects/orders/" + order.name().value(), "Order"));
        segments.add(BreadcrumbSegment.current(family.family().value(), "Family"));
        return segments;
    }

    private List<BreadcrumbSegment> breadcrumbToGenus(InsectGenusInfo genus,
                                                      InsectFamilyInfo family,
                                                      InsectOrderInfo order) {
        var segments = new ArrayList<>(cladePrefix());
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
        var segments = new ArrayList<>(cladePrefix());
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
        InsectCladeAnchors.Anchor anchor = InsectCladeAnchors.resolve(lineage);
        CladeView view = cladeQuery.getBySlug(anchor.cladeSlug()).orElseThrow(
                () -> new IllegalStateException("anchor clade not found: " + anchor.cladeSlug()));
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
        Clade clade;
        try {
            clade = Clade.of(slug);
        } catch (IllegalArgumentException notAClade) {
            return "redirect:/insects/orders";
        }
        if (CladeTraversal.ancestry(clade).stream().anyMatch(node -> node instanceof Plantae)) {
            return "redirect:/plants/clades/" + slug;
        }
        var description = clade.description();
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
        CladeView view = cladeQuery.getBySlug(slug).orElseThrow(
                () -> new IllegalStateException("clade not found: " + slug));
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
        var me = currentNaturalist(request);
        if (me.isEmpty()) {
            return "redirect:/insects/identify";
        }

        Instant capturedInstant = null;
        if (StringUtils.isNotBlank(capturedAt)) {
            try { capturedInstant = Instant.parse(capturedAt); }
            catch (java.time.format.DateTimeParseException ignored) { }
        }

        ImageUpload upload = ImageUpload.of(imageFile.getBytes());
        if (!upload.isValid()) {
            // The upload is not a JPEG/PNG/WebP (or is oversize) -- nothing is stored.
            return "redirect:/insects/identify?error=image";
        }

        GridCell area = GridCell.of(areaLat, areaLon);
        if (area != null && !area.isValid()) {
            // A claimed cell that is unreadable or off the lattice -- nothing is stored.
            return "redirect:/insects/identify?error=area";
        }

        try {
            var rankName = insectCommand.identification().identify(
                    upload,
                    new com.naturalist.vision.ImageMetadata(location, capturedInstant),
                    me.get(), notes, area);
            return "redirect:/insects/" + rankName.value();
        } catch (com.naturalist.identification.IdentificationRefused refused) {
            // The rate gate and the budget both live behind insectCommand.identification()
            // (InsectIdentificationCommandImpl), ahead of the store and any vision call.
            // "RATE" is a UI-only query-param string, not a usage.LimitKind; a budget refusal's
            // detail is the exhausted LimitKind's name (DAILY / MONTHLY ...).
            return switch (refused.reason()) {
                case RATE_LIMITED -> "redirect:/insects/identify?limit=RATE";
                case BUDGET_EXHAUSTED -> "redirect:/insects/identify?limit=" + refused.detail();
                case BUSY -> "redirect:/insects/identify?limit=BUSY";
            };
        }
    }

    @GetMapping("/species")
    String list(@RequestParam(defaultValue = "0") int page,
                HttpServletRequest request, Model model) {
        java.util.Optional<com.naturalist.naturalist.NaturalistName> viewer = currentNaturalist(request);
        boolean mine = collectionLensOn(request) && viewer.isPresent();
        PageRequest pageRequest = PageRequest.console(Math.max(0, page));
        Page<InsectViews.SpeciesCard> cards = mine
                ? insectViewQuery.collectedBy(viewer.get(), pageRequest)
                : insectViewQuery.speciesCards(pageRequest, viewer.orElse(null));
        model.addAttribute("cards", cards);
        model.addAttribute("mine", mine);
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
    String familyDetail(@PathVariable String name, HttpServletRequest request, Model model) {
        var rankPage = insectViewQuery.rankPage(InsectFamilyName.of(name), currentNaturalist(request).orElse(null));
        if (rankPage.isEmpty()) {
            return "redirect:/insects/families";
        }
        Insect insect = rankPage.get().insect();
        InsectFamilyInfo family = insect.family().info();
        InsectOrderInfo order = insect.order().info();
        var description = family.description();
        model.addAttribute("page", rankPage.get());
        model.addAttribute("glossaryLinker", glossaryLinker);
        model.addAttribute("descriptionPreschool", descriptionRenderer.render(description.preschool()));
        model.addAttribute("descriptionElementary", descriptionRenderer.render(description.elementary()));
        model.addAttribute("descriptionSecondary", descriptionRenderer.render(description.secondary()));
        model.addAttribute("descriptionUniversity", descriptionRenderer.render(description.university()));
        model.addAttribute("breadcrumb", breadcrumbToFamily(family, order));
        model.addAttribute("cladeTrail", cladeTrail(lineageToFamily(family, order)));
        model.addAttribute("ancestorIntros", introsForFamily(order));
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
    String orderDetail(@PathVariable String name, HttpServletRequest request, Model model) {
        var rankPage = insectViewQuery.rankPage(InsectOrderName.of(name), currentNaturalist(request).orElse(null));
        if (rankPage.isEmpty()) {
            return "redirect:/insects/orders";
        }
        InsectOrderInfo order = rankPage.get().insect().order().info();
        var description = order.description();
        model.addAttribute("page", rankPage.get());
        model.addAttribute("glossaryLinker", glossaryLinker);
        model.addAttribute("descriptionPreschool", descriptionRenderer.render(description.preschool()));
        model.addAttribute("descriptionElementary", descriptionRenderer.render(description.elementary()));
        model.addAttribute("descriptionSecondary", descriptionRenderer.render(description.secondary()));
        model.addAttribute("descriptionUniversity", descriptionRenderer.render(description.university()));
        model.addAttribute("breadcrumb", breadcrumbToOrder(order));
        model.addAttribute("cladeTrail", cladeTrail(lineageToOrder(order)));
        model.addAttribute("ancestorIntros", classOnlyIntros());
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
    String genusDetail(@PathVariable String name, HttpServletRequest request, Model model) {
        var rankPage = insectViewQuery.rankPage(InsectGenusName.of(name), currentNaturalist(request).orElse(null));
        if (rankPage.isEmpty()) {
            return "redirect:/insects/genera";
        }
        Insect insect = rankPage.get().insect();
        InsectGenusInfo genus = insect.genus().info();
        InsectFamilyInfo family = insect.family().info();
        InsectOrderInfo order = insect.order().info();
        var description = genus.description();
        model.addAttribute("page", rankPage.get());
        model.addAttribute("glossaryLinker", glossaryLinker);
        model.addAttribute("descriptionPreschool", descriptionRenderer.render(description.preschool()));
        model.addAttribute("descriptionElementary", descriptionRenderer.render(description.elementary()));
        model.addAttribute("descriptionSecondary", descriptionRenderer.render(description.secondary()));
        model.addAttribute("descriptionUniversity", descriptionRenderer.render(description.university()));
        model.addAttribute("breadcrumb", breadcrumbToGenus(genus, family, order));
        model.addAttribute("cladeTrail", cladeTrail(lineageToGenus(genus, family, order)));
        model.addAttribute("ancestorIntros", introsForGenus(order, family));
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
        var roles = insectQuery.functionalRoles().getByGuild(selected).sortedByParentName().stream().toList();
        model.addAttribute("selectedGuild", selected);
        model.addAttribute("guilds", FunctionalGuild.values());
        model.addAttribute("roles", roles);
        model.addAttribute("ancestorIntros", classOnlyIntros());
        model.addAttribute("breadcrumb", cladePrefix());
        model.addAttribute("cladeTrail", cladeTrail(List.of()));
        return "insects/guild";
    }

    @GetMapping("/{name}")
    String detail(@PathVariable String name,
                  @RequestParam(name = "error", required = false) String error,
                  HttpServletRequest request, Model model) {
        java.util.Optional<com.naturalist.naturalist.NaturalistName> viewer = currentNaturalist(request);
        var rankPage = insectViewQuery.rankPage(InsectSpeciesName.of(name), viewer.orElse(null));
        if (rankPage.isEmpty()) {
            return "redirect:/insects";
        }
        Insect i = rankPage.get().insect();
        InsectSpeciesInfo s = i.species().info();
        InsectGenusInfo genus = i.genus().info();
        InsectFamilyInfo family = i.family().info();
        InsectOrderInfo order = i.order().info();
        var description = s.description();
        if ("image".equals(error)) {
            model.addAttribute("imageError", true);
        }
        if ("area".equals(error)) {
            model.addAttribute("areaError", true);
        }
        model.addAttribute("page", rankPage.get());
        model.addAttribute("lens", collectionLensOn(request) && viewer.isPresent());
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
    String observe(@PathVariable String name,
                   @RequestParam(name = "notes", required = false) String notes,
                   HttpServletRequest request) {
        java.util.Optional<com.naturalist.naturalist.NaturalistName> me = currentNaturalist(request);
        if (me.isEmpty()) {
            return "redirect:/insects/" + name;
        }
        insectCommand.sightings().record(InsectSpeciesName.of(name), me.get(), notes);
        return "redirect:/insects/" + name;
    }

    @PostMapping("/{name}/images")
    String addImage(@PathVariable String name,
                    @RequestParam("image") MultipartFile imageFile,
                    @RequestParam(name = "location", required = false) String location,
                    @RequestParam(name = "notes", required = false) String notes,
                    @RequestParam(name = "areaLat", required = false) String areaLat,
                    @RequestParam(name = "areaLon", required = false) String areaLon,
                    HttpServletRequest request) throws IOException {
        var species = InsectSpeciesName.of(name);
        var me = currentNaturalist(request);
        ImageUpload upload = ImageUpload.of(imageFile.getBytes());
        if (!upload.isValid()) {
            return "redirect:/insects/" + name + "?error=image";
        }
        GridCell area = GridCell.of(areaLat, areaLon);
        if (area != null && !area.isValid()) {
            // A claimed cell that is unreadable or off the lattice -- nothing is stored.
            return "redirect:/insects/" + name + "?error=area";
        }
        insectCommand.photos().add(species, upload, me.orElse(null), notes, location, area);
        return "redirect:/insects/" + name;
    }

    @PostMapping("/{name}/notes")
    String updateNotes(@PathVariable String name,
                       @RequestParam("observationId") String observationId,
                       @RequestParam("notes") String notes,
                       @RequestParam(value = "returnPath", required = false) String returnPath,
                       HttpServletRequest request) {
        var destination = safeReturnPath(returnPath, "/insects/" + name);
        InsectObservationId obsId;
        try {
            obsId = InsectObservationId.of(java.util.UUID.fromString(observationId));
        } catch (IllegalArgumentException e) {
            // Malformed observationId -- same outcome as not-found, not a 500.
            return "redirect:" + destination;
        }
        // The sighting port owns the ownership rule: a missing id or a non-owner changes nothing
        // and looks identical to the caller (no 403, so nothing leaks about who owns what).
        currentNaturalist(request).ifPresent(viewer ->
                insectCommand.sightings().updateNotes(obsId, viewer, notes));
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
    String lifeStages(@PathVariable String name, Model model) {
        var insect = insectQuery.getByName(InsectSpeciesName.of(name));
        if (insect.isEmpty()) {
            return "redirect:/insects";
        }
        Insect i = insect.get();
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
    ResponseEntity<Resource> image(@PathVariable String filename) {
        try {
            return imageStore.read("insects", FileName.of(filename))
                    .map(stored -> ResponseEntity.ok()
                            .contentType(MediaType.parseMediaType(stored.mediaType()))
                            .contentLength(stored.size())
                            .cacheControl(CacheControl.maxAge(365, TimeUnit.DAYS).cachePublic().immutable())
                            .<Resource>body(new InputStreamResource(stored.content()::open)))
                    .orElseGet(() -> ResponseEntity.notFound().build());
        } catch (BulkheadFullException busy) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .header(HttpHeaders.RETRY_AFTER, "1")
                    .cacheControl(CacheControl.noStore())
                    .build();
        }
    }

}
