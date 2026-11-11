package com.naturalist.insects;

import com.naturalist.data.NaturalistTestExtension;
import com.naturalist.exception.InvariantViolationException;
import com.naturalist.naturalist.NaturalistName;
import com.naturalist.observation.GridCell;
import com.naturalist.observation.ImageStore;
import com.naturalist.observation.ImageUpload;
import com.naturalist.observation.OrganismImage;
import com.naturalist.observation.filesystem.FilesystemImageStore;
import com.naturalist.observation.test.ImageFixtures;
import com.naturalist.resilience.Resilience;
import com.naturalist.test.TestDirectories;
import com.naturalist.test.context.Fake;
import com.naturalist.test.context.Wired;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InsectPhotoCommandImplTest {

    private static final byte[] JPEG = {
            (byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0, 16, 'J', 'F', 'I', 'F', 0, 1, 1, 0, 0, 1};

    private static final byte[] HEIC = {
            0, 0, 0, 24, 'f', 't', 'y', 'p', 'h', 'e', 'i', 'c', 0, 0, 0, 0, 'm', 'i', 'f', '1'};

    @RegisterExtension
    NaturalistTestExtension nte = NaturalistTestExtension.create();

    Path imageRoot = newImageRoot();

    @Fake ImageStore store = new FilesystemImageStore(imageRoot, Resilience.noOp());
    @Wired InsectEntityCommands entityCommands;
    @Wired InsectCommand.PhotoCommand command;
    @Wired InsectQuery query;

    @Test
    void add_withNaturalist_insertsImageAndObservation() {
        var species = TestInsectsIdentifiers.InsectSpecies.BattusPhilenor.name;
        var naturalist = NaturalistName.of("test-naturalist");

        command.add(species, ImageUpload.of(JPEG), naturalist, "Found on pipevine", "Garden", null);

        var photo = query.images().forParentName(species).stream()
                .filter(img -> img.observationId() != null)
                .findFirst();
        assertThat(photo).isPresent();
        assertThat(store.read(InsectPhotoCommandImpl.SPACE, photo.get().resourceName())).isPresent();

        var observation = query.observations().getByName(photo.get().observationId());
        assertThat(observation).isPresent();
        assertThat(observation.get().observedBy()).isEqualTo(naturalist);
        assertThat(observation.get().subject()).isEqualTo(species);
        assertThat(observation.get().notes()).isEqualTo("Found on pipevine");
        assertThat(observation.get().location()).isEqualTo("Garden");
        assertThat(observation.get().identification()).isNull();
    }

    @Test
    void add_withoutNaturalist_insertsImageOnly() {
        var species = TestInsectsIdentifiers.InsectSpecies.BattusPhilenor.name;
        var before = query.images().forParentName(species).stream().count();

        command.add(species, ImageUpload.of(JPEG), null, null, null, null);

        var images = query.images().forParentName(species).stream().toList();
        assertThat(images).hasSize((int) before + 1);
        var added = images.stream()
                .filter(img -> store.read(InsectPhotoCommandImpl.SPACE, img.resourceName()).isPresent()
                        && img.observationId() == null)
                .findFirst();
        assertThat(added).isPresent();
    }

    @Test
    void add_genusRank_insertsImageAndObservationAtGenus() {
        var genus = TestInsectsIdentifiers.InsectGenus.Battus.name;
        var naturalist = NaturalistName.of("test-naturalist");

        command.add(genus, ImageUpload.of(JPEG), naturalist, "Unidentified swallowtail", null, null);

        var photo = query.images().forParentName(genus).stream()
                .filter(img -> img.observationId() != null)
                .findFirst();
        assertThat(photo).isPresent();
        assertThat(photo.get().parentName()).isEqualTo(genus);

        var observation = query.observations().getByName(photo.get().observationId());
        assertThat(observation).isPresent();
        assertThat(observation.get().subject()).isEqualTo(genus);
    }

    @Test
    void add_withNaturalist_blankNotesAndLocation_persistsAsNull() {
        var species = TestInsectsIdentifiers.InsectSpecies.BattusPhilenor.name;
        var naturalist = NaturalistName.of("test-naturalist");

        command.add(species, ImageUpload.of(JPEG), naturalist, "  ", "", null);

        var photo = query.images().forParentName(species).stream()
                .filter(img -> img.observationId() != null)
                .findFirst();
        assertThat(photo).isPresent();
        var observation = query.observations().getByName(photo.get().observationId());
        assertThat(observation).isPresent();
        assertThat(observation.get().notes()).isNull();
        assertThat(observation.get().location()).isNull();
    }

    @Test
    void add_invalidUpload_storesAndPersistsNothing() throws IOException {
        var species = TestInsectsIdentifiers.InsectSpecies.BattusPhilenor.name;
        var before = query.images().forParentName(species).stream().count();

        assertThatThrownBy(() -> command.add(species, ImageUpload.of(HEIC), null, null, null, null))
                .isInstanceOf(InvariantViolationException.class);

        assertThat(query.images().forParentName(species).stream().count()).isEqualTo(before);
        assertThat(filesUnder(imageRoot)).isEmpty();
    }

    @Test
    void add_persistFailure_deletesStoredFile() throws IOException {
        var species = TestInsectsIdentifiers.InsectSpecies.BattusPhilenor.name;
        var throwing = new InsectAddPhotoTransaction(entityCommands) {
            @Override
            protected void doExecute(PhotoAddition addition) {
                throw new IllegalStateException("boom");
            }
        };
        var failing = new InsectPhotoCommandImpl(store, throwing);

        assertThatThrownBy(() -> failing.add(species, ImageUpload.of(JPEG), null, null, null, null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("boom");

        assertThat(filesUnder(imageRoot)).isEmpty();
    }

    /** A JPEG that walks as segments and carries an Exif fix at Portland and an orientation. */
    private static ImageUpload portlandUpload() {
        return ImageUpload.of(ImageFixtures.jpeg(ImageFixtures.app0Jfif(),
                ImageFixtures.app1Exif(ByteOrder.BIG_ENDIAN, 6, 45.52, -122.68)));
    }

    /** Adds {@code upload} at Battus philenor as a signed-in naturalist and returns the image it added. */
    private OrganismImage<InsectImageId, InsectObservationId, InsectRankName> addAndFind(
            ImageUpload upload, @Nullable GridCell area) {
        var species = TestInsectsIdentifiers.InsectSpecies.BattusPhilenor.name;
        Set<InsectImageId> before = query.images().forParentName(species).stream()
                .map(OrganismImage::id).collect(Collectors.toSet());

        command.add(species, upload, NaturalistName.of("test-naturalist"), null, null, area);

        return query.images().forParentName(species).stream()
                .filter(img -> !before.contains(img.id()))
                .findFirst().orElseThrow();
    }

    @Test
    void add_persistsClaimedArea() {
        var claimed = GridCell.of(1000, -2000);
        var image = addAndFind(portlandUpload(), claimed);

        assertThat(query.observations().getByName(image.observationId()).orElseThrow().area()).isEqualTo(claimed);
    }

    @Test
    void add_nullArea_derivesFromExif() {
        var image = addAndFind(portlandUpload(), null);

        assertThat(query.observations().getByName(image.observationId()).orElseThrow().area())
                .isEqualTo(GridCell.of(1092, -2945));
    }

    @Test
    void add_storesStrippedBytes() {
        var image = addAndFind(portlandUpload(), null);

        var stored = store.read(InsectPhotoCommandImpl.SPACE, image.resourceName()).orElseThrow();
        assertThat(ImageUpload.of(stored.readAllBytes()).gps()).isEmpty();
        assertThat(stored.readAllBytes()).isEqualTo(portlandUpload().withoutMetadata().bytes());
    }

    /** A fresh image root per test, made before the {@code @Fake} store that writes under it. */
    private static Path newImageRoot() {
        try {
            return TestDirectories.create("insects-photo-command");
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static java.util.List<Path> filesUnder(Path root) throws IOException {
        try (Stream<Path> walk = Files.walk(root)) {
            return walk.filter(Files::isRegularFile).toList();
        }
    }
}
