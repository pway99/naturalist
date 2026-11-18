package com.naturalist.insects;

import com.naturalist.data.FileName;
import com.naturalist.data.NaturalistTestExtension;
import com.naturalist.exception.InvariantViolationException;
import com.naturalist.naturalist.NaturalistName;
import com.naturalist.observation.GridCell;
import com.naturalist.observation.ImageStore;
import com.naturalist.observation.ImageUpload;
import com.naturalist.observation.ImageVariant;
import com.naturalist.observation.OrganismImage;
import com.naturalist.observation.OrganismObservation;
import com.naturalist.observation.StoredImage;
import com.naturalist.observation.filesystem.FilesystemImageStore;
import com.naturalist.observation.test.ImageFixtures;
import com.naturalist.resilience.Resilience;
import com.naturalist.test.TestDirectories;
import com.naturalist.test.context.Fake;
import com.naturalist.test.context.Wired;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
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

    private static final byte[] JPEG = ImageFixtures.decodableJpeg();

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
    void add_notesAtMaxLength_isAccepted() {
        var notes = "a".repeat(OrganismObservation.MAX_NOTES_LENGTH);

        var image = addAndFind(ImageUpload.of(JPEG), null, notes);

        assertThat(query.observations().getByName(image.observationId()).orElseThrow().notes()).isEqualTo(notes);
    }

    @Test
    void add_notesOverMaxLength_isRefusedAndStoresNothing() throws IOException {
        var species = TestInsectsIdentifiers.InsectSpecies.BattusPhilenor.name;
        var before = query.images().forParentName(species).stream().count();
        var notes = "a".repeat(OrganismObservation.MAX_NOTES_LENGTH + 1);

        assertThatThrownBy(() -> command.add(species, ImageUpload.of(JPEG),
                NaturalistName.of("test-naturalist"), notes, null, null))
                .isInstanceOf(InvariantViolationException.class)
                .hasMessageContaining("notes");

        assertThat(query.images().forParentName(species).stream().count()).isEqualTo(before);
        assertThat(filesUnder(imageRoot)).isEmpty();
    }

    @Test
    void add_invalidPhotoAddition_isRefusedAndDeletesStoredFile() throws IOException {
        var species = TestInsectsIdentifiers.InsectSpecies.BattusPhilenor.name;
        var before = query.images().forParentName(species).stream().count();
        // Stores the file for real but hands back a blank name, so the image -- and with it the
        // PhotoAddition -- is invalid after the store has written; delete of that name removes the real file.
        var realNames = new java.util.ArrayList<FileName>();
        ImageStore blankNaming = new ImageStore() {
            @Override public FileName store(String space, ImageUpload upload) {
                realNames.add(store.store(space, upload));
                return FileName.of(" ");
            }
            @Override public java.util.Optional<StoredImage> read(String space, FileName name, ImageVariant variant) {
                return store.read(space, name, variant);
            }
            @Override public void requireStorable(ImageUpload upload) {
                store.requireStorable(upload);
            }
            @Override public void delete(String space, FileName name) {
                realNames.forEach(real -> store.delete(space, real));
            }
        };
        var refusing = new InsectPhotoCommandImpl(blankNaming, new InsectAddPhotoTransaction(entityCommands));

        assertThatThrownBy(() -> refusing.add(species, ImageUpload.of(JPEG), null, null, null, null))
                .isInstanceOf(InvariantViolationException.class)
                .hasMessageContaining("photoAddition");

        assertThat(realNames).hasSize(1);
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

    @Test
    void add_persistFailure_whenDeleteAlsoFails_countsTheFailedCleanup() {
        var species = TestInsectsIdentifiers.InsectSpecies.BattusPhilenor.name;
        var throwing = new InsectAddPhotoTransaction(entityCommands) {
            @Override
            protected void doExecute(PhotoAddition addition) {
                throw new IllegalStateException("boom");
            }
        };
        ImageStore undeletable = new ImageStore() {
            @Override public FileName store(String space, ImageUpload upload) {
                return store.store(space, upload);
            }
            @Override public java.util.Optional<StoredImage> read(String space, FileName name, ImageVariant variant) {
                return store.read(space, name, variant);
            }
            @Override public void requireStorable(ImageUpload upload) {
                store.requireStorable(upload);
            }
            @Override public void delete(String space, FileName name) {
                throw new UncheckedIOException(new IOException("disk said no"));
            }
        };
        var failing = new InsectPhotoCommandImpl(undeletable, throwing);
        var meters = new SimpleMeterRegistry();
        Metrics.addRegistry(meters);
        try {
            assertThatThrownBy(() -> failing.add(species, ImageUpload.of(JPEG), null, null, null, null))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessage("boom")
                    .satisfies(e -> assertThat(e.getSuppressed()).hasSize(1));

            var counter = meters.find("naturalist.invariant.check")
                    .tag("name", "InsectPhotoCommandImpl.add.storedDiscarded")
                    .tag("outcome", "violated")
                    .counter();
            assertThat(counter).isNotNull();
            assertThat(counter.count()).isEqualTo(1.0);
        } finally {
            Metrics.removeRegistry(meters);
        }
    }

    /** A decodable JPEG that carries an Exif fix at Portland and an orientation. */
    private static ImageUpload portlandUpload() {
        return ImageUpload.of(ImageFixtures.decodableJpeg(
                ImageFixtures.app1Exif(ByteOrder.BIG_ENDIAN, 6, 45.52, -122.68)));
    }

    /** Adds {@code upload} at Battus philenor as a signed-in naturalist and returns the image it added. */
    private OrganismImage<InsectImageId, InsectObservationId, InsectRankName> addAndFind(
            ImageUpload upload, @Nullable GridCell area) {
        return addAndFind(upload, area, null);
    }

    /** As {@link #addAndFind(ImageUpload, GridCell)}, with {@code notes} on the observation. */
    private OrganismImage<InsectImageId, InsectObservationId, InsectRankName> addAndFind(
            ImageUpload upload, @Nullable GridCell area, @Nullable String notes) {
        var species = TestInsectsIdentifiers.InsectSpecies.BattusPhilenor.name;
        Set<InsectImageId> before = query.images().forParentName(species).stream()
                .map(OrganismImage::id).collect(Collectors.toSet());

        command.add(species, upload, NaturalistName.of("test-naturalist"), notes, null, area);

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
