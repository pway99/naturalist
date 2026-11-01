package com.naturalist.insects;

import com.naturalist.data.NaturalistTestExtension;
import com.naturalist.observation.ImageUpload;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Catalog render + add-photo behavior for {@link InsectsController}: the species
 * listing renders its heading, the detail page renders the add-photo form and the
 * life-stages section, and posting a photo redirects back to the species. A
 * DB-free, scan-free, security-free {@code @WebMvcTest} slice
 * ({@link InsectsControllerTestConfig}). The CSRF hidden field is driven by the
 * {@code naturalistCsrfParam}/{@code naturalistCsrfToken} request attributes the
 * app's {@code NaturalistHeaderInterceptor} publishes in production, set directly
 * here. Security-filter behavior (login redirects, CSRF enforcement) is asserted
 * app-side in {@code console.auth.InsectsRouteSecurityWebMvcTest}.
 */
@WebMvcTest
class InsectsCatalogWebMvcTest {

    @RegisterExtension
    static final NaturalistTestExtension database = InsectsControllerTestConfig.DATABASE;

    private static final String CURRENT_NATURALIST = "naturalist.currentNaturalistName";

    /** Minimal valid JPEG: FF D8 FF E0 header + padding. */
    private static final byte[] TINY_JPEG = {
            (byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0,
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0
    };

    /** An ISO-BMFF {@code ftypheic} header: a HEIC photo, which the store rejects. */
    private static final byte[] TINY_HEIC = {
            0, 0, 0, 0x18, 'f', 't', 'y', 'p', 'h', 'e', 'i', 'c',
            0, 0, 0, 0
    };

    @Autowired
    MockMvc mockMvc;

    @Test
    void listing_rendersCatalogHeading() throws Exception {
        mockMvc.perform(get("/insects/species").requestAttr(CURRENT_NATURALIST, "patrick-way"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/html"))
                .andExpect(content().string(containsString("Insect Species")))
                .andExpect(content().string(containsString("species documented at Oak Vista")));
    }

    @Test
    void detail_rendersAddPhotoForm() throws Exception {
        mockMvc.perform(get("/insects/battus-philenor")
                        .requestAttr(CURRENT_NATURALIST, "patrick-way")
                        .requestAttr("naturalistCsrfParam", "_csrf")
                        .requestAttr("naturalistCsrfToken", "test-token"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Add Photo")))
                .andExpect(content().string(containsString("name=\"image\"")))
                .andExpect(content().string(containsString("name=\"_csrf\"")));
    }

    @Test
    void detail_rendersLifeStagesCollapsible() throws Exception {
        mockMvc.perform(get("/insects/battus-philenor").requestAttr(CURRENT_NATURALIST, "patrick-way"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("data-storage-key=\"life-stages\"")))
                .andExpect(content().string(containsString("data-storage-key=\"life-stage-egg\"")))
                .andExpect(content().string(containsString("<h2>Life Stages</h2>")));
    }

    @Test
    void detail_doesNotRenderStandalonePlateLink() throws Exception {
        mockMvc.perform(get("/insects/battus-philenor").requestAttr(CURRENT_NATURALIST, "patrick-way"))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("View Life Stages"))));
    }

    @Test
    void addImage_redirectsToDetail() throws Exception {
        var file = new MockMultipartFile("image", "test.jpg", "image/jpeg", TINY_JPEG);
        mockMvc.perform(multipart("/insects/battus-philenor/images").file(file)
                        .requestAttr(CURRENT_NATURALIST, "patrick-way"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/insects/battus-philenor"));
    }

    @Test
    void addImage_rejectsUnsupportedFormat() throws Exception {
        var file = new MockMultipartFile("image", "photo.heic", "image/heic", TINY_HEIC);
        mockMvc.perform(multipart("/insects/battus-philenor/images").file(file)
                        .requestAttr(CURRENT_NATURALIST, "patrick-way"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/insects/battus-philenor?error=image"));
    }

    @Test
    void detail_rendersImageErrorWhenFlagged() throws Exception {
        mockMvc.perform(get("/insects/battus-philenor?error=image").requestAttr(CURRENT_NATURALIST, "patrick-way"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("not a JPEG, PNG or WebP")));
    }

    @Test
    void image_servesStoredUpload() throws Exception {
        var name = InsectsControllerTestConfig.IMAGE_STORE.store("insects", ImageUpload.of(TINY_JPEG));
        mockMvc.perform(get("/insects/images/" + name.value()))
                .andExpect(status().isOk())
                .andExpect(content().contentType("image/jpeg"))
                .andExpect(header().string("Cache-Control", containsString("immutable")))
                .andExpect(content().bytes(TINY_JPEG));
    }

    @Test
    void image_missingIs404() throws Exception {
        mockMvc.perform(get("/insects/images/nope.jpg"))
                .andExpect(status().isNotFound());
    }

    @Test
    void identify_rejectsUnsupportedFormat() throws Exception {
        var file = new MockMultipartFile("image", "photo.heic", "image/heic", TINY_HEIC);
        mockMvc.perform(multipart("/insects/identify").file(file)
                        .requestAttr(CURRENT_NATURALIST, "patrick-way"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/insects/identify?error=image"));
    }

    @Test
    void addImage_rejectsOversize() throws Exception {
        var oversize = new byte[ImageUpload.MAX_BYTES + 1];
        oversize[0] = (byte) 0xFF;
        oversize[1] = (byte) 0xD8;
        oversize[2] = (byte) 0xFF;
        var file = new MockMultipartFile("image", "huge.jpg", "image/jpeg", oversize);
        mockMvc.perform(multipart("/insects/battus-philenor/images").file(file)
                        .requestAttr(CURRENT_NATURALIST, "patrick-way"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/insects/battus-philenor?error=image"));
    }
}
