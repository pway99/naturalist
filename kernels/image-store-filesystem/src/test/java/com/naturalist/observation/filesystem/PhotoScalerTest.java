package com.naturalist.observation.filesystem;

import com.naturalist.observation.ImageUpload;
import com.naturalist.observation.filesystem.PhotoScaler.Dimensions;
import com.naturalist.observation.test.ImageFixtures;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.zip.CRC32;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * {@link PhotoScaler} decodes untrusted bytes within a bounded raster and writes one canonical
 * shape: an upright, white-flattened, metadata-free JPEG no longer than the asked edge.
 */
class PhotoScalerTest {

    @Test
    void fitJpeg_scalesTheLongestEdgeTo640() {
        byte[] result = fit(image("jpg", 2000, 1500, BufferedImage.TYPE_INT_RGB), 640, 1);

        assertThat(decode(result).getWidth()).isEqualTo(640);
        assertThat(decode(result).getHeight()).isEqualTo(480);
        assertThat(ImageUpload.of(result).mediaType()).isEqualTo("image/jpeg");
    }

    @Test
    void fitJpeg_neverUpscales() {
        byte[] result = fit(image("png", 300, 200, BufferedImage.TYPE_INT_RGB), 640, 1);

        assertThat(decode(result).getWidth()).isEqualTo(300);
        assertThat(decode(result).getHeight()).isEqualTo(200);
        assertThat(ImageUpload.of(result).mediaType()).isEqualTo("image/jpeg");
    }

    @Test
    void fitJpeg_appliesOrientation6() {
        byte[] result = fit(image("jpg", 800, 600, BufferedImage.TYPE_INT_RGB), 640, 6);

        assertThat(decode(result).getWidth()).isEqualTo(480);
        assertThat(decode(result).getHeight()).isEqualTo(640);
    }

    @Test
    void fitJpeg_readsOrientationFromTheSplicedExif() {
        byte[] tagged = withExif(image("jpg", 800, 600, BufferedImage.TYPE_INT_RGB), 6, null, null);

        int orientation = ImageUpload.of(tagged).orientation().orElseThrow();
        byte[] result = fit(tagged, 1024, orientation);

        assertThat(orientation).isEqualTo(6);
        assertThat(decode(result).getWidth()).isEqualTo(600);
        assertThat(decode(result).getHeight()).isEqualTo(800);
    }

    @Test
    void fitJpeg_treatsAnUnknownOrientationAsUpright() {
        byte[] result = fit(image("jpg", 800, 600, BufferedImage.TYPE_INT_RGB), 1024, 99);

        assertThat(decode(result).getWidth()).isEqualTo(800);
        assertThat(decode(result).getHeight()).isEqualTo(600);
    }

    @Test
    void fitJpeg_flattensTransparencyOntoWhite() {
        byte[] result = fit(image("png", 10, 10, BufferedImage.TYPE_INT_ARGB), 640, 1);

        assertThat(decode(result).getRGB(5, 5) & 0xFFFFFF).isEqualTo(0xFFFFFF);
    }

    @Test
    void fitJpeg_writesNoMetadata() {
        byte[] tagged = withExif(image("jpg", 400, 300, BufferedImage.TYPE_INT_RGB), 6, 45.5, -122.7);

        byte[] result = fit(tagged, 1024, 1);

        var upload = ImageUpload.of(result);
        assertThat(upload.withoutMetadata()).isEqualTo(upload);
        assertThat(upload.orientation()).isEmpty();
        assertThat(upload.gps()).isEmpty();
    }

