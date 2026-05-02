package com.naturalist.insects.console;

import com.naturalist.data.NaturalistDatabase;
import com.naturalist.ddd.EntityName;
import com.naturalist.insects.*;
import com.naturalist.insects.lifestage.InsectLifeStageQuery;
import com.naturalist.resilience.Resilient;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Controller
@RequestMapping("/insects")
public class InsectsController {
    private final InsectQuery insectQuery;
    private final InsectLifeStageQuery insectLifeStageQuery;
    private final Map<String, byte[]> jpegCache = new ConcurrentHashMap<>();

    InsectsController() {
        //TODO:: This will eventually be a spring managed bean
        InsectsTestContext context = InsectsTestContext.create(NaturalistDatabase.create());
        this.insectQuery = context.insectQuery();
        this.insectLifeStageQuery = context.insectLifeStageQuery();
    }

    @GetMapping
    String list(Model model) {
        var species = insectQuery.species().allSpeciesNames().stream()
                .sorted(Comparator.comparing(EntityName::value))
                .map(name -> insectQuery.species()
                        .getByName(name)
                        .get())
                .toList();
        model.addAttribute("species", species);
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
    String detail(@PathVariable String name, Model model) {
        var speciesName = InsectSpeciesName.of(name);
        var species = insectQuery.species().getByName(speciesName);
        if (species.isEmpty()) {
            return "redirect:/insects";
        }
        InsectEntityCollections.ImageCollection images = insectQuery.images().forSpeciesName(speciesName);
        model.addAttribute("species", species.get());
        model.addAttribute("images", images.stream().toList());
        return "insects/detail";
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
    @Resilient(name = "image.conversion")
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
            jpeg = Files.readAllBytes(jpegTemp);
            jpegCache.put(filename, jpeg);
            return jpegResponse(jpeg);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return ResponseEntity.internalServerError().build();
        } finally {
            Files.deleteIfExists(heicTemp);
            Files.deleteIfExists(jpegTemp);
        }
    }

    private ResponseEntity<byte[]> jpegResponse(byte[] jpeg) {
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_JPEG)
                .cacheControl(CacheControl.maxAge(1, TimeUnit.HOURS))
                .body(jpeg);
    }
}
