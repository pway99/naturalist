package com.naturalist.insects;

import com.naturalist.data.NaturalistTestExtension;
import com.naturalist.naturalist.NaturalistName;
import com.naturalist.observation.OrganismObservation;
import com.naturalist.observation.test.ImageFixtures;
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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The notes limit belongs to {@link OrganismObservation}. Each form's endpoint (photo, sighting, notes
 * edit, identify) stops an over-long note itself: it counts the refusal under its own name and answers
 * {@code ?error=notes} without calling the command, so the command's own notes check never counts it and
 * nothing is written. The photo endpoints check a claimed area the same way, before the image bytes are
 * read: {@code ?error=area}, counted once at the edge, the upload never read and the command never called.
 */
@WebMvcTest
class ObservationNotesWebMvcTest {

    @RegisterExtension
    static final NaturalistTestExtension database = InsectsControllerTestConfig.DATABASE;

    private static final String CURRENT_NATURALIST = "naturalist.currentNaturalistName";

    private static final NaturalistName ME = NaturalistName.of("pat-way");

    private static final InsectSpeciesName SPECIES = InsectSpeciesName.of("battus-philenor");

    private static final String TOO_LONG = "a".repeat(OrganismObservation.MAX_NOTES_LENGTH + 1);

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

    /** The endpoint counted the refusal once and the command's notes check never ran. */
    private void assertStoppedAtEdge(String endpoint, String commandCheck) {
        Counter edge = violated("InsectsController." + endpoint + ".notes");
        assertThat(edge).as("edge count").isNotNull();
        assertThat(edge.count()).as("edge count").isEqualTo(1.0);
        assertThat(violated(commandCheck)).as("the command was never called").isNull();
    }

    private @Nullable Counter violated(String name) {
        return meters.find("naturalist.invariant.check").tag("name", name).tag("outcome", "violated").counter();
    }

