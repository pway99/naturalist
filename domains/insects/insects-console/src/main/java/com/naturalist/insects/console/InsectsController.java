package com.naturalist.insects.console;

import com.naturalist.clades.CladeTraversal;
import com.naturalist.clades.Eukaryota;
import com.naturalist.clades.Insecta;
import com.naturalist.data.FileName;
import com.naturalist.data.NaturalistDatabase;
import com.naturalist.data.Page;
import com.naturalist.data.PageRequest;
import com.naturalist.fieldnotes.render.DescriptionRenderer;
import com.naturalist.insects.*;
import com.naturalist.insects.console.render.InsectsParagraphCues;
import com.naturalist.insects.lifestage.InsectLifeStageQuery;
import com.naturalist.insects.lifestage.LifeStage;
import com.naturalist.authority.eol.EolClientMock;
import com.naturalist.library.CladeQuery;
import com.naturalist.library.CladeStep;
import com.naturalist.library.CladeView;
import com.naturalist.library.LibraryTestContext;
import com.naturalist.resilience.Resilience;
import com.naturalist.textgeneration.NoOpTextGenerationService;
import com.naturalist.resilience.Resilient;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.jspecify.annotations.Nullable;
import org.springframework.core.io.ClassPathResource;
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
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Controller
@RequestMapping("/insects")
public class InsectsController {
    private static final String IMAGE_CONVERSION = "image.conversion";

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
    private final Resilience resilience;
    private final DescriptionRenderer descriptionRenderer;
    private final ImageStorageService imageStorageService;
    private final InsectIdentificationCommand identificationCommand;
    private final InsectAddPhotoCommand addPhotoCommand;
    private final Map<String, byte[]> jpegCache = new ConcurrentHashMap<>();

