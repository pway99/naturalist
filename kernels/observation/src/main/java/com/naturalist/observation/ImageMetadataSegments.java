package com.naturalist.observation;

import com.naturalist.observability.Level;
import com.naturalist.observability.MethodObserver;
import com.naturalist.observability.Observer;

import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

/**
 * Finds the Exif block in an upload's container and reads the few tags the app cares about. The
 * Exif block is a TIFF structure wherever it lives: in a JPEG {@code APP1} segment after the
 * {@code Exif\0\0} header, in a PNG {@code eXIf} chunk, or in a WebP {@code EXIF} chunk.
 *
 * <p>Every read is bounds-checked against the bytes actually present, so a truncated, corrupt or
 * hostile file never throws: the read comes back empty and is observed as a violation of
 * {@code wellFormed} on {@link ImageUpload}, scoped to the method that gave up. An image that
 * simply carries no Exif, or Exif without the tag asked for, is empty and not observed.
 */
final class ImageMetadataSegments {

    private static final Observer observer = Observer.forClass(ImageUpload.class);

    private static final int SOS = 0xDA;

    private static final int APP1 = 0xE1;

    private static final byte[] EXIF_HEADER = {'E', 'x', 'i', 'f', 0, 0};

    private static final byte[] JPEG_SOI = {(byte) 0xFF, (byte) 0xD8};

    private static final byte[] RIFF = {'R', 'I', 'F', 'F'};

    private static final byte[] WEBP = {'W', 'E', 'B', 'P'};

