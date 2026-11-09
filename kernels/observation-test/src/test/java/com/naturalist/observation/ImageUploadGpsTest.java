package com.naturalist.observation;

import com.naturalist.observation.test.ImageFixtures;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Random;

import static com.naturalist.observation.test.ImageFixtures.app0Jfif;
import static com.naturalist.observation.test.ImageFixtures.app1Exif;
import static com.naturalist.observation.test.ImageFixtures.app1Xmp;
import static com.naturalist.observation.test.ImageFixtures.exif;
import static com.naturalist.observation.test.ImageFixtures.idat;
import static com.naturalist.observation.test.ImageFixtures.iend;
import static com.naturalist.observation.test.ImageFixtures.ihdr;
import static com.naturalist.observation.test.ImageFixtures.jpeg;
import static com.naturalist.observation.test.ImageFixtures.png;
import static com.naturalist.observation.test.ImageFixtures.tiff;
import static com.naturalist.observation.test.ImageFixtures.webp;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

/**
 * {@link ImageUpload#gps()} over real bytes: the fixtures build each container the way a camera or
 * an encoder lays it out, in both byte orders, and the reader must find the fix or come back empty
 * without throwing.
 */
class ImageUploadGpsTest {

    private final SimpleMeterRegistry meters = new SimpleMeterRegistry();

    private static final ImageUpload GPS_JPEG_AT_PORTLAND =
            ImageUpload.of(jpeg(app0Jfif(), app1Exif(ByteOrder.BIG_ENDIAN, null, 45.52, -122.68)));

    private static final ImageUpload PLAIN_JPEG = ImageUpload.of(jpeg(app0Jfif()));

    @BeforeEach
    void setUp() {
        Metrics.addRegistry(meters);
    }

    @AfterEach
    void tearDown() {
        Metrics.removeRegistry(meters);
    }

    @Test
    void jpeg_bigEndianExif_readsNorthWest() {
        var fix = ImageUpload.of(jpeg(app1Exif(ByteOrder.BIG_ENDIAN, null, 45.52, -122.68))).gps();
        assertThat(fix).isPresent();
        assertThat(fix.get().latitude()).isCloseTo(45.52, within(1e-6));
        assertThat(fix.get().longitude()).isCloseTo(-122.68, within(1e-6));
    }

    @Test
    void jpeg_littleEndianExif_readsSouthEast() {
        var fix = ImageUpload.of(jpeg(app1Exif(ByteOrder.LITTLE_ENDIAN, null, -33.87, 151.21))).gps();
        assertThat(fix).isPresent();
        assertThat(fix.get().latitude()).isCloseTo(-33.87, within(1e-6));
        assertThat(fix.get().longitude()).isCloseTo(151.21, within(1e-6));
    }

    @Test
    void jpeg_exifWithoutGpsIfd_isEmpty() {
        byte[] image = jpeg(app1Exif(ByteOrder.BIG_ENDIAN, 6, null, null));
        assertThat(ImageUpload.of(image).gps()).isEmpty();
        assertThat(ImageMetadataSegments.exifTiff(image).flatMap(ImageMetadataSegments::orientation)).contains(6);
        assertThat(totalViolations()).isZero();
    }

    @Test
    void jpeg_xmpOnly_isEmpty() {
        assertThat(ImageUpload.of(jpeg(app1Xmp())).gps()).isEmpty();
        assertThat(totalViolations()).isZero();
    }

    @Test
    void jpeg_truncatedApp1_isEmpty_andObserved() {
        byte[] whole = jpeg(app1Exif(ByteOrder.BIG_ENDIAN, null, 45.52, -122.68));
        byte[] truncated = Arrays.copyOf(whole, 40);
        assertThat(ImageUpload.of(truncated).gps()).isEmpty();
        assertThat(violations("ImageUpload.exifTiff.wellFormed")).isEqualTo(1);
    }

    @Test
    void jpeg_garbageTiff_isEmpty() {
        byte[] garbage = new byte[64];
        new Random(42).nextBytes(garbage);
        byte[] payload = concat("Exif\0\0".getBytes(StandardCharsets.US_ASCII), garbage);
        byte[] app1 = concat(new byte[]{(byte) 0xFF, (byte) 0xE1, 0, (byte) (payload.length + 2)}, payload);
        assertThat(ImageUpload.of(jpeg(app1)).gps()).isEmpty();
        assertThat(violations("ImageUpload.gps.wellFormed")).isEqualTo(1);
    }

