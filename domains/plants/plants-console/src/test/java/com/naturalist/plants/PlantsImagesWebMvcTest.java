package com.naturalist.plants;

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

@WebMvcTest
class PlantsImagesWebMvcTest {
    @RegisterExtension
    static final NaturalistTestExtension database = PlantsControllerTestConfig.DATABASE;

    /** Minimal JPEG: FF D8 FF E0 magic header + padding. */
    private static final byte[] TINY_JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};

    @Autowired
    MockMvc mockMvc;

    @Test
    void image_missingIs404() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/plants/images/nope.jpg"))
                .andExpect(MockMvcResultMatchers.status().isNotFound());
    }

    @Test
    void image_servesStoredUpload() throws Exception {
        var name = PlantsControllerTestConfig.IMAGE_STORE.store("plants", ImageUpload.of(TINY_JPEG));
        mockMvc.perform(MockMvcRequestBuilders.get("/plants/images/" + name.value()))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().contentType("image/jpeg"))
                .andExpect(MockMvcResultMatchers.header().string("Cache-Control", Matchers.containsString("immutable")))
                .andExpect(MockMvcResultMatchers.content().bytes(TINY_JPEG));
    }
}
