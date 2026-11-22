package com.naturalist.observation.filesystem;

import com.naturalist.observability.Handling;
import com.naturalist.observability.MethodObserver;
import com.naturalist.observability.Observer;

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
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Optional;

/**
 * Decodes an upload within a bounded raster and writes the one shape the store keeps: an upright,
 * white-flattened, metadata-free JPEG at quality {@code 0.85} whose longest edge is at most the
 * asked edge ({@link #PHOTO_EDGE} for the photo, {@link #TILE_EDGE} for the tile). An image is
 * never enlarged.
 *
 * <p>Decoding is bounded by the header, not the upload size: a few kilobytes of PNG can declare a
 * raster of gigabytes. {@link #declared} reads the header only; past {@link #MAX_PIXELS} declared
 * pixels an image is refused and is never decoded. Below it, a large image is
 * decoded with source subsampling, so the raster held is never much more than twice the target
 * edge on its longest side. The stream is cached in memory, never in a temporary file.
 *
 * <p>JPEG, PNG and GIF decode through the JDK's {@code ImageIO}; WebP through the TwelveMonkeys
 * {@code ImageIO} plug-in on the runtime classpath, which this class never names.
 */
final class PhotoScaler {

    /** The longest edge, in pixels, of the full-size photo. */
    static final int PHOTO_EDGE = 1024;

    /** The longest edge, in pixels, of the tile. */
    static final int TILE_EDGE = 640;

    /** Past this many declared pixels an image is not decoded at all. */
    static final long MAX_PIXELS = 100_000_000L;

    private static final float JPEG_QUALITY = 0.85f;

    private static final Observer observer = Observer.forClass(PhotoScaler.class);

    /** The size an image's header declares. */
    record Dimensions(int width, int height) {

        long pixels() {
            return (long) width * height;
        }

        int longestEdge() {
            return Math.max(width, height);
        }
    }

    private PhotoScaler() {
    }

    /**
     * The dimensions {@code bytes} declare, read from the header alone; empty when no reader accepts them. An empty
     * array is answered empty like any other bytes that are not an image: conform reads files from disk, and an empty
     * file is one to skip and count, not a refusal.
     */
    static Optional<Dimensions> declared(byte[] bytes) {
        MethodObserver methodObserver = observer.forMethod("declared");
        methodObserver.arguments(i -> i
            .notNull(bytes, "bytes"))
            .throwWhenInvalid();
        try (ImageInputStream in = new MemoryCacheImageInputStream(new ByteArrayInputStream(bytes))) {
            var readers = ImageIO.getImageReaders(in);
            if (!readers.hasNext()) {
                return Optional.empty();
            }
            ImageReader reader = readers.next();
            try {
                reader.setInput(in, true, true);
                var dimensions = new Dimensions(reader.getWidth(0), reader.getHeight(0));
                return dimensions.width() > 0 && dimensions.height() > 0 ? Optional.of(dimensions) : Optional.empty();
            } finally {
                reader.dispose();
            }
        } catch (IOException | RuntimeException undecodable) {
            methodObserver.noticeError("headerRead", undecodable, Handling.SWALLOWED);
            return Optional.empty();
        }
    }