    @Test
    void tinyJpegFromTheSuites_isEmpty() {
        byte[] tiny = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};
        assertThat(ImageUpload.of(tiny).gps()).isEmpty();
    }

    @Test
    void png_eXIfChunk_reads() {
        byte[] image = png(ihdr(), exif(tiff(ByteOrder.LITTLE_ENDIAN, 1, 45.52, -122.68)), idat(), iend());
        var fix = ImageUpload.of(image).gps();
        assertThat(fix).isPresent();
        assertThat(fix.get().latitude()).isCloseTo(45.52, within(1e-6));
        assertThat(fix.get().longitude()).isCloseTo(-122.68, within(1e-6));
    }

    @Test
    void webp_exifChunk_reads() {
        var fix = ImageUpload.of(webp(true, true)).gps();
        assertThat(fix).isPresent();
        assertThat(fix.get().latitude()).isCloseTo(ImageFixtures.PORTLAND.latitude(), within(1e-6));
        assertThat(fix.get().longitude()).isCloseTo(ImageFixtures.PORTLAND.longitude(), within(1e-6));
        assertThat(ImageUpload.of(webp(false, true)).gps()).isEmpty();
    }

    @Test
    void noMetadata_isEmpty() {
        assertThat(PLAIN_JPEG.gps()).isEmpty();
        assertThat(ImageUpload.of(png(ihdr(), idat(), iend())).gps()).isEmpty();
        assertThat(ImageUpload.of(webp(false, false)).gps()).isEmpty();
        assertThat(totalViolations()).isZero();
    }

    @Test
    void fixOffTheGlobe_isEmpty_andObserved() {
        assertThat(ImageUpload.of(jpeg(app1Exif(ByteOrder.BIG_ENDIAN, null, 95.0, -122.68))).gps()).isEmpty();
        assertThat(violations("ImageUpload.gps.fix.latitude")).isEqualTo(1);
    }

    @Test
    void zeroDenominator_isEmpty_andObserved() {
        byte[] tiff = tiff(ByteOrder.BIG_ENDIAN, null, 45.52, -122.68);
        // The latitude's degrees rational is 45/1; zero its denominator.
        int degrees = indexOf(tiff, new byte[]{0, 0, 0, 45, 0, 0, 0, 1});
        tiff[degrees + 7] = 0;
        assertThat(ImageUpload.of(png(ihdr(), exif(tiff), idat(), iend())).gps()).isEmpty();
        assertThat(violations("ImageUpload.gps.fix.latitude")).isEqualTo(1);
    }

    @Test
    void resolve_claimedWins() {
        assertThat(GridCell.resolve(GridCell.of(1000, -2000), GPS_JPEG_AT_PORTLAND))
                .isEqualTo(GridCell.of(1000, -2000));
    }

    @Test
    void resolve_nullClaim_readsExif() {
        assertThat(GridCell.resolve(null, GPS_JPEG_AT_PORTLAND)).isEqualTo(GridCell.of(1092, -2945));
    }

    @Test
    void resolve_nullClaim_noGps_isNull() {
        assertThat(GridCell.resolve(null, PLAIN_JPEG)).isNull();
    }

    private double violations(String name) {
        Counter counter = meters.find("naturalist.invariant.check").tag("outcome", "violated")
                .tag("name", name).counter();
        return counter == null ? 0 : counter.count();
    }

    private double totalViolations() {
        return meters.find("naturalist.invariant.check").tag("outcome", "violated").counters().stream()
                .mapToDouble(Counter::count).sum();
    }

    private static byte[] concat(byte[] a, byte[] b) {
        byte[] out = Arrays.copyOf(a, a.length + b.length);
        System.arraycopy(b, 0, out, a.length, b.length);
        return out;
    }

    private static int indexOf(byte[] haystack, byte[] needle) {
        for (int i = 0; i + needle.length <= haystack.length; i++) {
            if (Arrays.equals(haystack, i, i + needle.length, needle, 0, needle.length)) return i;
        }
        throw new AssertionError("pattern not in fixture");
    }
}
