package com.naturalist.observation;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.naturalist.observation.test.ImageFixtures.app0Jfif;
import static com.naturalist.observation.test.ImageFixtures.app13Iptc;
import static com.naturalist.observation.test.ImageFixtures.app14Adobe;
import static com.naturalist.observation.test.ImageFixtures.app1Exif;
import static com.naturalist.observation.test.ImageFixtures.app1Xmp;
import static com.naturalist.observation.test.ImageFixtures.app2Icc;
import static com.naturalist.observation.test.ImageFixtures.chunk;
import static com.naturalist.observation.test.ImageFixtures.com;
import static com.naturalist.observation.test.ImageFixtures.exif;
import static com.naturalist.observation.test.ImageFixtures.idat;
import static com.naturalist.observation.test.ImageFixtures.iend;
import static com.naturalist.observation.test.ImageFixtures.ihdr;
import static com.naturalist.observation.test.ImageFixtures.jpeg;
import static com.naturalist.observation.test.ImageFixtures.png;
import static com.naturalist.observation.test.ImageFixtures.segment;
import static com.naturalist.observation.test.ImageFixtures.text;
import static com.naturalist.observation.test.ImageFixtures.tiff;
import static com.naturalist.observation.test.ImageFixtures.webp;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link ImageUpload#withoutMetadata()} over real bytes: each container loses the blocks that can
 * carry a place, a person or a caption, keeps everything a decoder needs byte for byte, and keeps
 * a JPEG's orientation as the one Exif tag left. The tests read the output back as segments,
 * chunks and RIFF chunks of their own rather than trusting the stripper's walk.
 */
class ImageUploadStripTest {

    private static final int APP0 = 0xE0;

    private static final int APP2 = 0xE2;

    private static final int APP14 = 0xEE;

    private static final byte[] SOI = {(byte) 0xFF, (byte) 0xD8};

    private static final byte[] GARBAGE = {0x12, 0x34, 0x56, 0x78, (byte) 0x9A, (byte) 0xBC, (byte) 0xDE, (byte) 0xF0};

    /** Three {@code FF} fill bytes; before a marker's own {@code FF} they make a run of four. */
    private static final byte[] FILL = {(byte) 0xFF, (byte) 0xFF, (byte) 0xFF};

    private static final byte[] EXIF_HEADER = "Exif\0\0".getBytes(StandardCharsets.US_ASCII);

    private static final byte[] TINY_JPEG =
            {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};

    private final SimpleMeterRegistry meters = new SimpleMeterRegistry();

    @BeforeEach
    void setUp() {
        Metrics.addRegistry(meters);
    }

    @AfterEach
    void tearDown() {
        Metrics.removeRegistry(meters);
    }

    // ---- JPEG ---------------------------------------------------------------------------------

    @Test
    void jpeg_dropsExifXmpIptcAndComments_keepsJfifIccAdobe() {
        byte[] in = jpeg(app0Jfif(), app1Exif(ByteOrder.BIG_ENDIAN, null, 45.5, -122.7), app1Xmp(), app2Icc(),
                app13Iptc(), app14Adobe(), com("x"));

        ImageUpload out = ImageUpload.of(in).withoutMetadata();

        assertThat(markers(out.bytes())).containsExactly(APP0, APP2, APP14);
        assertThat(segments(out.bytes())).containsExactly(app0Jfif(), app2Icc(), app14Adobe());
        assertThat(out.gps()).isEmpty();
        assertThat(fromStartOfScan(out.bytes())).isEqualTo(fromStartOfScan(in));
        assertThat(totalViolations()).isZero();
    }

    @Test
    void jpeg_dropsEveryUnlistedAppSegment_keepsJfifIccAdobe() {
        byte[] quantisationTable = segment(0xDB, new byte[65]);
        byte[] app3 = segment(0xE3, "META".getBytes(StandardCharsets.US_ASCII));
        byte[] app11Jumbf = segment(0xEB, concat("JP".getBytes(StandardCharsets.US_ASCII),
                new byte[]{0, 1, 0, 0, 0, 1, 0, 0, 0, 0x20}, "jumbc2pa".getBytes(StandardCharsets.US_ASCII)));
        byte[] app12 = segment(0xEC, "Ducky\0camera text".getBytes(StandardCharsets.US_ASCII));
        byte[] app15 = segment(0xEF, "maker".getBytes(StandardCharsets.US_ASCII));
        byte[] in = jpeg(app0Jfif(), app2Icc(), app3, app11Jumbf, app12, app14Adobe(), app15, quantisationTable);

        ImageUpload out = ImageUpload.of(in).withoutMetadata();

        assertThat(markers(out.bytes())).containsExactly(APP0, APP2, APP14, 0xDB);
        assertThat(segments(out.bytes())).containsExactly(app0Jfif(), app2Icc(), app14Adobe(), quantisationTable);
        assertThat(fromStartOfScan(out.bytes())).isEqualTo(fromStartOfScan(in));
        assertThat(totalViolations()).isZero();
    }