    @Test
    void fitJpeg_decodesWebp() throws IOException {
        byte[] webp;
        try (var in = PhotoScalerTest.class.getResourceAsStream("/scaler/alpha.webp")) {
            webp = in.readAllBytes();
        }

        byte[] result = fit(webp, 640, 1);

        assertThat(ImageUpload.of(result).mediaType()).isEqualTo("image/jpeg");
        assertThat(PhotoScaler.declared(result)).contains(new Dimensions(64, 48));
        // The fixture's left strip (x < 8) is mostly transparent and its green channel is near 0
        // at the top row; flattened onto white it lightens. The opaque pixel keeps its colour.
        BufferedImage decoded = decode(result);
        assertThat(new Color(decoded.getRGB(2, 2)).getGreen()).isGreaterThan(120);
        assertThat(new Color(decoded.getRGB(40, 2)).getGreen()).isLessThan(60);
    }

    /**
     * A source with a distinct solid colour in each quadrant lands, for every Exif orientation,
     * with those colours in the upright arrangement: a transform that draws off the canvas, swaps
     * 6 and 8, or breaks a flip leaves a white or misplaced corner. The picture is also scaled
     * (400x300 to an edge of 200), so scale and orientation are proved together.
     */
    @Test
    void fitJpeg_placesEveryQuadrantUprightForOrientations1To8() {
        byte[] source = quadrants(400, 300);
        // Source quadrants: A top-left, B top-right, C bottom-right, D bottom-left.
        // Expected [top-left, top-right, bottom-right, bottom-left] of the upright result.
        String[] expected = {"ABCD", "BADC", "CDAB", "DCBA", "ADCB", "DABC", "CBAD", "BCDA"};
        Color[] colour = {Color.RED, Color.GREEN, Color.BLUE, Color.YELLOW};

        for (int orientation = 1; orientation <= 8; orientation++) {
            BufferedImage out = decode(fit(source, 200, orientation));
            int w = out.getWidth();
            int h = out.getHeight();
            int[][] centres = {{w / 4, h / 4}, {3 * w / 4, h / 4}, {3 * w / 4, 3 * h / 4}, {w / 4, 3 * h / 4}};
            for (int corner = 0; corner < 4; corner++) {
                Color want = colour[expected[orientation - 1].charAt(corner) - 'A'];
                Color got = new Color(out.getRGB(centres[corner][0], centres[corner][1]));
                assertThat(got).as("orientation %d corner %d", orientation, corner).satisfies(c -> {
                    assertThat(Math.abs(c.getRed() - want.getRed())).isLessThan(70);
                    assertThat(Math.abs(c.getGreen() - want.getGreen())).isLessThan(70);
                    assertThat(Math.abs(c.getBlue() - want.getBlue())).isLessThan(70);
                });
                assertThat(got).as("orientation %d corner %d is not white", orientation, corner)
                        .isNotEqualTo(Color.WHITE);
            }
        }
    }

    @Test
    void declared_readsDimensionsWithoutDecoding() {
        assertThat(PhotoScaler.declared(image("jpg", 2000, 1500, BufferedImage.TYPE_INT_RGB)))
                .contains(new Dimensions(2000, 1500));
    }

    @Test
    void dimensions_reportPixelsAndLongestEdge() {
        var dimensions = new Dimensions(50_000, 40_000);

        assertThat(dimensions.pixels()).isEqualTo(2_000_000_000L);
        assertThat(dimensions.longestEdge()).isEqualTo(50_000);
    }

    @Test
    void declared_readsABombsSizeFromTheHeaderAlone() {
        byte[] bomb = pngDeclaring(50_000, 50_000);

        assertThat(PhotoScaler.declared(bomb)).contains(new Dimensions(50_000, 50_000))
                .hasValueSatisfying(d -> assertThat(d.pixels()).isGreaterThan(PhotoScaler.MAX_PIXELS));
    }

    @Test
    void declared_isEmptyForUndecodableBytes() {
        byte[] junk = new byte[64];
        Arrays.fill(junk, (byte) 0x42);
        junk[0] = (byte) 0xFF;
        junk[1] = (byte) 0xD8;
        junk[2] = (byte) 0xFF;

        assertThat(PhotoScaler.declared(junk)).isEmpty();
    }

