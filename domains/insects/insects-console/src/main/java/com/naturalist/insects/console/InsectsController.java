package com.naturalist.insects.console;

import com.naturalist.insects.*;
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

    private final InsectSpeciesTestEntitySource speciesSource;
    private final InsectImageTestEntitySource imageSource;
    private final Map<String, byte[]> jpegCache = new ConcurrentHashMap<>();

    InsectsController(InsectSpeciesTestEntitySource speciesSource,
                      InsectImageTestEntitySource imageSource) {
        this.speciesSource = speciesSource;
        this.imageSource = imageSource;
    }

    @GetMapping
    String list(Model model) {
        var species = speciesSource.entityStream()
                .sorted(Comparator.comparing(s -> s.name().value()))
                .toList();
        model.addAttribute("species", species);
        return "insects/list";
    }

    @GetMapping("/{name}")
    String detail(@PathVariable String name, Model model) {
        var speciesName = InsectSpeciesName.of(name);
        var species = speciesSource.getByName(speciesName);
        if (species.isEmpty()) {
            return "redirect:/insects";
        }
        var images = imageSource.entityStream()
                .filter(img -> img.insectSpeciesName().equals(speciesName))
                .toList();
        model.addAttribute("species", species.get());
        model.addAttribute("images", images);
        return "insects/detail";
    }

    @GetMapping("/images/{filename}")
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
