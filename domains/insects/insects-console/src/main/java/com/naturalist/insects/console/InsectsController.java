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
import com.naturalist.resilience.Resilience;
import com.naturalist.resilience.Resilient;
import jakarta.servlet.http.HttpServletRequest;
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

    private final InsectQuery insectQuery;
    private final InsectCommand insectCommand;
    private final InsectLifeStageQuery insectLifeStageQuery;
    private final Resilience resilience;
    private final DescriptionRenderer descriptionRenderer;
    private final Map<String, byte[]> jpegCache = new ConcurrentHashMap<>();

    InsectsController(Resilience resilience) {
        //TODO:: This will eventually be a spring managed bean
        InsectsTestContext context = InsectsTestContext.create(NaturalistDatabase.create());
        this.insectQuery = context.insectQuery();
        this.insectCommand = context.insectCommand();
        this.insectLifeStageQuery = context.insectLifeStageQuery();
        this.resilience = resilience;
        this.descriptionRenderer = new DescriptionRenderer(InsectsParagraphCues.CUES);
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

    @GetMapping
    String index() {
        return "redirect:/insects/orders";
    }

    @GetMapping("/species")
    String list(@RequestParam(defaultValue = "0") int page, Model model) {
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
        model.addAttribute("speciesPage", speciesPage);
        model.addAttribute("gallery", gallery);
        model.addAttribute("rolesBySpecies", rolesBySpecies);
        model.addAttribute("familyByName", familyByName);
        model.addAttribute("genusByName", genusByName);
        model.addAttribute("ancestorIntros", classOnlyIntros());
        model.addAttribute("breadcrumb", cladePrefix());
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
            imagesByFamily.put(family.name(), imagesForFamily(family.name()));
        }
        InsectEntityCollections.ImageGallery gallery = InsectEntityCollections.ImageGallery.grouped(imagesByFamily);
        model.addAttribute("familyPage", familyPage);
        model.addAttribute("orderByName", orderByName);
        model.addAttribute("gallery", gallery);
        model.addAttribute("ancestorIntros", classOnlyIntros());
        model.addAttribute("breadcrumb", cladePrefix());
        return "insects/families";
    }

    @GetMapping("/families/{name}")
    String familyDetail(@PathVariable String name, Model model) {
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
            imagesByGenus.put(g.name(), imagesForGenus(g.name()));
        }
        InsectEntityCollections.ImageGallery gallery = InsectEntityCollections.ImageGallery.grouped(imagesByGenus);
        model.addAttribute("family", family);
        model.addAttribute("order", order);
        model.addAttribute("genera", genera);
        model.addAttribute("gallery", gallery);
        model.addAttribute("citations", insect.get().citations());
        model.addAttribute("descriptionPreschool", descriptionRenderer.render(description.preschool()));
        model.addAttribute("descriptionElementary", descriptionRenderer.render(description.elementary()));
        model.addAttribute("descriptionSecondary", descriptionRenderer.render(description.secondary()));
        model.addAttribute("descriptionUniversity", descriptionRenderer.render(description.university()));
        model.addAttribute("breadcrumb", breadcrumbToFamily(family, order));
        model.addAttribute("ancestorIntros", introsForFamily(order));
        return "insects/family";
    }

    @GetMapping("/orders")
    String orders(@RequestParam(defaultValue = "0") int page, Model model) {
        Page<InsectOrder> orderPage = insectQuery.orders()
                .findPage(PageRequest.console(Math.max(0, page)));
        Map<InsectRankName, Collection<InsectImage>> imagesByOrder = new LinkedHashMap<>();
        for (var order : orderPage.content()) {
            imagesByOrder.put(order.name(), imagesForOrder(order.name()));
        }
        InsectEntityCollections.ImageGallery gallery = InsectEntityCollections.ImageGallery.grouped(imagesByOrder);
        model.addAttribute("orderPage", orderPage);
        model.addAttribute("gallery", gallery);
        model.addAttribute("ancestorIntros", classOnlyIntros());
        model.addAttribute("breadcrumb", cladePrefix());
        return "insects/orders";
    }

    @GetMapping("/orders/{name}")
    String orderDetail(@PathVariable String name, Model model) {
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
            imagesByFamily.put(f.name(), imagesForFamily(f.name()));
        }
        InsectEntityCollections.ImageGallery gallery = InsectEntityCollections.ImageGallery.grouped(imagesByFamily);
        model.addAttribute("order", order);
        model.addAttribute("families", families);
        model.addAttribute("gallery", gallery);
        model.addAttribute("citations", insect.get().citations());
        model.addAttribute("descriptionPreschool", descriptionRenderer.render(description.preschool()));
        model.addAttribute("descriptionElementary", descriptionRenderer.render(description.elementary()));
        model.addAttribute("descriptionSecondary", descriptionRenderer.render(description.secondary()));
        model.addAttribute("descriptionUniversity", descriptionRenderer.render(description.university()));
        model.addAttribute("breadcrumb", breadcrumbToOrder(order));
        model.addAttribute("ancestorIntros", classOnlyIntros());
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
            imagesByGenus.put(genus.name(), imagesForGenus(genus.name()));
        }
        InsectEntityCollections.ImageGallery gallery = InsectEntityCollections.ImageGallery.grouped(imagesByGenus);
        model.addAttribute("genusPage", genusPage);
        model.addAttribute("familyByName", familyByName);
        model.addAttribute("gallery", gallery);
        model.addAttribute("ancestorIntros", classOnlyIntros());
        model.addAttribute("breadcrumb", cladePrefix());
        return "insects/genera";
    }

    @GetMapping("/genera/{name}")
    String genusDetail(@PathVariable String name, Model model) {
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
        model.addAttribute("descriptionPreschool", descriptionRenderer.render(description.preschool()));
        model.addAttribute("descriptionElementary", descriptionRenderer.render(description.elementary()));
        model.addAttribute("descriptionSecondary", descriptionRenderer.render(description.secondary()));
        model.addAttribute("descriptionUniversity", descriptionRenderer.render(description.university()));
        model.addAttribute("breadcrumb", breadcrumbToGenus(genus, family, order));
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
        var roles = insectQuery.functionalRoles().getByGuild(selected).stream()
                .sorted(Comparator.comparing(r -> r.parentName().value()))
                .toList();
        model.addAttribute("selectedGuild", selected);
        model.addAttribute("guilds", FunctionalGuild.values());
        model.addAttribute("roles", roles);
        model.addAttribute("ancestorIntros", classOnlyIntros());
        model.addAttribute("breadcrumb", cladePrefix());
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
        model.addAttribute("images", i.observations().stream().toList());
        model.addAttribute("citations", i.citations());
        model.addAttribute("role",
                insectQuery.functionalRoles().getByParentName(speciesName).orElse(null));
        model.addAttribute("descriptionPreschool", descriptionRenderer.render(description.preschool()));
        model.addAttribute("descriptionElementary", descriptionRenderer.render(description.elementary()));
        model.addAttribute("descriptionSecondary", descriptionRenderer.render(description.secondary()));
        model.addAttribute("descriptionUniversity", descriptionRenderer.render(description.university()));
        model.addAttribute("breadcrumb", breadcrumbToSpecies(s, genus, family, order));
        model.addAttribute("ancestorIntros", introsForSpecies(order, family, genus));
        Object csrf = request.getAttribute(CSRF_REQUEST_ATTRIBUTE);
        if (csrf != null) {
            model.addAttribute("_csrf", csrf);
        }
        return "insects/detail";
    }

    @PostMapping("/{name}/images")
    String addImage(@PathVariable String name, @RequestParam("resourceName") String resourceName) {
        var speciesName = InsectSpeciesName.of(name);
        var image = new InsectImage(
                InsectImageId.create(),
                speciesName,
                Instant.now(),
                FileName.of(resourceName));
        insectCommand.images().insert(image);
        return "redirect:/insects/" + name;
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

    private List<InsectImage> imagesForGenus(InsectGenusName genusName) {
        List<InsectImage> images = new ArrayList<>(
                insectQuery.images().forParentName(genusName).stream().toList());
        for (var species : insectQuery.species().forGenusName(genusName).stream().toList()) {
            images.addAll(insectQuery.images().forParentName(species.name()).stream().toList());
        }
        return images;
    }

    private List<InsectImage> imagesForFamily(InsectFamilyName familyName) {
        List<InsectImage> images = new ArrayList<>(
                insectQuery.images().forParentName(familyName).stream().toList());
        for (var genus : insectQuery.genera().forFamilyName(familyName).stream().toList()) {
            images.addAll(imagesForGenus(genus.name()));
        }
        return images;
    }

    private List<InsectImage> imagesForOrder(InsectOrderName orderName) {
        List<InsectImage> images = new ArrayList<>(
                insectQuery.images().forParentName(orderName).stream().toList());
        for (var family : insectQuery.families().forOrderName(orderName).stream().toList()) {
            images.addAll(imagesForFamily(family.name()));
        }
        return images;
    }
}
