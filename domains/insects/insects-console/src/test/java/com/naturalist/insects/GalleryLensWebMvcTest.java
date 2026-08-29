package com.naturalist.insects;

import com.naturalist.data.NaturalistTestExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * On a species detail page, the collection lens hides shared catalog photos and
 * shows an empty-collection note when the signed-in naturalist has no photos of
 * their own for that species. A DB-free, scan-free, security-free
 * {@code @WebMvcTest} slice ({@link InsectsControllerTestConfig}); {@code patrick-way}
 * has observed battus-philenor but owns none of its photos, so the shared
 * {@code pipevine-swallow-tail.HEIC} is his only catalog image.
 */
@WebMvcTest
class GalleryLensWebMvcTest {

    @RegisterExtension
    static final NaturalistTestExtension database = InsectsControllerTestConfig.DATABASE;

    private static final String CURRENT_NATURALIST = "naturalist.currentNaturalistName";

    @Autowired
    MockMvc mockMvc;

    @Test
    void lensOff_showsSharedCatalogPhoto() throws Exception {
        mockMvc.perform(get("/insects/battus-philenor").requestAttr(CURRENT_NATURALIST, "patrick-way"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("pipevine-swallow-tail.HEIC")));
    }

    @Test
    void lensOn_hidesSharedPhoto_andShowsEmptyNote() throws Exception {
        mockMvc.perform(get("/insects/battus-philenor")
                        .sessionAttr("insects.collectionLens", true)
                        .requestAttr(CURRENT_NATURALIST, "patrick-way"))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("pipevine-swallow-tail.HEIC"))))
                .andExpect(content().string(containsString("No photos of yours here yet")));
    }
}
