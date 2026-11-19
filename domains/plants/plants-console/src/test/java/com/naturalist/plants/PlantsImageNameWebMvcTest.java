package com.naturalist.plants;

import com.naturalist.data.NaturalistTestExtension;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * A photo or tile name with a directory part names no stored photo: the endpoint answers an uncached 404 and counts
 * the refusal itself, before the image store is asked (B1, terminate at the edge). A valid name the store does not
 * hold is a 404 too, counted as {@code found}: a broken link of ours or someone probing names.
 */
@WebMvcTest
class PlantsImageNameWebMvcTest {

    @RegisterExtension
    static final NaturalistTestExtension database = PlantsControllerTestConfig.DATABASE;

    @Autowired
    MockMvc mockMvc;

    SimpleMeterRegistry meters;

    @BeforeEach
    void addMeters() {
        meters = new SimpleMeterRegistry();
        Metrics.addRegistry(meters);
    }

    @AfterEach
    void removeMeters() {
        Metrics.removeRegistry(meters);
    }

    @Test
    void image_nameWithParentReference_is404NoStore_countedAtTheEdge() throws Exception {
        mockMvc.perform(get("/plants/images/{filename}", "..secret.jpg"))
                .andExpect(status().isNotFound())
                .andExpect(header().string("Cache-Control", "no-store"));

        assertStoppedAtEdge();
    }

    @Test
    void tile_nameWithBackslash_is404NoStore_countedAtTheEdge() throws Exception {
        mockMvc.perform(get("/plants/images/tile/{filename}", "sub\\b.jpg"))
                .andExpect(status().isNotFound())
                .andExpect(header().string("Cache-Control", "no-store"));

        assertStoppedAtEdge();
    }

    @Test
    void image_missing_is404NoStore_countedAsNotFound() throws Exception {
        mockMvc.perform(get("/plants/images/{filename}", "nope.jpg"))
                .andExpect(status().isNotFound())
                .andExpect(header().string("Cache-Control", "no-store"));

        var missing = violated("PlantsController.serve.found");
        assertThat(missing).as("missing count").isNotNull();
        assertThat(missing.count()).as("missing count").isEqualTo(1.0);
    }

    /** The endpoint counted the refusal once and the store's own name check never ran. */
    private void assertStoppedAtEdge() {
        var edge = violated("PlantsController.serve.filename");
        assertThat(edge).as("edge count").isNotNull();
        assertThat(edge.count()).as("edge count").isEqualTo(1.0);
        assertThat(violated("FilesystemImageStore.read.name")).as("the store was never asked").isNull();
    }

    private @Nullable Counter violated(String name) {
        return meters.find("naturalist.invariant.check").tag("name", name).tag("outcome", "violated").counter();
    }
}