    @Test
    void addImage_notesOverMaxLength_redirectsWithNotesError_andStoresNothing() throws Exception {
        Set<Path> filesBefore = storedFiles();
        int observationsBefore = observationsOfSpecies();

        mockMvc.perform(multipart("/insects/battus-philenor/images")
                        .file(photo())
                        .param("notes", TOO_LONG)
                        .requestAttr(CURRENT_NATURALIST, ME.value()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/insects/battus-philenor?error=notes"));

        assertStoppedAtEdge("addImage", "InsectPhotoCommandImpl.add.notes");
        assertThat(storedFiles()).isEqualTo(filesBefore);
        assertThat(observationsOfSpecies()).isEqualTo(observationsBefore);
    }

    @Test
    void addImage_notesAtMaxLength_isAdded() throws Exception {
        int observationsBefore = observationsOfSpecies();

        mockMvc.perform(multipart("/insects/battus-philenor/images")
                        .file(photo())
                        .param("notes", "a".repeat(OrganismObservation.MAX_NOTES_LENGTH))
                        .requestAttr(CURRENT_NATURALIST, ME.value()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/insects/battus-philenor"));

        assertThat(observationsOfSpecies()).isEqualTo(observationsBefore + 1);
    }

    @Test
    void detail_rendersNotesError() throws Exception {
        mockMvc.perform(get("/insects/battus-philenor?error=notes").requestAttr(CURRENT_NATURALIST, ME.value()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(
                        "Notes can be at most 2000 characters; nothing was saved.")));
    }

    @Test
    void observe_notesOverMaxLength_redirectsWithNotesError_andRecordsNothing() throws Exception {
        int observationsBefore = observationsOfSpecies();

        mockMvc.perform(post("/insects/battus-philenor/observe")
                        .param("notes", TOO_LONG)
                        .requestAttr(CURRENT_NATURALIST, ME.value()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/insects/battus-philenor?error=notes"));

        assertStoppedAtEdge("observe", "InsectSightingCommandImpl.record.notes");
        assertThat(observationsOfSpecies()).isEqualTo(observationsBefore);
    }

    @Test
    void updateNotes_notesOverMaxLength_redirectsWithNotesError_andLeavesNotesUnchanged() throws Exception {
        InsectObservationId id = InsectsControllerTestConfig.INSECTS.insectCommand().sightings().record(SPECIES, ME, "old");

        mockMvc.perform(post("/insects/battus-philenor/notes")
                        .param("observationId", id.value().toString())
                        .param("notes", TOO_LONG)
                        .requestAttr(CURRENT_NATURALIST, ME.value()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/insects/battus-philenor?error=notes"));

        assertStoppedAtEdge("updateNotes", "InsectSightingCommandImpl.updateNotes.notes");
        assertThat(InsectsControllerTestConfig.INSECTS.insectQuery().observations().getByName(id)
                .orElseThrow().notes()).isEqualTo("old");
    }

    @Test
    void updateNotes_fromFamilyPage_redirectsBackWithNotesError_andThePageRenders() throws Exception {
        InsectObservationId id = InsectsControllerTestConfig.INSECTS.insectCommand().sightings().record(SPECIES, ME, "old");

        mockMvc.perform(post("/insects/battus-philenor/notes")
                        .param("observationId", id.value().toString())
                        .param("notes", TOO_LONG)
                        .param("returnPath", "/insects/families/papilionidae")
                        .requestAttr(CURRENT_NATURALIST, ME.value()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/insects/families/papilionidae?error=notes"));

        mockMvc.perform(get("/insects/families/papilionidae?error=notes").requestAttr(CURRENT_NATURALIST, ME.value()))
                .andExpect(status().isOk());
    }

    @Test
    void identify_notesOverMaxLength_redirectsWithNotesError_andStoresNothing() throws Exception {
        Set<Path> filesBefore = storedFiles();

        mockMvc.perform(multipart("/insects/identify")
                        .file(photo())
                        .param("notes", TOO_LONG)
                        .requestAttr(CURRENT_NATURALIST, ME.value()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/insects/identify?error=notes"));

        assertStoppedAtEdge("identify", "InsectIdentificationCommandImpl.identify.notes");
        assertThat(storedFiles()).isEqualTo(filesBefore);
    }

    @Test
    void addImage_areaOffTheLattice_redirectsWithAreaError_beforeReadingTheImage() throws Exception {
        Set<Path> filesBefore = storedFiles();
        int observationsBefore = observationsOfSpecies();
        ObservationNotesWebMvcTest.CountingPhoto upload = new CountingPhoto();

        mockMvc.perform(multipart("/insects/battus-philenor/images")
                        .file(upload)
                        .param("areaLat", "not-a-cell")
                        .param("areaLon", "0")
                        .requestAttr(CURRENT_NATURALIST, ME.value()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/insects/battus-philenor?error=area"));

        assertAreaStoppedAtEdge("addImage", "InsectPhotoCommandImpl.add", upload);
        assertThat(storedFiles()).isEqualTo(filesBefore);
        assertThat(observationsOfSpecies()).isEqualTo(observationsBefore);
    }

    @Test
    void identify_areaOffTheLattice_redirectsWithAreaError_beforeReadingTheImage() throws Exception {
        Set<Path> filesBefore = storedFiles();
        ObservationNotesWebMvcTest.CountingPhoto upload = new CountingPhoto();

        mockMvc.perform(multipart("/insects/identify")
                        .file(upload)
                        .param("areaLat", "not-a-cell")
                        .param("areaLon", "0")
                        .requestAttr(CURRENT_NATURALIST, ME.value()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/insects/identify?error=area"));

        assertAreaStoppedAtEdge("identify", "InsectIdentificationCommandImpl.identify", upload);
        assertThat(storedFiles()).isEqualTo(filesBefore);
    }

    /** The endpoint counted the area refusal once, never read the upload, and the command never checked anything. */
    private void assertAreaStoppedAtEdge(String endpoint, String command, CountingPhoto upload) {
        assertThat(violatedCount("InsectsController." + endpoint + ".area")).as("edge count").isEqualTo(1.0);
        assertThat(upload.bytesRead.get()).as("the image bytes were never read").isZero();
        // Meters other tests registered reach this registry at zero, so "never called" is nothing counted.
        assertThat(meters.find("naturalist.invariant.check").counters().stream()
                .filter(counter -> String.valueOf(counter.getId().getTag("name")).startsWith(command + "."))
                .mapToDouble(Counter::count)
                .sum())
                .as("the command was never called")
                .isZero();
    }

    /** Every violated check under {@code prefix}, summed: a value object's refusal is tagged with the rule it broke. */
    private double violatedCount(String prefix) {
        return meters.find("naturalist.invariant.check").tag("outcome", "violated").counters().stream()
                .filter(counter -> String.valueOf(counter.getId().getTag("name")).startsWith(prefix))
                .mapToDouble(Counter::count)
                .sum();
    }

    /** A photo that counts how often its bytes are read. */
    private static final class CountingPhoto extends MockMultipartFile {
        final AtomicInteger bytesRead = new AtomicInteger();

        CountingPhoto() {
            super("image", "photo.jpg", "image/jpeg", ImageFixtures.decodableJpeg());
        }

        @Override
        public byte[] getBytes() throws IOException {
            bytesRead.incrementAndGet();
            return super.getBytes();
        }
    }

    private static MockMultipartFile photo() {
        return new MockMultipartFile("image", "photo.jpg", "image/jpeg", ImageFixtures.decodableJpeg());
    }

    private static int observationsOfSpecies() {
        return (int) InsectsControllerTestConfig.INSECTS.insectQuery().observations().forNaturalist(ME).stream()
                .filter(o -> o.subject().equals(SPECIES))
                .count();
    }

    /** Every regular file in the store, photos and tiles alike, so a leaked tile is seen. */
    private static Set<Path> storedFiles() throws IOException {
        try (Stream<Path> walk = Files.walk(InsectsControllerTestConfig.IMAGE_ROOT)) {
            return walk.filter(Files::isRegularFile).collect(Collectors.toCollection(java.util.HashSet::new));
        }
    }
}
