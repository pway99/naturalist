package com.naturalist.insects.console;

import com.naturalist.data.FileName;
import com.naturalist.data.NaturalistDatabase;
import com.naturalist.data.Page;
import com.naturalist.data.PageRequest;
import com.naturalist.fieldnotes.render.DescriptionRenderer;
import com.naturalist.insects.*;
import com.naturalist.insects.console.render.InsectsParagraphCues;
import com.naturalist.insects.lifestage.InsectLifeStageQuery;
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

    @GetMapping
    String list(@RequestParam(defaultValue = "0") int page, Model model) {
        Page<InsectSpecies> speciesPage = insectQuery.species().findPage(PageRequest.console(Math.max(0, page)));
        Map<InsectSpeciesName, List<InsectImage>> imagesBySpecies = new LinkedHashMap<>();
        Map<InsectSpeciesName, InsectFunctionalRole> rolesBySpecies = new LinkedHashMap<>();
        Map<InsectFamilyName, InsectFamily> familyByName = new LinkedHashMap<>();
        Map<InsectGenusName, InsectGenus> genusByName = new LinkedHashMap<>();
        for (var species : speciesPage.content()) {
            imagesBySpecies.put(
                    species.name(),
                    insectQuery.images().forParentName(species.name()).stream().toList());
            insectQuery.functionalRoles().getByParentName(species.name())
                    .ifPresent(role -> rolesBySpecies.put(species.name(), role));
            familyByName.computeIfAbsent(species.familyName(),
                    n -> insectQuery.families().getByName(n).orElseThrow());
            genusByName.computeIfAbsent(species.genusName(),
                    n -> insectQuery.genera().getByName(n).orElseThrow());
        }
        model.addAttribute("speciesPage", speciesPage);
        model.addAttribute("imagesBySpecies", imagesBySpecies);
        model.addAttribute("rolesBySpecies", rolesBySpecies);
        model.addAttribute("familyByName", familyByName);
        model.addAttribute("genusByName", genusByName);
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
        model.addAttribute("familyPage", familyPage);
        model.addAttribute("orderByName", orderByName);
        return "insects/families";
    }

    @GetMapping("/families/{name}")
    String familyDetail(@PathVariable String name, Model model) {
        var familyName = InsectFamilyName.of(name);
        var family = insectQuery.families().getByName(familyName);
        if (family.isEmpty()) {
            return "redirect:/insects/families";
        }
        InsectOrder order = insectQuery.orders().getByName(family.get().orderName()).orElseThrow();
        var description = family.get().description();
        var genera = insectQuery.genera().forFamilyName(familyName).stream()
                .sorted(Comparator.comparing(g -> g.name().value()))
                .toList();
        model.addAttribute("family", family.get());
        model.addAttribute("order", order);
        model.addAttribute("genera", genera);
        model.addAttribute("descriptionPreschool", descriptionRenderer.render(description.preschool()));
        model.addAttribute("descriptionElementary", descriptionRenderer.render(description.elementary()));
        model.addAttribute("descriptionSecondary", descriptionRenderer.render(description.secondary()));
        model.addAttribute("descriptionUniversity", descriptionRenderer.render(description.university()));
        return "insects/family";
    }

    @GetMapping("/orders")
    String orders(@RequestParam(defaultValue = "0") int page, Model model) {
        Page<InsectOrder> orderPage = insectQuery.orders()
                .findPage(PageRequest.console(Math.max(0, page)));
        model.addAttribute("orderPage", orderPage);
        return "insects/orders";
    }

    @GetMapping("/orders/{name}")
    String orderDetail(@PathVariable String name, Model model) {
        var orderName = InsectOrderName.of(name);
        var order = insectQuery.orders().getByName(orderName);
        if (order.isEmpty()) {
            return "redirect:/insects/orders";
        }
        var description = order.get().description();
        var families = insectQuery.families().forOrderName(orderName).stream()
                .sorted(Comparator.comparing(f -> f.name().value()))
                .toList();
        model.addAttribute("order", order.get());
        model.addAttribute("families", families);
        model.addAttribute("descriptionPreschool", descriptionRenderer.render(description.preschool()));
        model.addAttribute("descriptionElementary", descriptionRenderer.render(description.elementary()));
        model.addAttribute("descriptionSecondary", descriptionRenderer.render(description.secondary()));
        model.addAttribute("descriptionUniversity", descriptionRenderer.render(description.university()));
        return "insects/order";
    }

    @GetMapping("/genera")
    String genera(@RequestParam(defaultValue = "0") int page, Model model) {
        Page<InsectGenus> genusPage = insectQuery.genera()
                .findPage(PageRequest.console(Math.max(0, page)));
        model.addAttribute("genusPage", genusPage);
        return "insects/genera";
    }

    @GetMapping("/genera/{name}")
    String genusDetail(@PathVariable String name, Model model) {
        var genusName = InsectGenusName.of(name);
        var genus = insectQuery.genera().getByName(genusName);
        if (genus.isEmpty()) {
            return "redirect:/insects/genera";
        }
        InsectFamily family = insectQuery.families().getByName(genus.get().familyName()).orElseThrow();
        InsectOrder order = insectQuery.orders().getByName(family.orderName()).orElseThrow();
        var description = genus.get().description();
        var members = insectQuery.species()
                .forGenusName(genusName)
                .stream()
                .sorted(Comparator.comparing(s -> s.name().value()))
                .toList();
        model.addAttribute("genus", genus.get());
        model.addAttribute("family", family);
        model.addAttribute("order", order);
        model.addAttribute("species", members);
        model.addAttribute("descriptionPreschool", descriptionRenderer.render(description.preschool()));
        model.addAttribute("descriptionElementary", descriptionRenderer.render(description.elementary()));
        model.addAttribute("descriptionSecondary", descriptionRenderer.render(description.secondary()));
        model.addAttribute("descriptionUniversity", descriptionRenderer.render(description.university()));
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
        return "insects/guild";
    }

    @GetMapping("/{name}")
    String detail(@PathVariable String name, HttpServletRequest request, Model model) {
        var speciesName = InsectSpeciesName.of(name);
        var species = insectQuery.species().getByName(speciesName);
        if (species.isEmpty()) {
            return "redirect:/insects";
        }
        InsectSpecies s = species.get();
        InsectGenus genus = insectQuery.genera().getByName(s.genusName()).orElseThrow();
        InsectFamily family = insectQuery.families().getByName(s.familyName()).orElseThrow();
        InsectOrder order = insectQuery.orders().getByName(family.orderName()).orElseThrow();
        InsectEntityCollections.ImageCollection images = insectQuery.images().forParentName(speciesName);
        var description = s.description();
        model.addAttribute("species", s);
        model.addAttribute("genus", genus);
        model.addAttribute("family", family);
        model.addAttribute("order", order);
        model.addAttribute("images", images.stream().toList());
        model.addAttribute("role",
                insectQuery.functionalRoles().getByParentName(speciesName).orElse(null));
        model.addAttribute("descriptionPreschool", descriptionRenderer.render(description.preschool()));
        model.addAttribute("descriptionElementary", descriptionRenderer.render(description.elementary()));
        model.addAttribute("descriptionSecondary", descriptionRenderer.render(description.secondary()));
        model.addAttribute("descriptionUniversity", descriptionRenderer.render(description.university()));
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
        InsectFamily family = insectQuery.families().getByName(s.familyName()).orElseThrow();
        InsectOrder order = insectQuery.orders().getByName(family.orderName()).orElseThrow();
        var stages = insectLifeStageQuery.lifeStages().forParentName(speciesName).stream()
                .sorted(Comparator.comparingInt(stage -> stage.kind().ordinal()))
                .toList();
        model.addAttribute("species", s);
        model.addAttribute("family", family);
        model.addAttribute("order", order);
        model.addAttribute("stages", stages);
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
}
