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
        for (var species : speciesPage.content()) {
            imagesBySpecies.put(
                    species.name(),
                    insectQuery.images().forSpeciesName(species.name()).stream().toList());
        }
        model.addAttribute("speciesPage", speciesPage);
        model.addAttribute("imagesBySpecies", imagesBySpecies);
        return "insects/list";
    }

    @GetMapping("/guild/{guild}")
    String guild(@PathVariable String guild, Model model) {
        FunctionalGuild selected;
        try {
            selected = FunctionalGuild.valueOf(guild.toUpperCase());
        } catch (IllegalArgumentException e) {
            return "redirect:/insects";
        }
        var species = insectQuery.species().getByFunctionalGuild(selected).stream()
                .sorted(Comparator.comparing(s -> s.name().value()))
                .toList();
        model.addAttribute("selectedGuild", selected);
        model.addAttribute("guilds", FunctionalGuild.values());
        model.addAttribute("species", species);
        return "insects/guild";
    }

    @GetMapping("/{name}")
    String detail(@PathVariable String name, HttpServletRequest request, Model model) {
        var speciesName = InsectSpeciesName.of(name);
        var species = insectQuery.species().getByName(speciesName);
        if (species.isEmpty()) {
            return "redirect:/insects";
        }
        InsectEntityCollections.ImageCollection images = insectQuery.images().forSpeciesName(speciesName);
        var description = species.get().description();
        model.addAttribute("species", species.get());
        model.addAttribute("images", images.stream().toList());
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
        var stages = insectLifeStageQuery.lifeStages().forSpeciesName(speciesName).stream()
                .sorted(Comparator.comparingInt(stage -> stage.kind().ordinal()))
                .toList();
        model.addAttribute("species", species.get());
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
