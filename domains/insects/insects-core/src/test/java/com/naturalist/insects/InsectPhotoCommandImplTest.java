package com.naturalist.insects;

import com.naturalist.data.NaturalistTestExtension;
import com.naturalist.exception.InvariantViolationException;
import com.naturalist.naturalist.NaturalistName;
import com.naturalist.observation.ImageStore;
import com.naturalist.observation.ImageUpload;
import com.naturalist.observation.filesystem.FilesystemImageStore;
import com.naturalist.resilience.Resilience;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
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

    @TempDir
    Path imageRoot;

    ImageStore store;
    InsectsTestContextInternal context;
    InsectCommand.PhotoCommand command;
    InsectQuery query;

    @BeforeEach
    void setUp() {
        store = new FilesystemImageStore(imageRoot, Resilience.noOp());
        context = InsectsTestContextInternal.create(nte, store);
        command = context.insectCommand().photos();
        query = context.insectQuery();
    }

    @Test
    void add_withNaturalist_insertsImageAndObservation() {
        var species = TestInsectsIdentifiers.InsectSpecies.BattusPhilenor.name;
        var naturalist = NaturalistName.of("test-naturalist");

        command.add(species, ImageUpload.of(JPEG), naturalist, "Found on pipevine", "Garden");

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

        command.add(species, ImageUpload.of(JPEG), null, null, null);

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

        command.add(genus, ImageUpload.of(JPEG), naturalist, "Unidentified swallowtail", null);

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

        command.add(species, ImageUpload.of(JPEG), naturalist, "  ", "");

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

        assertThatThrownBy(() -> command.add(species, ImageUpload.of(HEIC), null, null, null))
                .isInstanceOf(InvariantViolationException.class);

        assertThat(query.images().forParentName(species).stream().count()).isEqualTo(before);
        assertThat(filesUnder(imageRoot)).isEmpty();
    }

    @Test
    void add_persistFailure_deletesStoredFile() throws IOException {
        var species = TestInsectsIdentifiers.InsectSpecies.BattusPhilenor.name;
        var throwing = new InsectAddPhotoTransaction(context.entityCommands()) {
            @Override
            protected void doExecute(PhotoAddition addition) {
                throw new IllegalStateException("boom");
            }
        };
        var failing = new InsectPhotoCommandImpl(store, throwing);

        assertThatThrownBy(() -> failing.add(species, ImageUpload.of(JPEG), null, null, null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("boom");

        assertThat(filesUnder(imageRoot)).isEmpty();
    }

    private static java.util.List<Path> filesUnder(Path root) throws IOException {
        try (Stream<Path> walk = Files.walk(root)) {
            return walk.filter(Files::isRegularFile).toList();
        }
    }
}
