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
import com.naturalist.insects.lifestage.InsectLifeStageQuery;
import com.naturalist.insects.lifestage.LifeStage;
import com.naturalist.library.CladeQuery;
import com.naturalist.library.CladeStep;
import com.naturalist.library.CladeView;
import com.naturalist.library.GlossaryLinker;
import jakarta.servlet.http.HttpServletRequest;
import org.jspecify.annotations.Nullable;
import org.springframework.http.CacheControl;
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
import com.naturalist.observation.ImageStore;
import com.naturalist.observation.ImageUpload;
import com.naturalist.observation.OrganismImage;
import com.naturalist.observation.OrganismObservation;

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
    private final InsectCommand insectCommand;
    private final InsectLifeStageQuery insectLifeStageQuery;
    private final CladeQuery cladeQuery;
    private final DescriptionRenderer descriptionRenderer;
    private final ImageStore imageStore;
    private final GlossaryLinker glossaryLinker;

    InsectsController(InsectQuery insectQuery,
                      InsectCommand insectCommand,
                      InsectLifeStageQuery insectLifeStageQuery,
                      CladeQuery cladeQuery,
                      com.naturalist.library.GlossaryTermQuery glossaryTermQuery,
                      ImageStore imageStore) {
        this.insectQuery = insectQuery;
        this.insectCommand = insectCommand;
        this.insectLifeStageQuery = insectLifeStageQuery;
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

    private List<AncestorIntro> introsForFamily(InsectOrder order) {
        return List.of(
                classInsectaIntro(),
                intro("Order " + order.order().value(), "intro-order-open", order.description()));
    }

    private List<AncestorIntro> introsForGenus(InsectOrder order, InsectFamily family) {
        return List.of(
                classInsectaIntro(),
                intro("Order " + order.order().value(), "intro-order-open", order.description()),
                intro("Family " + family.family().value(), "intro-family-open", family.description()));
    }

    private List<AncestorIntro> introsForSpecies(InsectOrder order, InsectFamily family,
                                                 InsectGenus genus) {
        return List.of(
                classInsectaIntro(),
                intro("Order " + order.order().value(), "intro-order-open", order.description()),
                intro("Family " + family.family().value(), "intro-family-open", family.description()),
                intro("Genus " + genus.genus().value(), "intro-genus-open", genus.description()));
    }

    private List<AncestorIntro> introsForLifeStages(InsectOrder order, InsectFamily family,
                                                    InsectGenus genus, InsectSpecies species) {
        var binomial = genus.genus().value() + " " + species.epithet().value();
        return List.of(
                classInsectaIntro(),
                intro("Order " + order.order().value(), "intro-order-open", order.description()),
                intro("Family " + family.family().value(), "intro-family-open", family.description()),
                intro("Genus " + genus.genus().value(), "intro-genus-open", genus.description()),
                intro("Species " + binomial, "intro-species-open", species.description()));
    }

    /**
     * Maps each image to the signed-in naturalist's own {@link OrganismObservation}
     * for it, when one exists. Only the viewer's observations are exposed — the
     * gallery's notes form edits the viewer's own field notes, never someone
     * else's.
     */
    private Map<InsectImageId, OrganismObservation<InsectObservationId, InsectRankName>> observationLookup(
            List<OrganismImage<InsectImageId, InsectObservationId, InsectRankName>> images,
            java.util.Optional<com.naturalist.naturalist.NaturalistName> viewer,
            InsectRankName subject) {
        var lookup = new java.util.HashMap<InsectImageId, OrganismObservation<InsectObservationId, InsectRankName>>();
        if (viewer.isEmpty()) {
            return lookup;
        }
        var mine = insectQuery.observations()
                .forNaturalistAndSubjects(viewer.get(), java.util.Set.of(subject));
        for (var img : images) {
            if (img.observationId() != null) {
                mine.stream()
                        .filter(o -> o.id().equals(img.observationId()))
                        .findFirst()
                        .ifPresent(o -> lookup.put(img.id(), o));
            }
        }
        return lookup;
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

    private List<BreadcrumbSegment> breadcrumbToOrder(InsectOrder order) {
        var segments = new ArrayList<>(cladePrefix());
        segments.add(BreadcrumbSegment.current(order.order().value(), "Order"));
        return segments;
    }

    private List<BreadcrumbSegment> breadcrumbToFamily(InsectFamily family, InsectOrder order) {
        var segments = new ArrayList<>(cladePrefix());
        segments.add(BreadcrumbSegment.link(order.order().value(),
                "/insects/orders/" + order.name().value(), "Order"));
        segments.add(BreadcrumbSegment.current(family.family().value(), "Family"));
        return segments;
    }

    private List<BreadcrumbSegment> breadcrumbToGenus(InsectGenus genus,
                                                      InsectFamily family,
                                                      InsectOrder order) {
        var segments = new ArrayList<>(cladePrefix());
        segments.add(BreadcrumbSegment.link(order.order().value(),
                "/insects/orders/" + order.name().value(), "Order"));
        segments.add(BreadcrumbSegment.link(family.family().value(),
                "/insects/families/" + family.name().value(), "Family"));
        segments.add(BreadcrumbSegment.current(genus.genus().value(), "Genus"));
        return segments;
    }

    private List<BreadcrumbSegment> breadcrumbToSpecies(InsectSpecies species,
                                                        InsectGenus genus,
                                                        InsectFamily family,
                                                        InsectOrder order) {
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
                .sorted(Comparator.comparing((InsectOrder o) -> o.order().value()))
                .map(o -> new CladeTaxonCard("/insects/orders/" + o.name().value(), o.order().value()))
                .toList();
    }

    private List<CladeTaxonCard> familiesPlacedAt(Clade clade) {
        return Pages.stream(1000, insectQuery.families()::findPage)
                .filter(f -> clade.equals(f.placedIn()))
                .sorted(Comparator.comparing((InsectFamily f) -> f.family().value()))
                .map(f -> new CladeTaxonCard("/insects/families/" + f.name().value(), f.family().value()))
                .toList();
    }

    private List<CladeTaxonCard> generaPlacedAt(Clade clade) {
        return Pages.stream(1000, insectQuery.genera()::findPage)
                .filter(g -> clade.equals(g.placedIn()))
                .sorted(Comparator.comparing((InsectGenus g) -> g.genus().value()))
                .map(g -> new CladeTaxonCard("/insects/genera/" + g.name().value(), g.genus().value()))
                .toList();
    }

    private List<CladeTaxonCard> speciesPlacedAt(Clade clade) {
        List<InsectSpecies> placedSpecies = Pages.stream(1000, insectQuery.species()::findPage)
                .filter(s -> clade.equals(s.placedIn()))
                .toList();
        java.util.Set<InsectGenusName> genusNameSet = placedSpecies.stream()
                .map(InsectSpecies::genusName)
                .collect(java.util.stream.Collectors.toSet());
        Map<InsectGenusName, InsectGenus> genusByName = new LinkedHashMap<>();
        insectQuery.genera().findByNameSet(genusNameSet).stream()
                .forEach(genus -> genusByName.put(genus.name(), genus));
        return placedSpecies.stream()
                .map(s -> {
                    InsectGenus genus = genusByName.get(s.genusName());
                    String genusEpithet = genus != null ? genus.genus().value() : "";
                    return new CladeTaxonCard("/insects/" + s.name().value(),
                            (genusEpithet + " " + s.epithet().value()).trim());
                })
                .sorted(Comparator.comparing(CladeTaxonCard::name))
                .toList();
    }

    private List<InsectCladeAnchors.LineageEntry> lineageToOrder(InsectOrder order) {
        return List.of(new InsectCladeAnchors.LineageEntry(
                order.name().value(), order.order().value()));
    }

    private List<InsectCladeAnchors.LineageEntry> lineageToFamily(InsectFamily family, InsectOrder order) {
        return List.of(
                new InsectCladeAnchors.LineageEntry(family.name().value(), family.family().value()),
                new InsectCladeAnchors.LineageEntry(order.name().value(), order.order().value()));
    }

    private List<InsectCladeAnchors.LineageEntry> lineageToGenus(InsectGenus genus,
                                                                 InsectFamily family,
                                                                 InsectOrder order) {
        return List.of(
                new InsectCladeAnchors.LineageEntry(genus.name().value(), genus.genus().value()),
                new InsectCladeAnchors.LineageEntry(family.name().value(), family.family().value()),
                new InsectCladeAnchors.LineageEntry(order.name().value(), order.order().value()));
    }

    private List<InsectCladeAnchors.LineageEntry> lineageToSpecies(InsectSpecies species,
                                                                   InsectGenus genus,
                                                                   InsectFamily family,
                                                                   InsectOrder order) {
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
        if (identified != null && !identified.isBlank()) {
            model.addAttribute("identified", identified);
        }
        if (limit != null && !limit.isBlank()) {
            model.addAttribute("limit", limit);
        }
        if ("image".equals(error)) {
            model.addAttribute("imageError", true);
        }
        return "insects/identify";
    }

    @PostMapping("/identify")
    String identify(@RequestParam("image") MultipartFile imageFile,
                    @RequestParam(name = "location", required = false) String location,
                    @RequestParam(name = "capturedAt", required = false) String capturedAt,
                    @RequestParam(name = "notes", required = false) String notes,
                    HttpServletRequest request) throws IOException {
        var me = currentNaturalist(request);
        if (me.isEmpty()) {
            return "redirect:/insects/identify";
        }

        Instant capturedInstant = null;
        if (capturedAt != null && !capturedAt.isBlank()) {
            try { capturedInstant = Instant.parse(capturedAt); }
            catch (java.time.format.DateTimeParseException ignored) { }
        }

        ImageUpload upload = ImageUpload.of(imageFile.getBytes());
        if (!upload.isValid()) {
            // The upload is not a JPEG/PNG/WebP (or is oversize) -- nothing is stored.
            return "redirect:/insects/identify?error=image";
        }

        try {
            var rankName = insectCommand.identification().identify(
                    upload,
                    new com.naturalist.vision.ImageMetadata(location, capturedInstant),
                    me.get(), notes);
            return "redirect:/insects/" + rankName.value();
        } catch (com.naturalist.identification.IdentificationRefused refused) {
            // The rate gate and the budget both live behind insectCommand.identification()
            // (InsectIdentificationCommandImpl), ahead of the store and any vision call.
            // "RATE" is a UI-only query-param string, not a usage.LimitKind; a budget refusal's
            // detail is the exhausted LimitKind's name (DAILY / MONTHLY ...).
            return switch (refused.reason()) {
                case RATE_LIMITED -> "redirect:/insects/identify?limit=RATE";
                case BUDGET_EXHAUSTED -> "redirect:/insects/identify?limit=" + refused.detail();
            };
        }
    }

    @GetMapping("/species")
    String list(@RequestParam(defaultValue = "0") int page,
                HttpServletRequest request, Model model) {
        boolean mine = collectionLensOn(request);
        Page<InsectSpecies> speciesPage = insectQuery.species().findPage(PageRequest.console(Math.max(0, page)));
        List<InsectSpecies> speciesInPage = speciesPage.content();

        java.util.Set<InsectRankName> speciesNameSet = speciesInPage.stream()
                .<InsectRankName>map(InsectSpecies::name)
                .collect(java.util.stream.Collectors.toSet());
        InsectEntityCollections.ImageGallery gallery = InsectEntityCollections.ImageGallery.of(
                insectQuery.images().forParentNames(speciesNameSet).stream().toList());

        Map<InsectSpeciesName, InsectFunctionalRole> rolesBySpecies = new LinkedHashMap<>();
        insectQuery.functionalRoles().getByParentNames(speciesNameSet).stream()
                .forEach(role -> {
                    if (role.parentName() instanceof InsectSpeciesName parentSpeciesName) {
                        rolesBySpecies.put(parentSpeciesName, role);
                    }
                });

        java.util.Set<InsectGenusName> genusNameSet = speciesInPage.stream()
                .map(InsectSpecies::genusName)
                .collect(java.util.stream.Collectors.toSet());
        Map<InsectGenusName, InsectGenus> genusByName = new LinkedHashMap<>();
        insectQuery.genera().findByNameSet(genusNameSet).stream()
                .forEach(genus -> genusByName.put(genus.name(), genus));

        java.util.Set<InsectFamilyName> familyNameSet = genusByName.values().stream()
                .map(InsectGenus::familyName)
                .collect(java.util.stream.Collectors.toSet());
        Map<InsectFamilyName, InsectFamily> familyByName = new LinkedHashMap<>();
        insectQuery.families().findByNameSet(familyNameSet).stream()
                .forEach(family -> familyByName.put(family.name(), family));

        java.util.Optional<com.naturalist.naturalist.NaturalistName> me = currentNaturalist(request);
        List<InsectSpecies> speciesList;
        if (mine && me.isPresent()) {
            java.util.Set<InsectRankName> mySubjects = insectQuery.observations()
                    .forNaturalist(me.get()).stream()
                    .map(OrganismObservation<InsectObservationId, InsectRankName>::subject)
                    .collect(java.util.stream.Collectors.toSet());
            speciesList = speciesPage.content().stream()
                    .filter(s -> mySubjects.contains(s.name()))
                    .toList();
        } else {
            speciesList = speciesPage.content();
        }
        model.addAttribute("mine", mine && me.isPresent());

        model.addAttribute("speciesPage", speciesPage);
        model.addAttribute("speciesList", speciesList);
        model.addAttribute("gallery", gallery);
        model.addAttribute("rolesBySpecies", rolesBySpecies);
        model.addAttribute("familyByName", familyByName);
        model.addAttribute("genusByName", genusByName);
        model.addAttribute("ancestorIntros", classOnlyIntros());
        model.addAttribute("breadcrumb", cladePrefix());
        model.addAttribute("cladeTrail", cladeTrail(List.of()));
        return "insects/list";
    }

    @GetMapping("/families")
    String families(@RequestParam(defaultValue = "0") int page, Model model) {
        Page<InsectFamily> familyPage = insectQuery.families()
                .findPage(PageRequest.console(Math.max(0, page)));
        java.util.Set<InsectOrderName> orderNameSet = familyPage.content().stream()
                .map(InsectFamily::orderName)
                .collect(java.util.stream.Collectors.toSet());
        Map<InsectOrderName, InsectOrder> orderByName = new LinkedHashMap<>();
        insectQuery.orders().findByNameSet(orderNameSet).stream()
                .forEach(order -> orderByName.put(order.name(), order));
        java.util.LinkedHashSet<InsectRankName> familyRoots = familyPage.content().stream()
                .map(family -> (InsectRankName) family.name())
                .collect(java.util.stream.Collectors.toCollection(java.util.LinkedHashSet::new));
        InsectEntityCollections.ImageGallery gallery = insectQuery.images().forRankHierarchies(familyRoots);
        model.addAttribute("familyPage", familyPage);
        model.addAttribute("orderByName", orderByName);
        model.addAttribute("gallery", gallery);
        model.addAttribute("ancestorIntros", classOnlyIntros());
        model.addAttribute("breadcrumb", cladePrefix());
        model.addAttribute("cladeTrail", cladeTrail(List.of()));
        return "insects/families";
    }

    @GetMapping("/families/{name}")
    String familyDetail(@PathVariable String name, HttpServletRequest request, Model model) {
        var familyName = InsectFamilyName.of(name);
        var insect = insectQuery.getByName(familyName);
        if (insect.isEmpty()) {
            return "redirect:/insects/families";
        }
        InsectFamily family = insect.get().family().family();
        InsectOrder order = insect.get().order().order();
        var description = family.description();
        model.addAttribute("family", family);
        model.addAttribute("order", order);
        model.addAttribute("children", insect.get().children());
        model.addAttribute("citations", insect.get().citations());
        model.addAttribute("features", insect.get().features());
        model.addAttribute("glossaryLinker", glossaryLinker);
        model.addAttribute("descriptionPreschool", descriptionRenderer.render(description.preschool()));
        model.addAttribute("descriptionElementary", descriptionRenderer.render(description.elementary()));
        model.addAttribute("descriptionSecondary", descriptionRenderer.render(description.secondary()));
        model.addAttribute("descriptionUniversity", descriptionRenderer.render(description.university()));
        model.addAttribute("breadcrumb", breadcrumbToFamily(family, order));
        model.addAttribute("cladeTrail", cladeTrail(lineageToFamily(family, order)));
        model.addAttribute("ancestorIntros", introsForFamily(order));
        List<OrganismImage<InsectImageId, InsectObservationId, InsectRankName>> rankImages = insectQuery.images()
                .forParentName(familyName).stream().toList();
        java.util.Optional<com.naturalist.naturalist.NaturalistName> viewer =
                currentNaturalist(request);
        model.addAttribute("images", rankImages);
        model.addAttribute("observations",
                observationLookup(rankImages, viewer, familyName));
        return "insects/family";
    }

    @GetMapping("/orders")
    String orders(@RequestParam(defaultValue = "0") int page, Model model) {
        Page<InsectOrder> orderPage = insectQuery.orders()
                .findPage(PageRequest.console(Math.max(0, page)));
        java.util.LinkedHashSet<InsectRankName> orderRoots = orderPage.content().stream()
                .map(order -> (InsectRankName) order.name())
                .collect(java.util.stream.Collectors.toCollection(java.util.LinkedHashSet::new));
        InsectEntityCollections.ImageGallery gallery = insectQuery.images().forRankHierarchies(orderRoots);
        model.addAttribute("orderPage", orderPage);
        model.addAttribute("gallery", gallery);
        model.addAttribute("ancestorIntros", classOnlyIntros());
        model.addAttribute("breadcrumb", cladePrefix());
        model.addAttribute("cladeTrail", cladeTrail(List.of()));
        return "insects/orders";
    }

    @GetMapping("/orders/{name}")
    String orderDetail(@PathVariable String name, HttpServletRequest request, Model model) {
        var orderName = InsectOrderName.of(name);
        var insect = insectQuery.getByName(orderName);
        if (insect.isEmpty()) {
            return "redirect:/insects/orders";
        }
        InsectOrder order = insect.get().order().order();
        var description = order.description();
        model.addAttribute("order", order);
        model.addAttribute("children", insect.get().children());
        model.addAttribute("citations", insect.get().citations());
        model.addAttribute("features", insect.get().features());
        model.addAttribute("glossaryLinker", glossaryLinker);
        model.addAttribute("descriptionPreschool", descriptionRenderer.render(description.preschool()));
        model.addAttribute("descriptionElementary", descriptionRenderer.render(description.elementary()));
        model.addAttribute("descriptionSecondary", descriptionRenderer.render(description.secondary()));
        model.addAttribute("descriptionUniversity", descriptionRenderer.render(description.university()));
        model.addAttribute("breadcrumb", breadcrumbToOrder(order));
        model.addAttribute("cladeTrail", cladeTrail(lineageToOrder(order)));
        model.addAttribute("ancestorIntros", classOnlyIntros());
        List<OrganismImage<InsectImageId, InsectObservationId, InsectRankName>> rankImages = insectQuery.images()
                .forParentName(orderName).stream().toList();
        java.util.Optional<com.naturalist.naturalist.NaturalistName> viewer =
                currentNaturalist(request);
        model.addAttribute("images", rankImages);
        model.addAttribute("observations",
                observationLookup(rankImages, viewer, orderName));
        return "insects/order";
    }

    @GetMapping("/genera")
    String genera(@RequestParam(defaultValue = "0") int page, Model model) {
        Page<InsectGenus> genusPage = insectQuery.genera()
                .findPage(PageRequest.console(Math.max(0, page)));
        java.util.Set<InsectFamilyName> familyNameSet = genusPage.content().stream()
                .map(InsectGenus::familyName)
                .collect(java.util.stream.Collectors.toSet());
        Map<InsectFamilyName, InsectFamily> familyByName = new LinkedHashMap<>();
        insectQuery.families().findByNameSet(familyNameSet).stream()
                .forEach(family -> familyByName.put(family.name(), family));
        java.util.LinkedHashSet<InsectRankName> genusRoots = genusPage.content().stream()
                .map(genus -> (InsectRankName) genus.name())
                .collect(java.util.stream.Collectors.toCollection(java.util.LinkedHashSet::new));
        InsectEntityCollections.ImageGallery gallery = insectQuery.images().forRankHierarchies(genusRoots);
        model.addAttribute("genusPage", genusPage);
        model.addAttribute("familyByName", familyByName);
        model.addAttribute("gallery", gallery);
        model.addAttribute("ancestorIntros", classOnlyIntros());
        model.addAttribute("breadcrumb", cladePrefix());
        model.addAttribute("cladeTrail", cladeTrail(List.of()));
        return "insects/genera";
    }

    @GetMapping("/genera/{name}")
    String genusDetail(@PathVariable String name, HttpServletRequest request, Model model) {
        var genusName = InsectGenusName.of(name);
        var insect = insectQuery.getByName(genusName);
        if (insect.isEmpty()) {
            return "redirect:/insects/genera";
        }
        InsectGenus genus = insect.get().genus().genus();
        InsectFamily family = insect.get().family().family();
        InsectOrder order = insect.get().order().order();
        var description = genus.description();
        model.addAttribute("genus", genus);
        model.addAttribute("family", family);
        model.addAttribute("order", order);
        model.addAttribute("children", insect.get().children());
        model.addAttribute("citations", insect.get().citations());
        model.addAttribute("features", insect.get().features());
        model.addAttribute("glossaryLinker", glossaryLinker);
        model.addAttribute("descriptionPreschool", descriptionRenderer.render(description.preschool()));
        model.addAttribute("descriptionElementary", descriptionRenderer.render(description.elementary()));
        model.addAttribute("descriptionSecondary", descriptionRenderer.render(description.secondary()));
        model.addAttribute("descriptionUniversity", descriptionRenderer.render(description.university()));
        model.addAttribute("breadcrumb", breadcrumbToGenus(genus, family, order));
        model.addAttribute("cladeTrail", cladeTrail(lineageToGenus(genus, family, order)));
        model.addAttribute("ancestorIntros", introsForGenus(order, family));
        List<OrganismImage<InsectImageId, InsectObservationId, InsectRankName>> rankImages = insectQuery.images()
                .forParentName(genusName).stream().toList();
        java.util.Optional<com.naturalist.naturalist.NaturalistName> viewer =
                currentNaturalist(request);
        model.addAttribute("images", rankImages);
        model.addAttribute("observations",
                observationLookup(rankImages, viewer, genusName));
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
        var roles = insectQuery.functionalRoles().getByGuild(selected).stream()
                .sorted(Comparator.comparing(r -> r.parentName().value()))
                .toList();
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
        var speciesName = InsectSpeciesName.of(name);
        var insect = insectQuery.getByName(speciesName);
        if (insect.isEmpty()) {
            return "redirect:/insects";
        }
        Insect i = insect.get();
        InsectSpecies s = i.species().species();
        InsectGenus genus = i.genus().genus();
        InsectFamily family = i.family().family();
        InsectOrder order = i.order().order();
        var description = s.description();
        if ("image".equals(error)) {
            model.addAttribute("imageError", true);
        }
        model.addAttribute("species", s);
        model.addAttribute("genus", genus);
        model.addAttribute("family", family);
        model.addAttribute("order", order);
        model.addAttribute("stages", i.lifeStages().stream()
                .sorted(Comparator.comparingInt(stage -> stage.kind().ordinal()))
                .toList());
        boolean lens = collectionLensOn(request);
        java.util.Optional<com.naturalist.naturalist.NaturalistName> viewer = currentNaturalist(request);
        List<OrganismImage<InsectImageId, InsectObservationId, InsectRankName>> galleryImages = i.observations().stream().toList();
        if (lens && viewer.isPresent()) {
            java.util.Set<InsectObservationId> myObservationIds = insectQuery.observations()
                    .forNaturalist(viewer.get()).stream()
                    .map(OrganismObservation<InsectObservationId, InsectRankName>::id)
                    .collect(java.util.stream.Collectors.toSet());
            galleryImages = galleryImages.stream()
                    .filter(img -> img.observationId() != null && myObservationIds.contains(img.observationId()))
                    .toList();
        }
        model.addAttribute("images", galleryImages);
        model.addAttribute("lens", lens && viewer.isPresent());

        var myObservations = viewer.isPresent()
                ? insectQuery.observations()
                        .forNaturalistAndSubjects(viewer.get(), java.util.Set.<InsectRankName>of(speciesName))
                : null;
        model.addAttribute("observations",
                observationLookup(galleryImages, viewer, speciesName));

        model.addAttribute("citations", i.citations());
        model.addAttribute("features", i.features());
        model.addAttribute("glossaryLinker", glossaryLinker);
        model.addAttribute("role", i.role());
        boolean collected = myObservations != null && !myObservations.isEmpty();
        model.addAttribute("collected", collected);
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
                    HttpServletRequest request) throws IOException {
        var species = InsectSpeciesName.of(name);
        var me = currentNaturalist(request);
        ImageUpload upload = ImageUpload.of(imageFile.getBytes());
        if (!upload.isValid()) {
            return "redirect:/insects/" + name + "?error=image";
        }
        insectCommand.photos().add(species, upload, me.orElse(null), notes, location);
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
        if (candidate == null || candidate.isBlank()) {
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
        var speciesName = InsectSpeciesName.of(name);
        var species = insectQuery.species().getByName(speciesName);
        if (species.isEmpty()) {
            return "redirect:/insects";
        }
        InsectSpecies s = species.get();
        InsectGenus genus = insectQuery.genera().getByName(s.genusName()).orElseThrow();
        InsectFamily family = insectQuery.families().getByName(genus.familyName()).orElseThrow();
        InsectOrder order = insectQuery.orders().getByName(family.orderName()).orElseThrow();
        var stages = insectLifeStageQuery.lifeStages().forParentName(speciesName).stream()
                .sorted(Comparator.comparingInt(stage -> stage.kind().ordinal()))
                .toList();
        model.addAttribute("species", s);
        model.addAttribute("family", family);
        model.addAttribute("order", order);
        model.addAttribute("stages", stages);
        model.addAttribute("ancestorIntros", introsForLifeStages(order, family, genus, s));
        model.addAttribute("breadcrumb", breadcrumbToSpecies(s, genus, family, order));
        model.addAttribute("cladeTrail", cladeTrail(lineageToSpecies(s, genus, family, order)));
        return "insects/life-stages";
    }

    /**
     * Serves a stored photo — an upload or a seed catalog image, the store resolves both —
     * under the media type the store derives from its file name. Upload names are unique,
     * so the response is cacheable forever.
     */
    @GetMapping("/images/{filename:.+}")
    ResponseEntity<byte[]> image(@PathVariable String filename) {
        return imageStore.read("insects", FileName.of(filename))
                .map(stored -> ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(stored.mediaType()))
                        .cacheControl(CacheControl.maxAge(365, TimeUnit.DAYS).cachePublic().immutable())
                        .body(stored.bytes()))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

}