    @Test
    void jpeg_fillBytesBeforeExif_stillStripped() {
        byte[] in = jpeg(app0Jfif(), FILL, app1Exif(ByteOrder.BIG_ENDIAN, null, 45.5, -122.7));
        assertThat(ImageUpload.of(in).gps()).isPresent();

        ImageUpload out = ImageUpload.of(in).withoutMetadata();

        assertThat(indexOf(out.bytes(), EXIF_HEADER)).isNegative();
        assertThat(out.bytes()).isEqualTo(jpeg(app0Jfif()));
        assertThat(out.gps()).isEmpty();
        assertThat(totalViolations()).isZero();
    }

    @Test
    void jpeg_fillBytesBeforeApp0_stillStripped() {
        byte[] in = jpeg(FILL, app0Jfif(), app1Exif(ByteOrder.BIG_ENDIAN, null, 45.5, -122.7));
        assertThat(ImageUpload.of(in).gps()).isPresent();

        ImageUpload out = ImageUpload.of(in).withoutMetadata();

        assertThat(indexOf(out.bytes(), EXIF_HEADER)).isNegative();
        assertThat(out.bytes()).isEqualTo(jpeg(FILL, app0Jfif()));
        assertThat(totalViolations()).isZero();
    }

    @Test
    void jpeg_junkByteBeforeExif_stillStripped() {
        byte[] in = jpeg(app0Jfif(), new byte[]{0x00}, app1Exif(ByteOrder.BIG_ENDIAN, null, 45.5, -122.7));
        assertThat(ImageUpload.of(in).gps()).isPresent();

        ImageUpload out = ImageUpload.of(in).withoutMetadata();

        assertThat(indexOf(out.bytes(), EXIF_HEADER)).isNegative();
        assertThat(out.bytes()).isEqualTo(jpeg(app0Jfif()));
        assertThat(violations("ImageUpload.withoutMetadata.junk")).isEqualTo(1);
    }

    @Test
    void jpeg_junkBeforeAnImageSegment_isDropped() {
        byte[] in = jpeg(app0Jfif(), new byte[]{0x12, (byte) 0xFF, 0x00, 0x34}, app2Icc());

        ImageUpload out = ImageUpload.of(in).withoutMetadata();

        assertThat(out.bytes()).isEqualTo(jpeg(app0Jfif(), app2Icc()));
        assertThat(violations("ImageUpload.withoutMetadata.junk")).isEqualTo(1);
    }

    @Test
    void jpeg_restartMarkerBeforeExif_stillStripped() {
        byte[] restart = {(byte) 0xFF, (byte) 0xD0};
        byte[] in = jpeg(app0Jfif(), restart, app1Exif(ByteOrder.BIG_ENDIAN, null, 45.5, -122.7));

        ImageUpload out = ImageUpload.of(in).withoutMetadata();

        assertThat(indexOf(out.bytes(), EXIF_HEADER)).isNegative();
        assertThat(out.bytes()).isEqualTo(jpeg(app0Jfif(), restart));
        assertThat(totalViolations()).isZero();
    }

    @Test
    void jpeg_fillBytesBeforeSos_scanKeptIntact() {
        byte[] in = jpeg(app0Jfif(), com("x"), FILL);

        ImageUpload out = ImageUpload.of(in).withoutMetadata();

        assertThat(out.bytes()).isEqualTo(jpeg(app0Jfif(), FILL));
        byte[] scanToEnd = Arrays.copyOfRange(in, in.length - 10, in.length);
        assertThat(Arrays.copyOfRange(out.bytes(), out.bytes().length - 10, out.bytes().length)).isEqualTo(scanToEnd);
        assertThat(totalViolations()).isZero();
    }

