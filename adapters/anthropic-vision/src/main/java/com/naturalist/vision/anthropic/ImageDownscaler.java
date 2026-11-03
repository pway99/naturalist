package com.naturalist.vision.anthropic;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;
import javax.imageio.stream.MemoryCacheImageInputStream;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Bounds the image one identification ships to the vendor (ADR-028, spend layer 5). The
 * vendor's effective maximum is {@value #MAX_EDGE}&nbsp;px on the longest edge; anything
 * larger is resized on their side and billed as if it were not, so sending it costs upload
 * time and nothing else.
 *
 * <p>A decodable image (JPEG, PNG and GIF through the JDK's own {@code ImageIO}) whose
 * longest edge exceeds {@link #MAX_EDGE} is scaled so that edge is exactly {@link #MAX_EDGE},
 * flattened onto white (so an alpha channel survives as a JPEG) and re-encoded as a JPEG at
 * quality {@code 0.85}. Everything else passes through as the same array: an image already
 * within the limit, WebP (which the JDK cannot decode, so it stays bounded only by
 * {@code ImageUpload.MAX_BYTES}), and bytes the decoder rejects. EXIF orientation is not
 * applied (parked as PL-17). No image library is added for this.
 *
 * <p>Decoding is bounded by the header, not the upload size: a few kilobytes of PNG can declare
 * a raster of gigabytes. The declared dimensions are read first; past {@link #MAX_PIXELS} the
 * bytes pass through undecoded (the vendor refuses what it cannot use, at no charge), and below
 * it a large image is decoded with source subsampling, so the raster held is never much more
 * than twice {@link #MAX_EDGE} on its longest edge. The stream is cached in memory, never in a
 * temporary file.
 */
final class ImageDownscaler {

    /** The longest edge, in pixels, the vendor uses without resizing. */
    static final int MAX_EDGE = 1568;

    /** Past this many declared pixels an image is not decoded at all. */
    static final long MAX_PIXELS = 100_000_000L;

    private static final float JPEG_QUALITY = 0.85f;

    /** The bytes to send and the media type that describes them. */
    record Sized(byte[] bytes, String mediaType) {
    }

    private ImageDownscaler() {
    }

    /**
     * Fits {@code bytes} within {@link #MAX_EDGE}: the same array and media type when there is
     * nothing to do, or a newly encoded JPEG when the image was larger.
     */
    static Sized fit(byte[] bytes, String mediaType) {
        var unchanged = new Sized(bytes, mediaType);
        try (ImageInputStream in = new MemoryCacheImageInputStream(new ByteArrayInputStream(bytes))) {
            var readers = ImageIO.getImageReaders(in);
            if (!readers.hasNext()) {
                return unchanged;
            }
            ImageReader reader = readers.next();
            try {
                reader.setInput(in, true, true);
                int width = reader.getWidth(0);
                int height = reader.getHeight(0);
                int longest = Math.max(width, height);
                if (longest <= MAX_EDGE || (long) width * height > MAX_PIXELS) {
                    return unchanged;
                }
                ImageReadParam param = reader.getDefaultReadParam();
                int step = Math.max(1, longest / MAX_EDGE);
                param.setSourceSubsampling(step, step, 0, 0);
                return new Sized(encodeJpeg(scale(reader.read(0, param), width, height)), "image/jpeg");
            } finally {
                reader.dispose();
            }
        } catch (IOException | RuntimeException undecodable) {
            return unchanged;
        }
    }

    /**
     * Scales {@code source} -- possibly already subsampled -- to the size a
     * {@code width x height} original takes with its longest edge at {@link #MAX_EDGE}.
     */
    private static BufferedImage scale(BufferedImage source, int width, int height) {
        double factor = (double) MAX_EDGE / Math.max(width, height);
        int scaledWidth = width >= height ? MAX_EDGE : Math.max(1, (int) Math.round(width * factor));
        int scaledHeight = height > width ? MAX_EDGE : Math.max(1, (int) Math.round(height * factor));

        var scaled = new BufferedImage(scaledWidth, scaledHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = scaled.createGraphics();
        try {
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, scaledWidth, scaledHeight);
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.drawImage(source, 0, 0, scaledWidth, scaledHeight, null);
        } finally {
            g.dispose();
        }
        return scaled;
    }

    private static byte[] encodeJpeg(BufferedImage image) throws IOException {
        var writers = ImageIO.getImageWritersByFormatName("jpeg");
        if (!writers.hasNext()) {
            throw new IOException("no JPEG ImageWriter available");
        }
        ImageWriter writer = writers.next();
        var out = new ByteArrayOutputStream();
        try (ImageOutputStream stream = ImageIO.createImageOutputStream(out)) {
            writer.setOutput(stream);
            ImageWriteParam param = writer.getDefaultWriteParam();
            param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            param.setCompressionQuality(JPEG_QUALITY);
            writer.write(null, new IIOImage(image, null, null), param);
        } finally {
            writer.dispose();
        }
        return out.toByteArray();
    }
}