    InsectsController(Resilience resilience, com.naturalist.vision.VisionService visionService) {
        //TODO:: This will eventually be a spring managed bean
        NaturalistDatabase db = NaturalistDatabase.create();
        InsectsTestContext context = InsectsTestContext.create(db);
        LibraryTestContext libraryContext = LibraryTestContext.create(db);
        this.insectQuery = context.insectQuery();
        this.insectCommand = context.insectCommand();
        this.insectLifeStageQuery = context.insectLifeStageQuery();
        this.resilience = resilience;
        this.descriptionRenderer = new DescriptionRenderer(InsectsParagraphCues.CUES);
        this.imageStorageService = new ImageStorageService(Path.of("data/images/insects"));
        // TODO:: This will eventually be a spring managed bean
        this.cladeQuery = libraryContext.cladeQuery();
        this.identificationCommand = new InsectIdentificationCommand(
                visionService,
                new NoOpTextGenerationService(),
                new EolClientMock(db),
                libraryContext.libraryCommand(),
                context.insectQuery(),
                context.catalogIdentificationTransaction());
        this.addPhotoCommand = new InsectAddPhotoCommand(context.addPhotoTransaction());
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

    private List<FeatureGroup> featureGroups(InsectRankName rankName) {
        return FeatureGroup.of(insectQuery.features().findByRankName(rankName).orElse(null));
    }

    /**
     * Maps each image to the signed-in naturalist's own {@link FieldObservation}
     * for it, when one exists. Only the viewer's observations are exposed — the
     * gallery's notes form edits the viewer's own field notes, never someone
     * else's.
     */
    private Map<InsectImageId, FieldObservation> observationLookup(
            List<InsectImage> images,
            java.util.Optional<com.naturalist.naturalist.NaturalistName> viewer,
            InsectRankName subject) {
        var lookup = new java.util.HashMap<InsectImageId, FieldObservation>();
        if (viewer.isEmpty()) {
            return lookup;
        }
        var mine = insectQuery.fieldObservations()
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
     * clade falls back to its tree-of-life page at {@code /clades/{slug}}.
     */
    private static final Map<String, String> CLADE_URL = Map.of(
            "insecta", "/insects/orders");

    private List<BreadcrumbSegment> cladePrefix() {
        return CladeTraversal.ancestry(new Insecta()).reversed().stream()
                .filter(c -> !(c instanceof Eukaryota))
                .map(c -> {
                    String rank = CLADE_RANK_LABEL.get(c.slug());
                    String url = CLADE_URL.getOrDefault(c.slug(), "/clades/" + c.slug());
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
                        HttpServletRequest request, Model model) {
        Object csrf = request.getAttribute(CSRF_REQUEST_ATTRIBUTE);
        if (csrf != null) {
            model.addAttribute("_csrf", csrf);
        }
        if (identified != null && !identified.isBlank()) {
            model.addAttribute("identified", identified);
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

        var imageBytes = imageFile.getBytes();
        var storedFileName = imageStorageService.store(imageBytes);

        Instant capturedInstant = null;
        if (capturedAt != null && !capturedAt.isBlank()) {
            try { capturedInstant = Instant.parse(capturedAt); }
            catch (java.time.format.DateTimeParseException ignored) { }
        }
        var image = new com.naturalist.vision.Image(
                imageBytes, "image/jpeg",
                new com.naturalist.vision.ImageMetadata(location, capturedInstant));

        var rankName = identificationCommand.identify(
                image, storedFileName, me.get(), notes);

        return "redirect:/insects/" + rankName.value();
    }

    @GetMapping("/species")
    String list(@RequestParam(defaultValue = "0") int page,
                HttpServletRequest request, Model model) {
        boolean mine = collectionLensOn(request);
        Page<InsectSpecies> speciesPage = insectQuery.species().findPage(PageRequest.console(Math.max(0, page)));
        List<InsectImage> allImages = new ArrayList<>();
        Map<InsectSpeciesName, InsectFunctionalRole> rolesBySpecies = new LinkedHashMap<>();
        Map<InsectFamilyName, InsectFamily> familyByName = new LinkedHashMap<>();
        Map<InsectGenusName, InsectGenus> genusByName = new LinkedHashMap<>();
        for (var species : speciesPage.content()) {
            allImages.addAll(insectQuery.images().forParentName(species.name()).stream().toList());
            insectQuery.functionalRoles().getByParentName(species.name())
                    .ifPresent(role -> rolesBySpecies.put(species.name(), role));
            InsectGenus genus = genusByName.computeIfAbsent(species.genusName(),
                    n -> insectQuery.genera().getByName(n).orElseThrow());
            familyByName.computeIfAbsent(genus.familyName(),
                    n -> insectQuery.families().getByName(n).orElseThrow());
        }
        InsectEntityCollections.ImageGallery gallery = InsectEntityCollections.ImageGallery.of(allImages);

        java.util.Optional<com.naturalist.naturalist.NaturalistName> me = currentNaturalist(request);
        List<InsectSpecies> speciesList;
        if (mine && me.isPresent()) {
            java.util.Set<InsectRankName> mySubjects = insectQuery.fieldObservations()
                    .forNaturalist(me.get()).stream()
                    .map(FieldObservation::subject)
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
        Map<InsectOrderName, InsectOrder> orderByName = new LinkedHashMap<>();
        for (var family : familyPage.content()) {
            orderByName.computeIfAbsent(family.orderName(),
                    n -> insectQuery.orders().getByName(n).orElseThrow());
        }
        Map<InsectRankName, Collection<InsectImage>> imagesByFamily = new LinkedHashMap<>();
        for (var family : familyPage.content()) {
            imagesByFamily.put(family.name(),
                    insectQuery.images().forRankHierarchy(family.name()).stream().toList());
        }
        InsectEntityCollections.ImageGallery gallery = InsectEntityCollections.ImageGallery.grouped(imagesByFamily);
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
        var genera = insectQuery.genera().forFamilyName(familyName).stream()
                .sorted(Comparator.comparing(g -> g.name().value()))
                .toList();
        Map<InsectRankName, Collection<InsectImage>> imagesByGenus = new LinkedHashMap<>();
        for (var g : genera) {
            imagesByGenus.put(g.name(),
                    insectQuery.images().forRankHierarchy(g.name()).stream().toList());
        }
        InsectEntityCollections.ImageGallery gallery = InsectEntityCollections.ImageGallery.grouped(imagesByGenus);
        model.addAttribute("family", family);
        model.addAttribute("order", order);
        model.addAttribute("genera", genera);
        model.addAttribute("gallery", gallery);
        model.addAttribute("citations", insect.get().citations());
        model.addAttribute("featureGroups", featureGroups(familyName));
        model.addAttribute("descriptionPreschool", descriptionRenderer.render(description.preschool()));
        model.addAttribute("descriptionElementary", descriptionRenderer.render(description.elementary()));
        model.addAttribute("descriptionSecondary", descriptionRenderer.render(description.secondary()));
        model.addAttribute("descriptionUniversity", descriptionRenderer.render(description.university()));
        model.addAttribute("breadcrumb", breadcrumbToFamily(family, order));
        model.addAttribute("cladeTrail", cladeTrail(lineageToFamily(family, order)));
        model.addAttribute("ancestorIntros", introsForFamily(order));
        List<InsectImage> rankImages = insectQuery.images()
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
        Map<InsectRankName, Collection<InsectImage>> imagesByOrder = new LinkedHashMap<>();
        for (var order : orderPage.content()) {
            imagesByOrder.put(order.name(),
                    insectQuery.images().forRankHierarchy(order.name()).stream().toList());
        }
        InsectEntityCollections.ImageGallery gallery = InsectEntityCollections.ImageGallery.grouped(imagesByOrder);
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
        var families = insectQuery.families().forOrderName(orderName).stream()
                .sorted(Comparator.comparing(f -> f.name().value()))
                .toList();
        Map<InsectRankName, Collection<InsectImage>> imagesByFamily = new LinkedHashMap<>();
        for (var f : families) {
            imagesByFamily.put(f.name(),
                    insectQuery.images().forRankHierarchy(f.name()).stream().toList());
        }
        InsectEntityCollections.ImageGallery gallery = InsectEntityCollections.ImageGallery.grouped(imagesByFamily);
        model.addAttribute("order", order);
        model.addAttribute("families", families);
        model.addAttribute("gallery", gallery);
        model.addAttribute("citations", insect.get().citations());
        model.addAttribute("featureGroups", featureGroups(orderName));
        model.addAttribute("descriptionPreschool", descriptionRenderer.render(description.preschool()));
        model.addAttribute("descriptionElementary", descriptionRenderer.render(description.elementary()));
        model.addAttribute("descriptionSecondary", descriptionRenderer.render(description.secondary()));
        model.addAttribute("descriptionUniversity", descriptionRenderer.render(description.university()));
        model.addAttribute("breadcrumb", breadcrumbToOrder(order));
        model.addAttribute("cladeTrail", cladeTrail(lineageToOrder(order)));
        model.addAttribute("ancestorIntros", classOnlyIntros());
        List<InsectImage> rankImages = insectQuery.images()
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
        Map<InsectFamilyName, InsectFamily> familyByName = new LinkedHashMap<>();
        for (var genus : genusPage.content()) {
            familyByName.computeIfAbsent(genus.familyName(),
                    n -> insectQuery.families().getByName(n).orElseThrow());
        }
        Map<InsectRankName, Collection<InsectImage>> imagesByGenus = new LinkedHashMap<>();
        for (var genus : genusPage.content()) {
            imagesByGenus.put(genus.name(),
                    insectQuery.images().forRankHierarchy(genus.name()).stream().toList());
        }
        InsectEntityCollections.ImageGallery gallery = InsectEntityCollections.ImageGallery.grouped(imagesByGenus);
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
        var members = insectQuery.species()
                .forGenusName(genusName)
                .stream()
                .sorted(Comparator.comparing(s -> s.name().value()))
                .toList();
        Map<InsectRankName, Collection<InsectImage>> imagesBySpecies = new LinkedHashMap<>();
        for (var s : members) {
            imagesBySpecies.put(s.name(),
                    insectQuery.images().forParentName(s.name()).stream().toList());
        }
        InsectEntityCollections.ImageGallery gallery = InsectEntityCollections.ImageGallery.grouped(imagesBySpecies);
        model.addAttribute("genus", genus);
        model.addAttribute("family", family);
        model.addAttribute("order", order);
        model.addAttribute("species", members);
        model.addAttribute("gallery", gallery);
        model.addAttribute("citations", insect.get().citations());
        model.addAttribute("featureGroups", featureGroups(genusName));
        model.addAttribute("descriptionPreschool", descriptionRenderer.render(description.preschool()));
        model.addAttribute("descriptionElementary", descriptionRenderer.render(description.elementary()));
        model.addAttribute("descriptionSecondary", descriptionRenderer.render(description.secondary()));
        model.addAttribute("descriptionUniversity", descriptionRenderer.render(description.university()));
        model.addAttribute("breadcrumb", breadcrumbToGenus(genus, family, order));
        model.addAttribute("cladeTrail", cladeTrail(lineageToGenus(genus, family, order)));
        model.addAttribute("ancestorIntros", introsForGenus(order, family));
        List<InsectImage> rankImages = insectQuery.images()
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
    String detail(@PathVariable String name, HttpServletRequest request, Model model) {
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
        model.addAttribute("species", s);
        model.addAttribute("genus", genus);
        model.addAttribute("family", family);
        model.addAttribute("order", order);
        model.addAttribute("stages", i.lifeStages().stream()
                .sorted(Comparator.comparingInt(stage -> stage.kind().ordinal()))
                .toList());
        boolean lens = collectionLensOn(request);
        java.util.Optional<com.naturalist.naturalist.NaturalistName> viewer = currentNaturalist(request);
        List<InsectImage> galleryImages = i.observations().stream().toList();
        if (lens && viewer.isPresent()) {
            java.util.Set<FieldObservationId> myObservationIds = insectQuery.fieldObservations()
                    .forNaturalist(viewer.get()).stream()
                    .map(FieldObservation::id)
                    .collect(java.util.stream.Collectors.toSet());
            galleryImages = galleryImages.stream()
                    .filter(img -> img.observationId() != null && myObservationIds.contains(img.observationId()))
                    .toList();
        }
        model.addAttribute("images", galleryImages);
        model.addAttribute("lens", lens && viewer.isPresent());

        var myObservations = viewer.isPresent()
                ? insectQuery.fieldObservations()
                        .forNaturalistAndSubjects(viewer.get(), java.util.Set.<InsectRankName>of(speciesName))
                : null;
        model.addAttribute("observations",
                observationLookup(galleryImages, viewer, speciesName));

        model.addAttribute("citations", i.citations());
        model.addAttribute("featureGroups", featureGroups(speciesName));
        model.addAttribute("role",
                insectQuery.functionalRoles().getByParentName(speciesName).orElse(null));
        boolean collected = myObservations != null && !myObservations.isEmpty();
        model.addAttribute("collected", collected);
        model.addAttribute("descriptionPreschool", descriptionRenderer.render(description.preschool()));
        model.addAttribute("descriptionElementary", descriptionRenderer.render(description.elementary()));
        model.addAttribute("descriptionSecondary", descriptionRenderer.render(description.secondary()));
        model.addAttribute("descriptionUniversity", descriptionRenderer.render(description.university()));
        model.addAttribute("breadcrumb", breadcrumbToSpecies(s, genus, family, order));
        model.addAttribute("cladeTrail", cladeTrail(lineageToSpecies(s, genus, family, order)));
        model.addAttribute("ancestorIntros", introsForSpecies(order, family, genus));
        Object csrf = request.getAttribute(CSRF_REQUEST_ATTRIBUTE);
        if (csrf != null) {
            model.addAttribute("_csrf", csrf);
        }
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
        var observation = new FieldObservation(
                FieldObservationId.create(),
                me.get(),
                InsectSpeciesName.of(name),
                Instant.now(),
                (notes == null || notes.isBlank()) ? null : notes,
                null, null);
        insectCommand.fieldObservations().insert(observation);
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
        var storedFileName = imageStorageService.store(imageFile.getBytes());
        addPhotoCommand.addPhoto(species, storedFileName,
                me.orElse(null), notes, location);
        return "redirect:/insects/" + name;
    }

    @PostMapping("/{name}/notes")
    String updateNotes(@PathVariable String name,
                       @RequestParam("observationId") String observationId,
                       @RequestParam("notes") String notes,
                       @RequestParam(value = "returnPath", required = false) String returnPath,
                       HttpServletRequest request) {
        var destination = safeReturnPath(returnPath, "/insects/" + name);
        var obsId = FieldObservationId.of(java.util.UUID.fromString(observationId));
        var existing = insectQuery.fieldObservations().getByName(obsId);
        if (existing.isEmpty()) {
            return "redirect:" + destination;
        }
        var obs = existing.get();
        if (!owns(obs, currentNaturalist(request))) {
            // Refuse silently rather than 403 — the caller learns nothing about
            // whether the observation exists or who owns it, only that nothing changed.
            return "redirect:" + destination;
        }
        var updated = obs.withNotes((notes == null || notes.isBlank()) ? null : notes);
        insectCommand.fieldObservations().update(updated);
        return "redirect:" + destination;
    }

    /**
     * True when {@code viewer} is signed in as the naturalist who recorded
     * {@code obs}. Guards {@link #updateNotes} against naturalist A overwriting
     * naturalist B's field notes by POSTing B's observation id —
     * {@link FieldObservationId} is a UUIDv7 and therefore time-ordered and
     * partially guessable, so the id alone is not proof of ownership.
     * Package-private (not {@code private}) so it is directly unit-testable
     * without standing up the controller's full servlet/database wiring.
     */
    static boolean owns(FieldObservation obs,
                        java.util.Optional<com.naturalist.naturalist.NaturalistName> viewer) {
        return viewer.isPresent() && obs.observedBy().equals(viewer.get());
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
        return candidate;
    }

    @PostMapping("/{name}/re-identify")
    String reIdentify(@PathVariable String name,
                      @RequestParam("observationId") String observationId,
                      @RequestParam("newSubject") String newSubject,
                      @RequestParam("newSubjectRank") String newSubjectRank,
                      HttpServletRequest request) {
        var obsId = FieldObservationId.of(java.util.UUID.fromString(observationId));
        var existing = insectQuery.fieldObservations().getByName(obsId);
        if (existing.isEmpty()) return "redirect:/insects/" + name;

        var obs = existing.get();
        var newRankName = InsectRankName.of(
                newSubject, com.naturalist.taxonomy.LinealRank.valueOf(newSubjectRank));

        var updatedObs = obs.withSubject(newRankName);
        insectCommand.fieldObservations().update(updatedObs);

        // TODO: update linked images' parentName to newRankName
        // (no query method to find images by observationId yet)

        return "redirect:/insects/" + newSubject;
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

    @GetMapping("/images/{filename}")
    @Resilient(name = IMAGE_CONVERSION)
    ResponseEntity<byte[]> image(@PathVariable String filename) throws IOException {
        byte[] jpeg = jpegCache.get(filename);
        if (jpeg != null) {
            return jpegResponse(jpeg);
        }

        var resource = new ClassPathResource("insects/images/" + filename);
        if (!resource.exists()) {
            return ResponseEntity.notFound().build();
        }

        Path heicTemp = Files.createTempFile("insect-", ".heic");
        Path jpegTemp = Files.createTempFile("insect-", ".jpg");
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

}