    @Test
    void declared_readsAnOrdinaryPhotoWithinTheCeiling() {
        assertThat(PhotoScaler.declared(image("jpg", 2000, 1500, BufferedImage.TYPE_INT_RGB)))
                .contains(new Dimensions(2000, 1500))
                .hasValueSatisfying(d -> assertThat(d.pixels()).isLessThanOrEqualTo(PhotoScaler.MAX_PIXELS));
    }

    /** {@code jpeg} with an Exif APP1 spliced in straight after its SOI. */
    private static byte[] withExif(byte[] jpeg, Integer orientation, Double lat, Double lon) {
        byte[] exif = ImageFixtures.app1Exif(ByteOrder.BIG_ENDIAN, orientation, lat, lon);
        byte[] tagged = new byte[jpeg.length + exif.length];
        System.arraycopy(jpeg, 0, tagged, 0, 2);
        System.arraycopy(exif, 0, tagged, 2, exif.length);
        System.arraycopy(jpeg, 2, tagged, 2 + exif.length, jpeg.length - 2);
        return tagged;
    }

    private static byte[] image(String format, int width, int height, int type) {
        var image = new BufferedImage(width, height, type);
        var out = new ByteArrayOutputStream();
        try {
            if (type == BufferedImage.TYPE_INT_RGB) {
                var g = image.createGraphics();
                g.setColor(Color.ORANGE);
                g.fillRect(0, 0, width, height);
                g.dispose();
            }
            ImageIO.write(image, format, out);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return out.toByteArray();
    }

    /** A JPEG split into four solid quadrants: red top-left, green top-right, blue bottom-right, yellow bottom-left. */
    private static byte[] quadrants(int width, int height) {
        var image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        var g = image.createGraphics();
        g.setColor(Color.RED);
        g.fillRect(0, 0, width / 2, height / 2);
        g.setColor(Color.GREEN);
        g.fillRect(width / 2, 0, width - width / 2, height / 2);
        g.setColor(Color.BLUE);
        g.fillRect(width / 2, height / 2, width - width / 2, height - height / 2);
        g.setColor(Color.YELLOW);
        g.fillRect(0, height / 2, width / 2, height - height / 2);
        g.dispose();
        var out = new ByteArrayOutputStream();
        try {
            ImageIO.write(image, "jpg", out);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return out.toByteArray();
    }

    private static BufferedImage decode(byte[] bytes) {
        try {
            return ImageIO.read(new ByteArrayInputStream(bytes));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** A PNG whose IHDR declares {@code width x height} and whose IDAT is a stub: never decodable. */
    private static byte[] pngDeclaring(int width, int height) {
        byte[] ihdr = ByteBuffer.allocate(13).putInt(width).putInt(height).put((byte) 8).put((byte) 2)
                .put((byte) 0).put((byte) 0).put((byte) 0).array();
        var out = new ByteArrayOutputStream();
        out.writeBytes(new byte[]{(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A});
        chunk(out, "IHDR", ihdr);
        chunk(out, "IDAT", new byte[]{0x78, (byte) 0x9C, 0x03, 0x00, 0x00, 0x00, 0x00, 0x01});
        chunk(out, "IEND", new byte[0]);
        return out.toByteArray();
    }

    private static void chunk(ByteArrayOutputStream out, String type, byte[] data) {
        byte[] typeBytes = type.getBytes(StandardCharsets.US_ASCII);
        var crc = new CRC32();
        crc.update(typeBytes);
        crc.update(data);
        out.writeBytes(ByteBuffer.allocate(4).putInt(data.length).array());
        out.writeBytes(typeBytes);
        out.writeBytes(data);
        out.writeBytes(ByteBuffer.allocate(4).putInt((int) crc.getValue()).array());
    }

    /** {@code bytes} through {@link PhotoScaler#fitJpeg}, with the dimensions its header declares, as the store passes them. */
    private static byte[] fit(byte[] bytes, int edge, int orientation) {
        return PhotoScaler.fitJpeg(bytes, PhotoScaler.declared(bytes).orElseThrow(), edge, orientation);
    }
}