    private static final byte[] PNG_SIGNATURE = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};

    private static final int TAG_ORIENTATION = 0x0112;

    private static final int TAG_GPS_IFD = 0x8825;

    private static final int TYPE_ASCII = 2;

    private static final int TYPE_SHORT = 3;

    private static final int TYPE_LONG = 4;

    private static final int TYPE_RATIONAL = 5;

    private ImageMetadataSegments() {
    }

    /**
     * The TIFF payload of the image's Exif block: a JPEG Exif {@code APP1}, a PNG {@code eXIf}
     * chunk or a WebP {@code EXIF} chunk. Empty for any other format and for an image without one.
     */
    static Optional<byte[]> exifTiff(byte[] image) {
        MethodObserver m = observer.forMethod("exifTiff");
        try {
            Bytes bytes = new Bytes(image == null ? new byte[0] : image, ByteOrder.BIG_ENDIAN);
            if (bytes.startsWith(0, JPEG_SOI)) return jpegExif(bytes);
            if (bytes.startsWith(0, PNG_SIGNATURE)) return pngExif(bytes);
            if (bytes.startsWith(0, RIFF) && bytes.startsWith(8, WEBP)) {
                return webpExif(bytes.withOrder(ByteOrder.LITTLE_ENDIAN));
            }
            return Optional.empty();
        } catch (Malformed e) {
            return malformed(m);
        }
    }

    /** The orientation tag ({@code 0x0112}) of IFD0, the value a viewer rotates the picture by. */
    static Optional<Integer> orientation(byte[] tiff) {
        MethodObserver m = observer.forMethod("orientation");
        try {
            Bytes t = tiffBytes(tiff);
            long entry = find(t, t.u32(4), TAG_ORIENTATION);
            if (entry < 0 || t.u16(entry + 2) != TYPE_SHORT) return Optional.empty();
            return Optional.of(t.u16(entry + 8));
        } catch (Malformed e) {
            return malformed(m);
        }
    }

    /**
     * The GPS fix: latitude from tags 1 ({@code N}/{@code S}) and 2, longitude from tags 3
     * ({@code E}/{@code W}) and 4, each three rationals of degrees, minutes and seconds. Empty when
     * IFD0 has no GPS IFD or the GPS IFD lacks either coordinate. A fix that decodes to no point on
     * the globe, which is what a zero denominator or a corrupt rational gives, is observed against
     * the {@link Coordinates} invariants and returned empty, never handed on.
     */
    static Optional<Coordinates> gps(byte[] tiff) {
        MethodObserver m = observer.forMethod("gps");
        Coordinates fix;
        try {
            Bytes t = tiffBytes(tiff);
            long pointer = find(t, t.u32(4), TAG_GPS_IFD);
            if (pointer < 0) return Optional.empty();
            long gpsIfd = t.u32(pointer + 8);
            Optional<Double> lat = coordinate(t, gpsIfd, 1, 'N', 'S');
            Optional<Double> lon = coordinate(t, gpsIfd, 3, 'E', 'W');
            if (lat.isEmpty() || lon.isEmpty()) return Optional.empty();
            fix = new Coordinates(lat.get(), lon.get());
        } catch (Malformed e) {
            return malformed(m);
        }
        if (!m.observable(fix, "fix").observe(Level.INFO).violations().isEmpty()) return Optional.empty();
        return Optional.of(fix);
    }

    // ---- containers ---------------------------------------------------------------------------

    /**
     * Walks the segments from offset 2. Stops at start-of-scan, after which there is no more
     * metadata; a marker that is not {@code FF xx}, a length below 2, a segment running past the
     * array or bytes ending before start-of-scan are malformed.
     */
    private static Optional<byte[]> jpegExif(Bytes b) {
        long pos = 2;
        while (true) {
            if (b.u8(pos) != 0xFF) throw new Malformed();
            int marker = b.u8(pos + 1);
            if (marker == SOS) return Optional.empty();
            int length = b.u16(pos + 2);
            if (length < 2) throw new Malformed();
            long payload = pos + 4;
            long end = pos + 2 + length;
            b.require(payload, length - 2);
            if (marker == APP1 && b.startsWith(payload, EXIF_HEADER)) {
                return Optional.of(b.slice(payload + EXIF_HEADER.length, end));
            }
            pos = end;
        }
    }

    /** Walks the chunks after the signature (length, type, data, CRC) up to {@code IEND}. */
    private static Optional<byte[]> pngExif(Bytes b) {
        long pos = PNG_SIGNATURE.length;
        while (true) {
            long length = b.u32(pos);
            String type = b.ascii(pos + 4, 4);
            long data = pos + 8;
            b.require(data, length + 4);
            if (type.equals("eXIf")) return Optional.of(b.slice(data, data + length));
            if (type.equals("IEND")) return Optional.empty();
            pos = data + length + 4;
        }
    }

    /**
     * Walks the RIFF chunks after the {@code WEBP} form type (four-character code, size, payload,
     * a padding byte after an odd payload) to the end of the bytes. Some writers repeat the JPEG
     * {@code Exif\0\0} header inside the chunk; it is skipped.
     */
    private static Optional<byte[]> webpExif(Bytes b) {
        long pos = 12;
        while (pos < b.data().length) {
            String fourCc = b.ascii(pos, 4);
            long size = b.u32(pos + 4);
            long payload = pos + 8;
            b.require(payload, size);
            if (fourCc.equals("EXIF")) {
                long start = b.startsWith(payload, EXIF_HEADER) ? payload + EXIF_HEADER.length : payload;
                return Optional.of(b.slice(start, payload + size));
            }
            pos = payload + size + (size & 1);
        }
        return Optional.empty();
    }

    // ---- TIFF ---------------------------------------------------------------------------------

    /** The block with its byte order read from the {@code II}/{@code MM} header and checked. */
    private static Bytes tiffBytes(byte[] tiff) {
        Bytes b = new Bytes(tiff == null ? new byte[0] : tiff, ByteOrder.BIG_ENDIAN);
        ByteOrder order;
        if (b.startsWith(0, new byte[]{'M', 'M'})) order = ByteOrder.BIG_ENDIAN;
        else if (b.startsWith(0, new byte[]{'I', 'I'})) order = ByteOrder.LITTLE_ENDIAN;
        else throw new Malformed();
        Bytes t = b.withOrder(order);
        if (t.u16(2) != 42) throw new Malformed();
        return t;
    }

    /** The offset of the entry with {@code tag} in the IFD at {@code ifd}, or -1 when absent. */
    private static long find(Bytes t, long ifd, int tag) {
        int count = t.u16(ifd);
        for (int i = 0; i < count; i++) {
            long entry = ifd + 2 + 12L * i;
            if (t.u16(entry) == tag) return entry;
        }
        return -1;
    }

    /**
     * One coordinate from the GPS IFD: the hemisphere letter at {@code refTag} and the three
     * rationals at {@code refTag + 1}, negated for {@code negative}. Empty when either tag is
     * absent; a letter that is neither hemisphere, or a tag of the wrong type, is malformed.
     */
    private static Optional<Double> coordinate(Bytes t, long gpsIfd, int refTag, char positive, char negative) {
        long ref = find(t, gpsIfd, refTag);
        long value = find(t, gpsIfd, refTag + 1);
        if (ref < 0 || value < 0) return Optional.empty();
        if (t.u16(ref + 2) != TYPE_ASCII || t.u16(value + 2) != TYPE_RATIONAL || t.u32(value + 4) < 3) {
            throw new Malformed();
        }
        int letter = t.u8(ref + 8);
        if (letter != positive && letter != negative) throw new Malformed();
        long at = t.u32(value + 8);
        double degrees = rational(t, at) + rational(t, at + 8) / 60.0 + rational(t, at + 16) / 3600.0;
        return Optional.of(letter == negative ? -degrees : degrees);
    }

    private static double rational(Bytes t, long at) {
        return (double) t.u32(at) / (double) t.u32(at + 4);
    }

    private static <T> Optional<T> malformed(MethodObserver m) {
        m.state(c -> c.isTrue(false, "wellFormed")).observe(Level.INFO);
        return Optional.empty();
    }

    // ---- reading ------------------------------------------------------------------------------

    /** Raised by a read past the bytes present or a structure that cannot be what it claims. */
    private static final class Malformed extends RuntimeException {
        Malformed() {
            super(null, null, false, false);
        }
    }

    /**
     * Unsigned reads in one byte order over an array; offsets are
     * {@code long} so a 32-bit offset from the file can never overflow, and every read outside the
     * window is {@link Malformed}.
     */
    private record Bytes(byte[] data, ByteOrder order) {

        Bytes withOrder(ByteOrder other) {
            return new Bytes(data, other);
        }

        void require(long at, long count) {
            if (at < 0 || count < 0 || at + count > data.length) throw new Malformed();
        }

        int u8(long at) {
            require(at, 1);
            return data[(int) at] & 0xFF;
        }

        int u16(long at) {
            require(at, 2);
            int a = u8(at);
            int b = u8(at + 1);
            return order == ByteOrder.BIG_ENDIAN ? (a << 8) | b : (b << 8) | a;
        }

        long u32(long at) {
            require(at, 4);
            long hi = u16(at);
            long lo = u16(at + 2);
            return order == ByteOrder.BIG_ENDIAN ? (hi << 16) | lo : (lo << 16) | hi;
        }

        boolean startsWith(long at, byte[] prefix) {
            if (at < 0 || at + prefix.length > data.length) return false;
            for (int i = 0; i < prefix.length; i++) {
                if (data[(int) at + i] != prefix[i]) return false;
            }
            return true;
        }

        String ascii(long at, int count) {
            require(at, count);
            return new String(data, (int) at, count, StandardCharsets.US_ASCII);
        }

        byte[] slice(long from, long to) {
            require(from, to - from);
            byte[] out = new byte[(int) (to - from)];
            System.arraycopy(data, (int) from, out, 0, out.length);
            return out;
        }
    }
}
