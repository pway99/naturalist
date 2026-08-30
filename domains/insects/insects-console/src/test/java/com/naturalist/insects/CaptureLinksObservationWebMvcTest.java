package com.naturalist.insects;

import com.naturalist.data.NaturalistTestExtension;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Capturing a photo as a signed-in naturalist adds the species to that
 * naturalist's collection: a subsequent {@code /insects/species} view with the
 * collection lens on lists the newly-observed species.
 *
 * <p>A DB-free, scan-free, security-free {@code @WebMvcTest} slice
 * ({@link InsectsControllerTestConfig}). The signed-in naturalist is supplied
 * through the {@code naturalist.currentNaturalistName} request attribute — the
 * same seam {@code InsectsController} reads in production (written there by the
 * app's {@code NaturalistHeaderInterceptor}) — so no Spring Security is needed.
 * The write (POST image) and read (GET species) share the slice's single
 * in-memory {@code NaturalistDatabase}, so the recorded observation is visible
 * to the follow-up query.
 */
@WebMvcTest
class CaptureLinksObservationWebMvcTest {

    // The slice's shared in-memory database (owned by the config), registered here so
    // JUnit resets it per test. Same instance the InsectsController beans wire from.
    @RegisterExtension
    static final NaturalistTestExtension database = InsectsControllerTestConfig.DATABASE;

    private static final String CURRENT_NATURALIST = "naturalist.currentNaturalistName";
    private static final Path IMAGE_DIR = Path.of("data/images/insects");

    /** Minimal valid JPEG: FF D8 FF E0 header + padding. */
    private static final byte[] TINY_JPEG = {
            (byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0
    };

    @Autowired
    MockMvc mockMvc;

    @Test
    void capture_asNaturalist_addsSpeciesToCollection() throws Exception {
        var file = new MockMultipartFile("image", "test.jpg", "image/jpeg", TINY_JPEG);
        mockMvc.perform(multipart("/insects/battus-philenor/images").file(file)
                        .requestAttr(CURRENT_NATURALIST, "amir-hassan"))
                .andExpect(status().is3xxRedirection());

        mockMvc.perform(get("/insects/species")
                        .sessionAttr("insects.collectionLens", true)
                        .requestAttr(CURRENT_NATURALIST, "amir-hassan"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("battus-philenor")));
    }

    @AfterAll
    static void cleanUpTestImages() throws IOException {
        if (Files.isDirectory(IMAGE_DIR)) {
            try (var files = Files.list(IMAGE_DIR)) {
                files.forEach(f -> { try { Files.delete(f); } catch (IOException ignored) { } });
            }
        }
    }
}
