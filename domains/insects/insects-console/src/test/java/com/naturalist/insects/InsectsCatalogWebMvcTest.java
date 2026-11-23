package com.naturalist.insects;

import com.naturalist.observation.test.ImageFixtures;
import com.naturalist.data.FileName;
import com.naturalist.data.NaturalistTestExtension;
import com.naturalist.naturalist.NaturalistName;
import com.naturalist.observation.ImageUpload;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import java.io.IOException;
import java.io.ByteArrayInputStream;
import java.io.UncheckedIOException;
import java.io.ByteArrayOutputStream;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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

    /** A small JPEG the store can decode. */
    private static final byte[] TINY_JPEG = ImageFixtures.decodableJpeg();

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
        MockMultipartFile file = new MockMultipartFile("image", "test.jpg", "image/jpeg", TINY_JPEG);
        mockMvc.perform(multipart("/insects/battus-philenor/images").file(file)
                .requestAttr(CURRENT_NATURALIST, "patrick-way"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/insects/battus-philenor"));
    }

    @Test
    void addImage_rejectsUnsupportedFormat() throws Exception {
        MockMultipartFile file = new MockMultipartFile("image", "photo.heic", "image/heic", TINY_HEIC);
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
        FileName name = InsectsControllerTestConfig.IMAGE_STORE.store("insects", ImageUpload.of(TINY_JPEG));
        mockMvc.perform(get("/insects/images/" + name.value()))
                .andExpect(status().isOk())
                .andExpect(content().contentType("image/jpeg"))
                .andExpect(header().string("Cache-Control", containsString("immutable")))
                .andExpect(content().bytes(TINY_JPEG));
    }

    @Test
    void image_missingIs404() throws Exception {
        mockMvc.perform(get("/insects/images/nope.jpg"))
                .andExpect(status().isNotFound())
                .andExpect(header().string("Cache-Control", "no-store"));
    }

    @Test
    void identify_rejectsUnsupportedFormat() throws Exception {
        MockMultipartFile file = new MockMultipartFile("image", "photo.heic", "image/heic", TINY_HEIC);
        mockMvc.perform(multipart("/insects/identify").file(file)
                .requestAttr(CURRENT_NATURALIST, "patrick-way"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/insects/identify?error=image"));
    }

    @Test
    void updateNotes_byNonOwner_redirectsWithoutChange() throws Exception {
        mockMvc.perform(post("/insects/battus-philenor/observe").param("notes", "original")
                .requestAttr(CURRENT_NATURALIST, "pat-way"))
                .andExpect(status().is3xxRedirection());
        InsectObservationId id = InsectsControllerTestConfig.INSECTS.insectQuery().observations()
                .forNaturalist(NaturalistName.of("pat-way")).stream().findFirst().orElseThrow().id();

        mockMvc.perform(post("/insects/battus-philenor/notes")
                .param("observationId", id.value().toString())
                .param("notes", "overwritten")
                .requestAttr(CURRENT_NATURALIST, "mallory"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/insects/battus-philenor"));

        assertThat(InsectsControllerTestConfig.INSECTS.insectQuery().observations().getByName(id)
                .orElseThrow().notes()).isEqualTo("original");
    }

    @Test
    void updateNotes_byOwner_changesNotes() throws Exception {
        mockMvc.perform(post("/insects/battus-philenor/observe").param("notes", "original")
                .requestAttr(CURRENT_NATURALIST, "pat-way"))
                .andExpect(status().is3xxRedirection());
        InsectObservationId id = InsectsControllerTestConfig.INSECTS.insectQuery().observations()
                .forNaturalist(NaturalistName.of("pat-way")).stream().findFirst().orElseThrow().id();

        mockMvc.perform(post("/insects/battus-philenor/notes")
                .param("observationId", id.value().toString())
                .param("notes", "revised")
                .requestAttr(CURRENT_NATURALIST, "pat-way"))
                .andExpect(status().is3xxRedirection());

        assertThat(InsectsControllerTestConfig.INSECTS.insectQuery().observations().getByName(id)
                .orElseThrow().notes()).isEqualTo("revised");
    }

    @Test
    void addImage_rejectsOversize() throws Exception {
        byte[] oversize = new byte[ImageUpload.MAX_BYTES + 1];
        oversize[0] = (byte) 0xFF;
        oversize[1] = (byte) 0xD8;
        oversize[2] = (byte) 0xFF;
        MockMultipartFile file = new MockMultipartFile("image", "huge.jpg", "image/jpeg", oversize);
        mockMvc.perform(multipart("/insects/battus-philenor/images").file(file)
                .requestAttr(CURRENT_NATURALIST, "patrick-way"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/insects/battus-philenor?error=image"));
    }

    @Test
    void identify_redirectsWithErrorImage_whenThePhotoIsUnstorable() throws Exception {
        long storedBefore = storedFiles();
        MockMultipartFile file = new MockMultipartFile("image", "bomb.png", "image/png", pixelBomb());

        mockMvc.perform(multipart("/insects/identify").file(file)
                .requestAttr(CURRENT_NATURALIST, "patrick-way"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/insects/identify?error=image"));

        assertThat(storedFiles()).as("nothing is stored").isEqualTo(storedBefore);
    }

    @Test
    void addImage_redirectsWithErrorImage_whenThePhotoIsUnstorable() throws Exception {
        long storedBefore = storedFiles();
        MockMultipartFile file = new MockMultipartFile("image", "bomb.png", "image/png", pixelBomb());

        mockMvc.perform(multipart("/insects/battus-philenor/images").file(file)
                .requestAttr(CURRENT_NATURALIST, "patrick-way"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/insects/battus-philenor?error=image"));

        assertThat(storedFiles()).as("nothing is stored").isEqualTo(storedBefore);
    }

    /** A well-formed PNG whose header declares 50000 x 50000 pixels: a valid upload the store cannot use. */
    private static byte[] pixelBomb() {
        byte[] header = ByteBuffer.allocate(13).putInt(50_000).putInt(50_000).put((byte) 8).put((byte) 2)
                .put((byte) 0).put((byte) 0).put((byte) 0).array();
        return ImageFixtures.png(ImageFixtures.chunk("IHDR", header), ImageFixtures.idat(), ImageFixtures.iend());
    }

    private static long storedFiles() throws IOException {
        try (Stream<Path> walk = Files.walk(InsectsControllerTestConfig.IMAGE_ROOT)) {
            return walk.filter(Files::isRegularFile).count();
        }
    }

    @Test
    void tile_missingIs404NoStore() throws Exception {
        mockMvc.perform(get("/insects/images/tile/nope.jpg"))
                .andExpect(status().isNotFound())
                .andExpect(header().string("Cache-Control", "no-store"));
    }

    private static byte[] largeJpeg() {
        BufferedImage image = new BufferedImage(2000, 1500, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream encoded = new ByteArrayOutputStream();
        try {
            ImageIO.write(image, "jpg", encoded);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return encoded.toByteArray();
    }

    @Test
    void tile_servesTheStoredTile() throws Exception {
        FileName name = InsectsControllerTestConfig.IMAGE_STORE.store("insects", ImageUpload.of(largeJpeg()));
        byte[] body = mockMvc.perform(get("/insects/images/tile/" + name.value()))
                .andExpect(status().isOk())
                .andExpect(content().contentType("image/jpeg"))
                .andExpect(header().string("Cache-Control", containsString("immutable")))
                .andReturn().getResponse().getContentAsByteArray();
        BufferedImage tile = ImageIO.read(new ByteArrayInputStream(body));
        assertThat(tile.getWidth()).isEqualTo(640);
        assertThat(tile.getHeight()).isEqualTo(480);
    }

    @Test
    void rankPage_showsTilesNotPhotos() throws Exception {
        mockMvc.perform(get("/insects/battus-philenor").requestAttr(CURRENT_NATURALIST, "patrick-way"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("src=\"/insects/images/tile/pipevine-swallow-tail.jpg\"")))
                .andExpect(content().string(containsString("/images/photo-not-found.svg")))
                .andExpect(content().string(not(containsString("src=\"/insects/images/pipevine-swallow-tail.jpg\""))));
    }

    @Test
    void gallery_linksEachTileToItsPhoto() throws Exception {
        mockMvc.perform(get("/insects/battus-philenor").requestAttr(CURRENT_NATURALIST, "patrick-way"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("data-photo-view")))
                .andExpect(content().string(containsString("href=\"/insects/images/pipevine-swallow-tail.jpg\"")));
    }

    @Test
    void listing_cardsShowTilesLinkedToTheRankPage() throws Exception {
        mockMvc.perform(get("/insects/species").requestAttr(CURRENT_NATURALIST, "patrick-way"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("src=\"/insects/images/tile/pipevine-swallow-tail.jpg\"")))
                .andExpect(content().string(containsString("/images/photo-not-found.svg")))
                .andExpect(content().string(containsString("href=\"/insects/battus-philenor\" class=\"card-image-link\"")))
                .andExpect(content().string(not(containsString("src=\"/insects/images/pipevine-swallow-tail.jpg\""))));
    }

    /** A photo whose header reads but whose body does not decode passes the header check and is refused by the store: still {@code ?error=image}, never a 500. */
    @Test
    void identify_redirectsWithErrorImage_whenThePhotoBodyDoesNotDecode() throws Exception {
        long storedBefore = storedFiles();
        MockMultipartFile file = new MockMultipartFile("image", "corrupt.png", "image/png", ImageFixtures.corruptBodyPng());

        mockMvc.perform(multipart("/insects/identify").file(file)
                .requestAttr(CURRENT_NATURALIST, "patrick-way"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/insects/identify?error=image"));

        assertThat(storedFiles()).as("nothing is stored").isEqualTo(storedBefore);
    }

    /** Adding a photo whose body does not decode is answered {@code ?error=image} like any other unstorable photo, never a 500. */
    @Test
    void addImage_redirectsWithErrorImage_whenThePhotoBodyDoesNotDecode() throws Exception {
        long storedBefore = storedFiles();
        MockMultipartFile file = new MockMultipartFile("image", "corrupt.png", "image/png", ImageFixtures.corruptBodyPng());

        mockMvc.perform(multipart("/insects/battus-philenor/images").file(file)
                .requestAttr(CURRENT_NATURALIST, "patrick-way"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/insects/battus-philenor?error=image"));

        assertThat(storedFiles()).as("nothing is stored").isEqualTo(storedBefore);
    }

    /** {@code ?error=busy}, the answer to an add-photo the image store had no write permit for, tells the visitor to try again. */
    @Test
    void detail_rendersBusyErrorWhenFlagged() throws Exception {
        mockMvc.perform(get("/insects/battus-philenor?error=busy").requestAttr(CURRENT_NATURALIST, "patrick-way"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Too many photos are being saved right now")));
    }
}