    @Test
    void jpeg_keepsOrientationAsTheOnlyExifTag() {
        byte[] in = jpeg(app0Jfif(), app1Exif(ByteOrder.LITTLE_ENDIAN, 6, 45.5, -122.7));

        ImageUpload out = ImageUpload.of(in).withoutMetadata();

        assertThat(segments(out.bytes())).containsExactly(app0Jfif(), ImageMetadataSegments.orientationOnlyApp1(6));
        assertThat(out.gps()).isEmpty();
        assertThat(fromStartOfScan(out.bytes())).isEqualTo(fromStartOfScan(in));
    }

    @Test
    void orientationOnlyApp1_isAnExifSegmentWithOneTag() {
        byte[] expected = {
                (byte) 0xFF, (byte) 0xE1, 0x00, 0x22,
                'E', 'x', 'i', 'f', 0, 0,
                'M', 'M', 0x00, 0x2A, 0x00, 0x00, 0x00, 0x08,
                0x00, 0x01,
                0x01, 0x12, 0x00, 0x03, 0x00, 0x00, 0x00, 0x01, 0x00, 0x06, 0x00, 0x00,
                0x00, 0x00, 0x00, 0x00};
        assertThat(ImageMetadataSegments.orientationOnlyApp1(6)).isEqualTo(expected);
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 3, 4, 5, 6, 7, 8})
    void jpeg_everyOrientationValueSurvives(int orientation) {
        byte[] in = jpeg(app0Jfif(), app1Exif(ByteOrder.BIG_ENDIAN, orientation, 45.5, -122.7));

        ImageUpload out = ImageUpload.of(in).withoutMetadata();

        if (orientation == 1) {
            assertThat(segments(out.bytes())).containsExactly(app0Jfif());
        } else {
            assertThat(segments(out.bytes()))
                    .containsExactly(app0Jfif(), ImageMetadataSegments.orientationOnlyApp1(orientation));
        }
    }

    @Test
    void jpeg_withoutApp0_orientationFollowsStartOfImage() {
        byte[] in = jpeg(app2Icc(), app1Exif(ByteOrder.BIG_ENDIAN, 3, 45.5, -122.7));

        ImageUpload out = ImageUpload.of(in).withoutMetadata();

        assertThat(segments(out.bytes())).containsExactly(ImageMetadataSegments.orientationOnlyApp1(3), app2Icc());
    }

    @Test
    void jpeg_twoApp1Segments_bothRemoved_orientationFromTheExifOne() {
        byte[] in = jpeg(app0Jfif(), app1Xmp(), app1Exif(ByteOrder.BIG_ENDIAN, 8, 45.5, -122.7));

        ImageUpload out = ImageUpload.of(in).withoutMetadata();

        assertThat(segments(out.bytes())).containsExactly(app0Jfif(), ImageMetadataSegments.orientationOnlyApp1(8));
        assertThat(out.gps()).isEmpty();
    }

    @Test
    void jpeg_isIdempotent() {
        byte[] in = jpeg(app0Jfif(), app1Exif(ByteOrder.LITTLE_ENDIAN, 6, 45.5, -122.7), app1Xmp(), app2Icc(),
                com("x"));

        ImageUpload once = ImageUpload.of(in).withoutMetadata();
        ImageUpload twice = once.withoutMetadata();

        assertThat(twice.bytes()).isEqualTo(once.bytes());
        assertThat(twice).isSameAs(once);
    }

    @Test
    void jpeg_withoutMetadata_isUnchanged() {
        byte[] in = jpeg(app0Jfif(), app2Icc(), app14Adobe());

        assertThat(ImageUpload.of(in).withoutMetadata().bytes()).isEqualTo(in);
    }

    @Test
    void tinyJpegFromTheSuites_isUnchanged() {
        ImageUpload out = ImageUpload.of(TINY_JPEG.clone()).withoutMetadata();

        assertThat(out.bytes()).isEqualTo(TINY_JPEG);
        assertThat(violations("ImageUpload.withoutMetadata.wellFormed")).isEqualTo(1);
    }

    @Test
    void jpeg_corruptAfterExif_stripsWhatItParsed_copiesTheRest() {
        byte[] corrupt = {0x12, 0x34, 0x56, 0x78};
        byte[] in = concat(SOI, app0Jfif(), app1Exif(ByteOrder.BIG_ENDIAN, null, 45.5, -122.7), corrupt);

        ImageUpload out = ImageUpload.of(in).withoutMetadata();

        assertThat(out.bytes()).isEqualTo(concat(SOI, app0Jfif(), corrupt));
        assertThat(violations("ImageUpload.withoutMetadata.wellFormed")).isEqualTo(1);
    }

    @Test
    void jpeg_exifApp1RunningPastTheEnd_isDroppedWithEverythingAfterIt() {
        byte[] in = concat(SOI, app0Jfif(), overrun(app1Exif(ByteOrder.BIG_ENDIAN, 6, 45.5, -122.7)), GARBAGE);

        ImageUpload out = ImageUpload.of(in).withoutMetadata();

        assertThat(out.bytes()).isEqualTo(concat(SOI, app0Jfif()));
        assertThat(violations("ImageUpload.withoutMetadata.wellFormed")).isEqualTo(1);
    }

    @Test
    void jpeg_exifApp1RunningPastTheEnd_rightAfterSoi_stripsToNoImageAtAll() {
        byte[] in = concat(SOI, overrun(app1Exif(ByteOrder.BIG_ENDIAN, null, 45.5, -122.7)), GARBAGE);
        assertThat(ImageUpload.of(in).isValid()).isTrue();

        ImageUpload out = ImageUpload.of(in).withoutMetadata();

        assertThat(out.bytes()).isEqualTo(SOI);
        assertThat(out.isValid()).isFalse();
    }

    @Test
    void jpeg_bytesAfterEoi_areDropped() {
        byte[] image = jpeg(app0Jfif(), app2Icc());
        byte[] motionPhoto = concat(image, new byte[]{0, 0, 0, 0x18, 'f', 't', 'y', 'p', 'm', 'p', '4', '2'});

        ImageUpload out = ImageUpload.of(motionPhoto).withoutMetadata();

        assertThat(out.bytes()).isEqualTo(image);
        assertThat(violations("ImageUpload.withoutMetadata.trailer")).isEqualTo(1);
    }

    @Test
    void jpeg_scanWithStuffedBytesAndRestarts_isKeptWhole() {
        byte[] scan = {(byte) 0xFF, (byte) 0xDA, 0x00, 0x02,
                0x12, (byte) 0xFF, 0x00, 0x34, (byte) 0xFF, (byte) 0xD0, 0x56, (byte) 0xFF, (byte) 0xFF, (byte) 0xD9};
        byte[] in = concat(SOI, app0Jfif(), com("x"), scan);

        ImageUpload out = ImageUpload.of(in).withoutMetadata();

        assertThat(out.bytes()).isEqualTo(concat(SOI, app0Jfif(), scan));
        assertThat(totalViolations()).isZero();
    }

    @Test
    void jpeg_commentBetweenScans_isDropped() {
        byte[] firstScan = {(byte) 0xFF, (byte) 0xDA, 0x00, 0x02, 0x12, 0x34};
        byte[] secondScan = {(byte) 0xFF, (byte) 0xDA, 0x00, 0x02, 0x56, 0x78, (byte) 0xFF, (byte) 0xD9};
        byte[] in = concat(SOI, app0Jfif(), firstScan, com("taken at home"), secondScan);

        ImageUpload out = ImageUpload.of(in).withoutMetadata();

        assertThat(out.bytes()).isEqualTo(concat(SOI, app0Jfif(), firstScan, secondScan));
    }

    // ---- PNG ----------------------------------------------------------------------------------

    @Test
    void png_dropsExifAndTextChunks_keepsTheRest() {
        byte[] in = png(ihdr(), exif(tiff(ByteOrder.BIG_ENDIAN, 6, 45.5, -122.7)), text("Comment", "x"),
                chunk("zTXt", new byte[]{'k', 0, 0, 0x78, (byte) 0x9C, 0x03, 0, 0, 0, 0, 1}),
                chunk("iTXt", new byte[]{'k', 0, 0, 0, 0, 0, 'x'}), idat(), iend());

        ImageUpload out = ImageUpload.of(in).withoutMetadata();

        assertThat(pngChunkTypes(out.bytes())).containsExactly("IHDR", "IDAT", "IEND");
        assertThat(out.bytes()).isEqualTo(png(ihdr(), idat(), iend()));
        assertThat(out.gps()).isEmpty();
    }

    @Test
    void png_bytesAfterIend_areDropped() {
        byte[] image = png(ihdr(), idat(), iend());

        ImageUpload out = ImageUpload.of(concat(image, GARBAGE)).withoutMetadata();

        assertThat(out.bytes()).isEqualTo(image);
        assertThat(violations("ImageUpload.withoutMetadata.trailer")).isEqualTo(1);
    }

    @Test
    void png_textChunkRunningPastTheEnd_isDroppedWithEverythingAfterIt() {
        byte[] text = text("Comment", "taken at home");
        text[3] = (byte) 0x7F;  // the chunk now claims more data than the file holds
        byte[] in = concat(png(ihdr(), idat()), text, iend());

        ImageUpload out = ImageUpload.of(in).withoutMetadata();

        assertThat(out.bytes()).isEqualTo(png(ihdr(), idat()));
        assertThat(violations("ImageUpload.withoutMetadata.wellFormed")).isEqualTo(1);
    }

    @Test
    void png_withoutMetadata_isUnchanged() {
        byte[] in = png(ihdr(), idat(), iend());

        assertThat(ImageUpload.of(in).withoutMetadata().bytes()).isEqualTo(in);
    }

    // ---- WebP ---------------------------------------------------------------------------------

    @Test
    void webp_dropsExifAndXmp_fixesRiffSizeAndVp8xFlags() {
        byte[] in = webp(true, true);
        assertThat(riffChunks(in).get(0).payload()[0] & 0b1100).isEqualTo(0b1100);

        byte[] out = ImageUpload.of(in).withoutMetadata().bytes();

        List<RiffChunk> chunks = riffChunks(out);
        assertThat(chunks).extracting(RiffChunk::fourCc).containsExactly("VP8X", "VP8L");
        assertThat(riffSize(out)).isEqualTo(out.length - 8);
        assertThat(chunks.get(0).payload()[0] & 0b1100).isZero();
        assertThat(chunks.get(0).payload()[0] & 0x10).isEqualTo(0x10);
        assertThat(chunks.get(1)).isEqualTo(riffChunks(in).get(1));
        assertThat(ImageUpload.of(out).gps()).isEmpty();
    }

    @Test
    void webp_strippedIsTheMetadataFreeEncoding() {
        assertThat(ImageUpload.of(webp(true, true)).withoutMetadata().bytes()).isEqualTo(webp(false, false));
    }

    @Test
    void webp_oddLengthChunk_paddingHandled() {
        List<RiffChunk> plain = riffChunks(webp(false, false));
        RiffChunk vp8x = plain.get(0);
        RiffChunk vp8l = plain.get(1);
        RiffChunk xmp = riffChunks(webp(false, true)).get(2);
        assertThat(xmp.fourCc()).isEqualTo("XMP ");
        assertThat(xmp.payload().length % 2).isOne();
        byte[] flags = vp8x.payload().clone();
        flags[0] |= 0x04;
        // The padded XMP chunk sits between the header and the image, so the image must survive its removal.
        byte[] in = riff(new RiffChunk("VP8X", flags), xmp, vp8l);

        byte[] out = ImageUpload.of(in).withoutMetadata().bytes();

        assertThat(riffChunks(out)).containsExactly(vp8x, vp8l);
        assertThat(out).isEqualTo(webp(false, false));
    }

    @Test
    void webp_bytesBeyondTheRiffSize_areDropped() {
        byte[] image = webp(false, false);

        ImageUpload out = ImageUpload.of(concat(image, GARBAGE)).withoutMetadata();

        assertThat(out.bytes()).isEqualTo(image);
        assertThat(violations("ImageUpload.withoutMetadata.trailer")).isEqualTo(1);
    }

    @Test
    void webp_withoutMetadata_isUnchanged() {
        byte[] in = webp(false, false);

        assertThat(ImageUpload.of(in).withoutMetadata().bytes()).isEqualTo(in);
    }

    // ---- all formats --------------------------------------------------------------------------

    @Test
    void mediaType_andValidity_preservedAfterStrip() {
        List<ImageUpload> uploads = List.of(
                ImageUpload.of(jpeg(app0Jfif(), app1Exif(ByteOrder.BIG_ENDIAN, 6, 45.5, -122.7), com("x"))),
                ImageUpload.of(png(ihdr(), exif(tiff(ByteOrder.BIG_ENDIAN, 6, 45.5, -122.7)), idat(), iend())),
                ImageUpload.of(webp(true, true)));

        for (ImageUpload upload : uploads) {
            ImageUpload stripped = upload.withoutMetadata();
            assertThat(stripped).isNotEqualTo(upload);
            assertThat(stripped.mediaType()).isEqualTo(upload.mediaType());
            assertThat(stripped.isValid()).isTrue();
            assertThat(stripped.withoutMetadata()).isEqualTo(stripped);
        }
        assertThat(totalViolations()).isZero();
    }

    // ---- reading the output back --------------------------------------------------------------

    /** The marker of every segment between {@code SOI} and start-of-scan. */
    private static List<Integer> markers(byte[] jpeg) {
        List<Integer> markers = new ArrayList<>();
        for (byte[] segment : segments(jpeg)) markers.add(segment[1] & 0xFF);
        return markers;
    }

    /** Every whole segment, marker and length included, between {@code SOI} and start-of-scan. */
    private static List<byte[]> segments(byte[] jpeg) {
        List<byte[]> segments = new ArrayList<>();
        int pos = 2;
        while ((jpeg[pos + 1] & 0xFF) != 0xDA) {
            int length = ((jpeg[pos + 2] & 0xFF) << 8) | (jpeg[pos + 3] & 0xFF);
            segments.add(Arrays.copyOfRange(jpeg, pos, pos + 2 + length));
            pos += 2 + length;
        }
        return segments;
    }

    /** Start-of-scan to the end: the entropy-coded data and {@code EOI}. */
    private static byte[] fromStartOfScan(byte[] jpeg) {
        int pos = 2;
        for (byte[] segment : segments(jpeg)) pos += segment.length;
        return Arrays.copyOfRange(jpeg, pos, jpeg.length);
    }

    private static List<String> pngChunkTypes(byte[] png) {
        List<String> types = new ArrayList<>();
        ByteBuffer b = ByteBuffer.wrap(png);
        int pos = 8;
        while (pos < png.length) {
            int length = b.getInt(pos);
            types.add(new String(png, pos + 4, 4, StandardCharsets.US_ASCII));
            pos += 12 + length;
        }
        return types;
    }

    private record RiffChunk(String fourCc, byte[] payload) {
        @Override
        public boolean equals(Object other) {
            return other instanceof RiffChunk c && fourCc.equals(c.fourCc) && Arrays.equals(payload, c.payload);
        }

        @Override
        public int hashCode() {
            return fourCc.hashCode() * 31 + Arrays.hashCode(payload);
        }
    }

    private static List<RiffChunk> riffChunks(byte[] webp) {
        List<RiffChunk> chunks = new ArrayList<>();
        ByteBuffer b = ByteBuffer.wrap(webp).order(ByteOrder.LITTLE_ENDIAN);
        int pos = 12;
        while (pos < webp.length) {
            int size = b.getInt(pos + 4);
            chunks.add(new RiffChunk(new String(webp, pos, 4, StandardCharsets.US_ASCII),
                    Arrays.copyOfRange(webp, pos + 8, pos + 8 + size)));
            pos += 8 + size + (size & 1);
        }
        return chunks;
    }

    /** A WebP of {@code chunks} in order, each padded to even length, under a RIFF size that counts them. */
    private static byte[] riff(RiffChunk... chunks) {
        int body = 4;
        for (RiffChunk chunk : chunks) body += 8 + chunk.payload().length + chunk.payload().length % 2;
        ByteBuffer b = ByteBuffer.allocate(8 + body).order(ByteOrder.LITTLE_ENDIAN)
                .put("RIFF".getBytes(StandardCharsets.US_ASCII)).putInt(body)
                .put("WEBP".getBytes(StandardCharsets.US_ASCII));
        for (RiffChunk chunk : chunks) {
            b.put(chunk.fourCc().getBytes(StandardCharsets.US_ASCII)).putInt(chunk.payload().length)
                    .put(chunk.payload());
            if (chunk.payload().length % 2 == 1) b.put((byte) 0);
        }
        return b.array();
    }

    /** {@code segment} with its length field claiming far more than the bytes that follow. */
    private static byte[] overrun(byte[] segment) {
        byte[] out = segment.clone();
        out[2] = (byte) 0x7F;
        out[3] = (byte) 0xFF;
        return out;
    }

    private static int riffSize(byte[] webp) {
        return ByteBuffer.wrap(webp).order(ByteOrder.LITTLE_ENDIAN).getInt(4);
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

    private static int indexOf(byte[] haystack, byte[] needle) {
        for (int i = 0; i + needle.length <= haystack.length; i++) {
            if (Arrays.equals(haystack, i, i + needle.length, needle, 0, needle.length)) return i;
        }
        return -1;
    }

    private static byte[] concat(byte[]... parts) {
        int length = 0;
        for (byte[] part : parts) length += part.length;
        byte[] out = new byte[length];
        int at = 0;
        for (byte[] part : parts) {
            System.arraycopy(part, 0, out, at, part.length);
            at += part.length;
        }
        return out;
    }
}
