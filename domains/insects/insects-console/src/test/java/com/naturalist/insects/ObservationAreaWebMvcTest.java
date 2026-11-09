package com.naturalist.insects;

import com.naturalist.data.NaturalistTestExtension;
import com.naturalist.naturalist.NaturalistName;
import com.naturalist.observation.GridCell;
import com.naturalist.observation.ImageUpload;
import com.naturalist.observation.OrganismObservation;
import com.naturalist.observation.test.ImageFixtures;
import com.naturalist.library.LibraryTestContext;
import com.naturalist.vision.ImageMetadata;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.io.IOException;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The controller's one job for an observation's area: turn the posted {@code areaLat}/{@code areaLon}
 * fields into a {@link GridCell} and hand it to the command, or refuse a cell it cannot read with
 * {@code ?error=area} before anything is stored. Which source wins -- the posted cell or the photo's
 * EXIF -- is the command's rule and is covered where the command is; the end-to-end cases here run
 * through the real wiring ({@link InsectsControllerTestConfig}) to show the pieces meet.
 */
@WebMvcTest
class ObservationAreaWebMvcTest {

    @RegisterExtension
    static final NaturalistTestExtension database = InsectsControllerTestConfig.DATABASE;

    private static final String CURRENT_NATURALIST = "naturalist.currentNaturalistName";

    private static final NaturalistName ME = NaturalistName.of("pat-way");

    private static final InsectSpeciesName SPECIES = InsectSpeciesName.of("battus-philenor");

    private static final GridCell PORTLAND_CELL = GridCell.of(1092, -2945);

    private static final byte[] PLAIN_JPEG = ImageFixtures.jpeg(ImageFixtures.app0Jfif());

    private static final byte[] PORTLAND_JPEG = ImageFixtures.jpeg(
            ImageFixtures.app0Jfif(),
            ImageFixtures.app1Exif(ByteOrder.BIG_ENDIAN, null, 45.52, -122.68));

    @Autowired
    MockMvc mockMvc;

