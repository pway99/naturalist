package com.naturalist.vision.anthropic;

import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.nio.ByteBuffer;
import java.util.zip.CRC32;
import java.util.zip.DeflaterOutputStream;

import static java.nio.charset.StandardCharsets.US_ASCII;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link ImageDownscaler#fit} bounds what one identification ships to the vendor: a decodable
 * image larger than {@link ImageDownscaler#MAX_EDGE} on its longest edge is re-encoded as a
 * JPEG at that edge; everything else -- small images, WebP, bytes the JDK cannot decode --
 * passes through as the same array.
 */
class ImageDownscalerTest {

    @Test
    void largePng_isDownscaledToTheLongestEdge_asJpeg() throws IOException {
        var sized = ImageDownscaler.fit(png(4000, 3000, BufferedImage.TYPE_INT_RGB), "image/png");
        var out = ImageIO.read(new ByteArrayInputStream(sized.bytes()));
        assertThat(sized.mediaType()).isEqualTo("image/jpeg");
        assertThat(out.getWidth()).isEqualTo(1568);
        assertThat(out.getHeight()).isEqualTo(1176);
        assertThat(sized.bytes()).startsWith((byte) 0xFF, (byte) 0xD8);
    }

    @Test
    void portrait_isScaledOnItsLongestEdge() throws IOException {
        var sized = ImageDownscaler.fit(png(1000, 4000, BufferedImage.TYPE_INT_RGB), "image/png");
        var out = ImageIO.read(new ByteArrayInputStream(sized.bytes()));
        assertThat(out.getHeight()).isEqualTo(1568);
        assertThat(out.getWidth()).isEqualTo(392);
    }

    @Test
    void argbPng_isFlattenedToJpeg() throws IOException {
        var sized = ImageDownscaler.fit(png(3000, 3000, BufferedImage.TYPE_INT_ARGB), "image/png");
        assertThat(sized.mediaType()).isEqualTo("image/jpeg");
        assertThat(ImageIO.read(new ByteArrayInputStream(sized.bytes())).getWidth()).isEqualTo(1568);
    }

    @Test
    void smallJpeg_passesThroughUnchanged() throws IOException {
        byte[] jpeg = jpeg(800, 600);
        var sized = ImageDownscaler.fit(jpeg, "image/jpeg");
        assertThat(sized.bytes()).isSameAs(jpeg);
        assertThat(sized.mediaType()).isEqualTo("image/jpeg");
    }

    @Test
    void exactlyTheLimit_passesThroughUnchanged() throws IOException {
        byte[] jpeg = jpeg(1568, 1000);
        assertThat(ImageDownscaler.fit(jpeg, "image/jpeg").bytes()).isSameAs(jpeg);
    }

    @Test
    void webp_passesThroughUnchanged() {
        byte[] webp = new byte[64];
        System.arraycopy("RIFF".getBytes(US_ASCII), 0, webp, 0, 4);
        System.arraycopy("WEBP".getBytes(US_ASCII), 0, webp, 8, 4);
        var sized = ImageDownscaler.fit(webp, "image/webp");
        assertThat(sized.bytes()).isSameAs(webp);
        assertThat(sized.mediaType()).isEqualTo("image/webp");
    }

    @Test
    void undecodableJpeg_passesThroughUnchanged() {
        byte[] broken = new byte[64];
        broken[0] = (byte) 0xFF;
        broken[1] = (byte) 0xD8;
        broken[2] = (byte) 0xFF;
        assertThat(ImageDownscaler.fit(broken, "image/jpeg").bytes()).isSameAs(broken);
    }

    /**
     * A decompression bomb: a few hundred bytes whose header declares 20000 x 20000 pixels.
     * Decoding it would allocate a 400 MB raster before the missing pixel data is noticed;
     * {@code fit} reads the header only, sees the declared size is past
     * {@link ImageDownscaler#MAX_PIXELS}, and passes the bytes through.
     */
    @Test
    void pngDeclaringHugeDimensions_passesThrough_withoutAllocatingItsRaster() throws IOException {
        byte[] bomb = pngHeaderOnly(20_000, 20_000);
        var threads = (com.sun.management.ThreadMXBean) ManagementFactory.getThreadMXBean();
        long thread = Thread.currentThread().threadId();
        long before = threads.getThreadAllocatedBytes(thread);

        var sized = ImageDownscaler.fit(bomb, "image/png");

        long allocated = threads.getThreadAllocatedBytes(thread) - before;
        assertThat(sized.bytes()).isSameAs(bomb);
        assertThat(allocated).as("bytes allocated while fitting the bomb").isLessThan(64L * 1024 * 1024);
    }

    /** A large image just under {@link ImageDownscaler#MAX_PIXELS} is still decoded and scaled. */
    @Test
    void largeImageUnderThePixelCap_isStillDownscaled() throws IOException {
        var sized = ImageDownscaler.fit(jpeg(8000, 6000), "image/jpeg");
        var out = ImageIO.read(new ByteArrayInputStream(sized.bytes()));
        assertThat(out.getWidth()).isEqualTo(1568);
        assertThat(out.getHeight()).isEqualTo(1176);
    }

    private static byte[] png(int width, int height, int type) throws IOException {
        return encode(render(width, height, type), "png");
    }

    private static byte[] jpeg(int width, int height) throws IOException {
        return encode(render(width, height, BufferedImage.TYPE_INT_RGB), "jpeg");
    }

    private static BufferedImage render(int width, int height, int type) {
        var image = new BufferedImage(width, height, type);
        Graphics2D g = image.createGraphics();
        try {
            g.setPaint(new GradientPaint(0, 0, Color.GREEN, width, height, Color.BLUE));
            g.fillRect(0, 0, width, height);
        } finally {
            g.dispose();
        }
        return image;
    }

    private static byte[] encode(BufferedImage image, String format) throws IOException {
        var out = new ByteArrayOutputStream();
        if (!ImageIO.write(image, format, out)) {
            throw new IOException("no ImageIO writer for " + format);
        }
        return out.toByteArray();
    }

    /** A valid 8-bit greyscale PNG header declaring {@code width x height}, with one row of pixels. */
    private static byte[] pngHeaderOnly(int width, int height) throws IOException {
        byte[] ihdr = ByteBuffer.allocate(13).putInt(width).putInt(height)
                .put((byte) 8).put((byte) 0).put((byte) 0).put((byte) 0).put((byte) 0).array();
        var deflated = new ByteArrayOutputStream();
        try (var z = new DeflaterOutputStream(deflated)) {
            z.write(new byte[width + 1]);
        }
        var out = new ByteArrayOutputStream();
        out.write(new byte[] {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n'});
        chunk(out, "IHDR", ihdr);
        chunk(out, "IDAT", deflated.toByteArray());
        chunk(out, "IEND", new byte[0]);
        return out.toByteArray();
    }

    private static void chunk(ByteArrayOutputStream out, String type, byte[] data) throws IOException {
        byte[] name = type.getBytes(US_ASCII);
        var crc = new CRC32();
        crc.update(name);
        crc.update(data);
        out.write(ByteBuffer.allocate(4).putInt(data.length).array());
        out.write(name);
        out.write(data);
        out.write(ByteBuffer.allocate(4).putInt((int) crc.getValue()).array());
    }
}
