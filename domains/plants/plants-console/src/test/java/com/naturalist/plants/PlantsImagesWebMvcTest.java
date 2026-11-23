package com.naturalist.plants;

import com.naturalist.observation.test.ImageFixtures;
import com.naturalist.data.FileName;
import com.naturalist.data.NaturalistTestExtension;
import com.naturalist.observation.ImageUpload;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import javax.imageio.ImageIO;
import org.assertj.core.api.Assertions;

@WebMvcTest
class PlantsImagesWebMvcTest {
    @RegisterExtension
    static final NaturalistTestExtension database = PlantsControllerTestConfig.DATABASE;

    /** A small JPEG the store can decode. */
    private static final byte[] TINY_JPEG = ImageFixtures.decodableJpeg();

    @Autowired
    MockMvc mockMvc;

    @Test
    void image_missingIs404() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/plants/images/nope.jpg"))
                .andExpect(MockMvcResultMatchers.status().isNotFound())
                .andExpect(MockMvcResultMatchers.header().string("Cache-Control", "no-store"));
    }

    @Test
    void image_servesStoredUpload() throws Exception {
        FileName name = PlantsControllerTestConfig.IMAGE_STORE.store("plants", ImageUpload.of(TINY_JPEG));
        mockMvc.perform(MockMvcRequestBuilders.get("/plants/images/" + name.value()))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().contentType("image/jpeg"))
                .andExpect(MockMvcResultMatchers.header().string("Cache-Control", Matchers.containsString("immutable")))
                .andExpect(MockMvcResultMatchers.content().bytes(TINY_JPEG));
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
        FileName name = PlantsControllerTestConfig.IMAGE_STORE.store("plants", ImageUpload.of(largeJpeg()));
        byte[] body = mockMvc.perform(MockMvcRequestBuilders.get("/plants/images/tile/" + name.value()))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().contentType("image/jpeg"))
                .andExpect(MockMvcResultMatchers.header().string("Cache-Control", Matchers.containsString("immutable")))
                .andReturn().getResponse().getContentAsByteArray();
        BufferedImage tile = ImageIO.read(new ByteArrayInputStream(body));
        Assertions.assertThat(tile.getWidth()).isEqualTo(640);
        Assertions.assertThat(tile.getHeight()).isEqualTo(480);
    }

    @Test
    void tile_missingIs404NoStore() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/plants/images/tile/nope.jpg"))
                .andExpect(MockMvcResultMatchers.status().isNotFound())
                .andExpect(MockMvcResultMatchers.header().string("Cache-Control", "no-store"));
    }

    @Test
    void plantPage_showsTilesLinkedToPhotos() throws Exception {
        String photo = "IMG_5905338E-63EC-495D-A63A-CA79AA9C066E.JPEG";
        mockMvc.perform(MockMvcRequestBuilders.get("/plants/aristolochia-californica"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().string(Matchers.containsString("src=\"/plants/images/tile/" + photo + "\"")))
                .andExpect(MockMvcResultMatchers.content().string(Matchers.containsString("/images/photo-not-found.svg")))
                .andExpect(MockMvcResultMatchers.content().string(Matchers.containsString("data-photo-view")))
                .andExpect(MockMvcResultMatchers.content().string(Matchers.containsString("href=\"/plants/images/" + photo + "\"")))
                .andExpect(MockMvcResultMatchers.content().string(Matchers.not(Matchers.containsString("src=\"/plants/images/" + photo + "\""))));
    }
}