    @Test
    void addImage_postedCell_reachesTheObservation() throws Exception {
        mockMvc.perform(multipart("/insects/battus-philenor/images")
                        .file(photo(PLAIN_JPEG))
                        .param("areaLat", "1092")
                        .param("areaLon", "-2945")
                        .requestAttr(CURRENT_NATURALIST, ME.value()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/insects/battus-philenor"));

        assertThat(newestObservation().area()).isEqualTo(PORTLAND_CELL);
    }

    @Test
    void addImage_noCellPosted_commandDerivesFromExif_andServedBytesHaveNoMetadata() throws Exception {
        Set<Path> before = storedFiles();

        mockMvc.perform(multipart("/insects/battus-philenor/images")
                        .file(photo(PORTLAND_JPEG))
                        .requestAttr(CURRENT_NATURALIST, ME.value()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/insects/battus-philenor"));

        assertThat(newestObservation().area()).isEqualTo(PORTLAND_CELL);

        Set<Path> added = storedFiles();
        added.removeAll(before);
        assertThat(added).hasSize(1);
        String name = added.iterator().next().getFileName().toString();
        byte[] served = mockMvc.perform(get("/insects/images/" + name))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsByteArray();
        assertThat(ImageUpload.of(served).isValid()).isTrue();
        assertThat(ImageUpload.of(served).gps()).isEmpty();
    }

    @Test
    void addImage_noGps_noCell_areaIsNull() throws Exception {
        mockMvc.perform(multipart("/insects/battus-philenor/images")
                        .file(photo(PLAIN_JPEG))
                        .requestAttr(CURRENT_NATURALIST, ME.value()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/insects/battus-philenor"));

        assertThat(newestObservation().area()).isNull();
    }

    @Test
    void addImage_outOfRangeCell_redirectsWithAreaError() throws Exception {
        assertRefusedWithAreaError("2160", "0");
    }

    @Test
    void addImage_nonNumericCell_redirectsWithAreaError() throws Exception {
        assertRefusedWithAreaError("abc", "1");
    }

    @Test
    void addImage_halfCell_redirectsWithAreaError() throws Exception {
        assertRefusedWithAreaError(null, "1");
    }

    @Test
    void identify_passesThePostedCellToTheCommand() throws Exception {
        var identification = new RecordingIdentification();

        standalone(commandOver(identification)).perform(multipart("/insects/identify")
                        .file(photo(PLAIN_JPEG))
                        .param("areaLat", "1092")
                        .param("areaLon", "-2945")
                        .requestAttr(CURRENT_NATURALIST, ME.value()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/insects/battus-philenor"));

        assertThat(identification.areas).containsExactly(PORTLAND_CELL);
    }

    @Test
    void identify_noCellPosted_handsTheCommandNull() throws Exception {
        var identification = new RecordingIdentification();

        standalone(commandOver(identification)).perform(multipart("/insects/identify")
                        .file(photo(PORTLAND_JPEG))
                        .requestAttr(CURRENT_NATURALIST, ME.value()))
                .andExpect(status().is3xxRedirection());

        // The controller never reads the photo's EXIF: with nothing posted it claims no area, and the
        // command derives one.
        assertThat(identification.calls).isEqualTo(1);
        assertThat(identification.areas).containsExactly((GridCell) null);
    }

    @Test
    void identify_unreadableCell_redirectsWithAreaError_andNeverReachesTheCommand() throws Exception {
        var identification = new RecordingIdentification();

        standalone(commandOver(identification)).perform(multipart("/insects/identify")
                        .file(photo(PLAIN_JPEG))
                        .param("areaLat", "0")
                        .param("areaLon", "4320")
                        .requestAttr(CURRENT_NATURALIST, ME.value()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/insects/identify?error=area"));

        assertThat(identification.calls).isZero();
    }

    @Test
    void detail_rendersAreaError() throws Exception {
        mockMvc.perform(get("/insects/battus-philenor?error=area").requestAttr(CURRENT_NATURALIST, ME.value()))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("The area could not be read; the photo was not added.")));
    }

    private void assertRefusedWithAreaError(String areaLat, String areaLon) throws Exception {
        Set<Path> filesBefore = storedFiles();
        int observationsBefore = observationsOfSpecies().size();

        var request = multipart("/insects/battus-philenor/images")
                .file(photo(PLAIN_JPEG))
                .requestAttr(CURRENT_NATURALIST, ME.value());
        if (areaLat != null) {
            request.param("areaLat", areaLat);
        }
        if (areaLon != null) {
            request.param("areaLon", areaLon);
        }
        mockMvc.perform(request)
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/insects/battus-philenor?error=area"));

        assertThat(storedFiles()).isEqualTo(filesBefore);
        assertThat(observationsOfSpecies()).hasSize(observationsBefore);
    }

    private static MockMultipartFile photo(byte[] bytes) {
        return new MockMultipartFile("image", "photo.jpg", "image/jpeg", bytes);
    }

    /** Records the area each identification was handed, and identifies the same species every time. */
    private static final class RecordingIdentification implements InsectCommand.IdentificationCommand {
        int calls;
        final List<GridCell> areas = new ArrayList<>();

        @Override
        public InsectRankName identify(ImageUpload upload, ImageMetadata metadata, NaturalistName naturalist,
                                       @Nullable String notes, @Nullable GridCell area) {
            calls++;
            areas.add(area);
            return SPECIES;
        }
    }

    /** An {@link InsectCommand} whose only live port is identification; any other port is a test bug. */
    private static InsectCommand commandOver(InsectCommand.IdentificationCommand identification) {
        return (InsectCommand) Proxy.newProxyInstance(
                InsectCommand.class.getClassLoader(), new Class<?>[]{InsectCommand.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("identification")) {
                        return identification;
                    }
                    throw new UnsupportedOperationException(method.getName());
                });
    }

    /** A controller over the slice's real queries and the given command, for the cases that inspect the command. */
    private static MockMvc standalone(InsectCommand command) {
        LibraryTestContext library = LibraryTestContext.create(InsectsControllerTestConfig.DATABASE);
        return MockMvcBuilders.standaloneSetup(new InsectsController(
                        InsectsControllerTestConfig.INSECTS.insectQuery(),
                        InsectsControllerTestConfig.INSECTS.insectViewQuery(),
                        command,
                        library.cladeQuery(),
                        library.glossaryTermQuery(),
                        InsectsControllerTestConfig.IMAGE_STORE))
                .build();
    }

    private static List<OrganismObservation<InsectObservationId, InsectRankName>> observationsOfSpecies() {
        return InsectsControllerTestConfig.INSECTS.insectQuery().observations().forNaturalist(ME).stream()
                .filter(o -> o.subject().equals(SPECIES))
                .toList();
    }

    private static OrganismObservation<InsectObservationId, InsectRankName> newestObservation() {
        return observationsOfSpecies().stream()
                .max(java.util.Comparator.comparing(OrganismObservation::observedOn))
                .orElseThrow();
    }

    private static Set<Path> storedFiles() throws IOException {
        try (Stream<Path> walk = Files.walk(InsectsControllerTestConfig.IMAGE_ROOT)) {
            return walk.filter(Files::isRegularFile).collect(Collectors.toCollection(java.util.HashSet::new));
        }
    }
}