    /**
     * {@code bytes} as an upright JPEG whose longest edge is {@code min(edge, its own)}. The Exif
     * {@code orientation} (1 to 8, anything else read as 1) is applied here because the output
     * carries no metadata to apply it later. The output carries no Exif, XMP, IPTC or comment,
     * only the JFIF header the JDK's JPEG writer emits. {@code declared} is what {@link #declared}
     * read from these bytes; the caller has already refused a header that does not decode or
     * declares more than {@link #MAX_PIXELS} pixels, so this method never re-reads or re-checks it.
     *
     * @throws IllegalArgumentException when the image fails to decode: a refusal of the input, which this method does
     *         not count, so the caller that refuses the photo counts it once, below {@code ERROR}
     * @throws ReencodeFailedException when the decoded image cannot be drawn upright or encoded (or no JPEG writer is
     *         available): a fault, noticed here at {@code ERROR} as {@code encoded}, so a caller passes it on uncounted
     */
    static byte[] fitJpeg(byte[] bytes, Dimensions declared, int edge, int orientation) {
        MethodObserver methodObserver = observer.forMethod("fitJpeg");
        methodObserver.arguments(i -> i
            .notEmpty(bytes, "bytes")
            .notNull(declared, "declared")
            .atLeast(edge, 1, "edge"))
            .throwWhenInvalid();
        int tag = orientation >= 1 && orientation <= 8 ? orientation : 1;
        boolean quarterTurn = tag >= 5;
        int orientedWidth = quarterTurn ? declared.height() : declared.width();
        int orientedHeight = quarterTurn ? declared.width() : declared.height();
        BufferedImage source;
        try {
            source = decode(bytes, declared, edge);
        } catch (IOException | RuntimeException undecodable) {
            // a refusal of the input, not a fault: the caller that refuses the photo counts it once, at WARN (decodable)
            throw new IllegalArgumentException("image could not be decoded", undecodable);
        }
        try {
            return encodeJpeg(upright(source, tag, orientedWidth, orientedHeight, edge));
        } catch (IOException | RuntimeException e) {
            methodObserver.noticeError("encoded", e, Handling.WRAPPED);
            throw new ReencodeFailedException("decoded image could not be re-encoded", e);
        }
    }

    private static BufferedImage decode(byte[] bytes, Dimensions declared, int edge) throws IOException {
        try (ImageInputStream in = new MemoryCacheImageInputStream(new ByteArrayInputStream(bytes))) {
            var readers = ImageIO.getImageReaders(in);
            if (!readers.hasNext()) {
                throw new IOException("no ImageReader accepts the bytes");
            }
            ImageReader reader = readers.next();
            try {
                reader.setInput(in, true, true);
                ImageReadParam param = reader.getDefaultReadParam();
                int step = Math.max(1, declared.longestEdge() / (2 * edge));
                param.setSourceSubsampling(step, step, 0, 0);
                return reader.read(0, param);
            } finally {
                reader.dispose();
            }
        }
    }

    /**
     * Draws {@code source} -- possibly subsampled -- rotated and flipped by {@code tag}, scaled to
     * the size an {@code orientedWidth x orientedHeight} original takes with its longest edge at
     * most {@code edge}, onto white.
     */
    private static BufferedImage upright(BufferedImage source, int tag, int orientedWidth, int orientedHeight, int edge) {
        int longest = Math.max(orientedWidth, orientedHeight);
        int width = orientedWidth;
        int height = orientedHeight;
        if (longest > edge) {
            width = orientedWidth == longest ? edge : scaledShort(orientedWidth, edge, longest);
            height = orientedHeight == longest ? edge : scaledShort(orientedHeight, edge, longest);
        }
        boolean quarterTurn = tag >= 5;
        int rasterWidth = quarterTurn ? source.getHeight() : source.getWidth();
        int rasterHeight = quarterTurn ? source.getWidth() : source.getHeight();

        var result = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = result.createGraphics();
        try {
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, width, height);
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.scale((double) width / rasterWidth, (double) height / rasterHeight);
            g.transform(orientation(tag, source.getWidth(), source.getHeight()));
            g.drawImage(source, 0, 0, null);
        } finally {
            g.dispose();
        }
        return result;
    }

    private static int scaledShort(int shortEdge, int edge, int longEdge) {
        return Math.max(1, (int) Math.round(shortEdge * edge / (double) longEdge));
    }

    /** The transform taking a {@code width x height} raster to the Exif-upright one. */
    private static AffineTransform orientation(int tag, int width, int height) {
        return switch (tag) {
            case 2 -> new AffineTransform(-1, 0, 0, 1, width, 0);
            case 3 -> new AffineTransform(-1, 0, 0, -1, width, height);
            case 4 -> new AffineTransform(1, 0, 0, -1, 0, height);
            case 5 -> new AffineTransform(0, 1, 1, 0, 0, 0);
            case 6 -> new AffineTransform(0, 1, -1, 0, height, 0);
            case 7 -> new AffineTransform(0, -1, -1, 0, height, width);
            case 8 -> new AffineTransform(0, -1, 1, 0, 0, width);
            default -> new AffineTransform();
        };
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
