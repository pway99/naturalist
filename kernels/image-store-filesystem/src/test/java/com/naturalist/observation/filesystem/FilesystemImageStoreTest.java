package com.naturalist.observation.filesystem;

import com.naturalist.data.FileName;
import com.naturalist.exception.InvariantViolationException;
import com.naturalist.observation.ImageUpload;
import com.naturalist.observation.StoredImage;
import com.naturalist.observation.test.ImageFixtures;
import com.naturalist.resilience.Resilience;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FilesystemImageStoreTest {

    private static final byte[] JPEG = {
            (byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 2, 3, 4};

    private static final byte[] HEIC = {
            0, 0, 0, 0x18, 0x66, 0x74, 0x79, 0x70, 0x68, 0x65, 0x69, 0x63, 0, 0, 0, 0};

    @TempDir
    Path root;

    FilesystemImageStore store;

    @BeforeEach
    void setUp() {
        store = new FilesystemImageStore(root, Resilience.noOp());
    }

    @Test
    void store_writesUnderSpaceWithSniffedExtension() {
        FileName name = store.store("insects", ImageUpload.of(JPEG));

        assertThat(name.value()).endsWith(".jpg");
        assertThat(Files.exists(root.resolve("insects").resolve(name.value()))).isTrue();
    }

    @Test
    void store_createsSpaceDirectoryWhenMissing() throws IOException {
        store.store("insects", ImageUpload.of(JPEG));
        Path space = root.resolve("insects");
        try (Stream<Path> files = Files.list(space)) {
            for (Path p : files.toList()) Files.delete(p);
        }
        Files.delete(space);

        FileName name = store.store("insects", ImageUpload.of(JPEG));

        assertThat(Files.exists(space.resolve(name.value()))).isTrue();
    }

    @Test
    void store_rejectsInvalidUpload() throws IOException {
        assertThatThrownBy(() -> store.store("insects", ImageUpload.of(HEIC)))
                .isInstanceOf(InvariantViolationException.class);

        Path space = root.resolve("insects");
        if (Files.exists(space)) {
            try (Stream<Path> files = Files.list(space)) {
                assertThat(files.toList()).isEmpty();
            }
        }
    }

    @Test
    void read_roundTripsStoredBytesAndMediaType() {
        FileName name = store.store("insects", ImageUpload.of(JPEG));

        Optional<StoredImage> read = store.read("insects", name);

        assertThat(read).isPresent();
        assertThat(read.get().bytes()).isEqualTo(JPEG);
        assertThat(read.get().mediaType()).isEqualTo("image/jpeg");
    }

    @Test
    void store_stripsMetadata_readReturnsNone() {
        byte[] withGps = ImageFixtures.jpeg(ImageFixtures.app0Jfif(),
                ImageFixtures.app1Exif(ByteOrder.BIG_ENDIAN, 6, 45.5, -122.7));
        FileName name = store.store("insects", ImageUpload.of(withGps));

        StoredImage stored = store.read("insects", name).orElseThrow();

        ImageUpload upload = ImageUpload.of(stored.bytes());
        assertThat(upload.gps()).isEmpty();
        assertThat(upload.withoutMetadata()).isEqualTo(upload);
    }

    @Test
    void store_refusesAnUploadThatStripsToANonImage() throws IOException {
        byte[] exif = ImageFixtures.app1Exif(ByteOrder.BIG_ENDIAN, null, 45.5, -122.7);
        byte[] bytes = new byte[2 + exif.length + 4];
        bytes[0] = (byte) 0xFF;
        bytes[1] = (byte) 0xD8;
        System.arraycopy(exif, 0, bytes, 2, exif.length);
        System.arraycopy(new byte[]{0x12, 0x34, 0x56, 0x78}, 0, bytes, 2 + exif.length, 4);
        ImageUpload upload = ImageUpload.of(bytes);
        assertThat(upload.isValid()).isTrue();

        assertThatThrownBy(() -> store.store("insects", upload)).isInstanceOf(InvariantViolationException.class);

        Path space = root.resolve("insects");
        if (Files.exists(space)) {
            try (Stream<Path> files = Files.list(space)) {
                assertThat(files.toList()).isEmpty();
            }
        }
    }

    @Test
    void read_missingIsEmpty() {
        assertThat(store.read("insects", FileName.of("nope.jpg"))).isEmpty();
    }

    @Test
    void read_rejectsPathTraversal() throws IOException {
        Files.writeString(root.resolve("secret.jpg"), "secret");
        Files.createDirectories(root.resolve("insects/sub"));
        Files.write(root.resolve("insects/sub/b.jpg"), JPEG);

        assertThat(store.read("insects", FileName.of("../../etc/passwd"))).isEmpty();
        assertThat(store.read("insects", FileName.of("../secret.jpg"))).isEmpty();
        assertThat(store.read("insects", FileName.of("sub/b.jpg"))).isEmpty();
        assertThat(store.read("insects", FileName.of("sub\\b.jpg"))).isEmpty();
    }

    @Test
    void read_fallsBackToClasspath() {
        Optional<StoredImage> read = store.read("testspace", FileName.of("seed.jpg"));

        assertThat(read).isPresent();
        assertThat(read.get().mediaType()).isEqualTo("image/jpeg");
    }

    @Test
    void read_unknownExtensionIsOctetStream() throws IOException {
        Files.createDirectories(root.resolve("insects"));
        Files.write(root.resolve("insects/blob.bin"), new byte[]{1, 2, 3});

        Optional<StoredImage> read = store.read("insects", FileName.of("blob.bin"));

        assertThat(read).isPresent();
        assertThat(read.get().mediaType()).isEqualTo("application/octet-stream");
    }

    @Test
    void read_sniffsMediaTypeFromBytes() throws IOException {
        Files.createDirectories(root.resolve("insects"));
        Files.write(root.resolve("insects/mislabelled.png"), JPEG);

        Optional<StoredImage> read = store.read("insects", FileName.of("mislabelled.png"));

        assertThat(read).isPresent();
        assertThat(read.get().mediaType()).isEqualTo("image/jpeg");
    }

    @Test
    void delete_removesStoredFile() {
        FileName name = store.store("insects", ImageUpload.of(JPEG));

        store.delete("insects", name);

        assertThat(Files.exists(root.resolve("insects").resolve(name.value()))).isFalse();
        assertThat(store.read("insects", name)).isEmpty();
    }

    @Test
    void delete_missingIsNoOp() {
        assertThatCode(() -> store.delete("insects", FileName.of("nope.jpg"))).doesNotThrowAnyException();
    }

    @Test
    void delete_rejectsPathTraversal() throws IOException {
        Path secret = root.resolve("secret.jpg");
        Files.writeString(secret, "secret");

        store.delete("insects", FileName.of("../secret.jpg"));

        assertThat(Files.exists(secret)).isTrue();
    }
}
