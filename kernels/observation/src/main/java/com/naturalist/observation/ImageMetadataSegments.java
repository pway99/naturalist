package com.naturalist.observation;

import com.naturalist.observability.Level;
import com.naturalist.observability.MethodObserver;
import com.naturalist.observability.Observer;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Optional;
import java.util.Set;

/**
 * Walks an upload's container block by block, reads the few Exif tags the app cares about, and
 * strips the rest. The Exif block is a TIFF structure wherever it lives: in a JPEG {@code APP1}
 * segment after the {@code Exif\0\0} header, in a PNG {@code eXIf} chunk, or in a WebP
 * {@code EXIF} chunk. Reading and stripping share one walk, {@link Blocks}, so they never
 * disagree about where a block starts, where it ends, or where the image does.
 *
 * <p>Every read is bounds-checked against the bytes actually present, so a truncated, corrupt or
 * hostile file never throws. A read that loses its footing comes back empty; a strip keeps what
 * it walked and decides about the rest by what the walk broke on (see {@link #withoutMetadata}).
 * Either is observed as a violation of {@code wellFormed} through the caller's
 * {@link MethodObserver}, so it is scoped to the {@link ImageUpload} method a reader can open. An
 * image that simply carries no Exif, or Exif without the tag asked for, is empty and not
 * observed.
 */
final class ImageMetadataSegments {

    private static final Observer observer = Observer.forClass(ImageMetadataSegments.class);

    private static final String APP0 = "FFE0";

    private static final String APP1 = "FFE1";

    private static final String APP13 = "FFED";

    private static final String COM = "FFFE";

    /**
     * The JPEG segments a strip drops: every {@code COM}, and every {@code APPn} but {@code APP0}
     * (JFIF), {@code APP2} (ICC profile) and {@code APP14} (Adobe colour transform), which a
     * decoder reads. The rest carry what a camera, an editor or a phone says about the picture:
     * Exif and XMP in {@code APP1}, JUMBF and C2PA content credentials (which can embed Exif
     * again) in {@code APP11}, maker text in {@code APP12}, IPTC in {@code APP13}, and others.
     */
    private static final Set<String> JPEG_METADATA = Set.of(APP1, "FFE3", "FFE4", "FFE5", "FFE6", "FFE7",
            "FFE8", "FFE9", "FFEA", "FFEB", "FFEC", APP13, "FFEF", COM);

    private static final String SOS = "FFDA";

    private static final String EOI = "FFD9";

    /** The entropy-coded data after a start-of-scan header, as one block. */
    private static final String SCAN = "scan";

    private static final byte[] EXIF_HEADER = {'E', 'x', 'i', 'f', 0, 0};

    private static final byte[] JPEG_SOI = {(byte) 0xFF, (byte) 0xD8};

    private static final byte[] RIFF = {'R', 'I', 'F', 'F'};

    private static final byte[] WEBP = {'W', 'E', 'B', 'P'};

    private static final byte[] PNG_SIGNATURE = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};

    /** The {@code VP8X} flag bits that announce an {@code EXIF} (bit 3) and an {@code XMP } (bit 2) chunk. */
    private static final int VP8X_EXIF_AND_XMP = 0b1100;

    private static final int TAG_ORIENTATION = 0x0112;

    private static final int TAG_GPS_IFD = 0x8825;

    private static final int TYPE_ASCII = 2;

    private static final int TYPE_SHORT = 3;

    private static final int TYPE_RATIONAL = 5;

    private ImageMetadataSegments() {
    }

    /**
     * The TIFF payload of the image's Exif block: a JPEG Exif {@code APP1} before the first scan,
     * a PNG {@code eXIf} chunk or a WebP {@code EXIF} chunk. Empty for any other format and for an
     * image without one.
     */
    static Optional<byte[]> exifTiff(byte[] image, MethodObserver m) {
        Blocks blocks = Blocks.of(image, m);
        if (blocks == null) return Optional.empty();
        for (Block block; (block = blocks.next()) != null; ) {
            if (block.type().equals(SOS)) return Optional.empty();
            Optional<byte[]> tiff = blocks.exif(block);
            if (tiff.isPresent()) return tiff;
        }
        return blocks.stop().broke() ? malformed(m) : Optional.empty();
    }

    /** The orientation tag ({@code 0x0112}) of IFD0, the value a viewer rotates the picture by. */
    static Optional<Integer> orientation(byte[] tiff, MethodObserver m) {
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
     * IFD0 has no GPS IFD or the GPS IFD lacks either coordinate. A fix of exactly 0°, 0° is what
     * some firmware writes when it has no fix at all, so it is observed as {@code noFix} and
     * returned empty rather than placing the picture in the Gulf of Guinea. A fix that decodes to
     * no point on the globe, which is what a zero denominator or a corrupt rational gives, is
     * observed against the {@link Coordinates} invariants and returned empty, never handed on.
     */
    static Optional<Coordinates> gps(byte[] tiff, MethodObserver m) {
        Coordinates fix;
        try {
            Bytes t = tiffBytes(tiff);
            long pointer = find(t, t.u32(4), TAG_GPS_IFD);
            if (pointer < 0) return Optional.empty();
            long gpsIfd = t.u32(pointer + 8);
            Optional<Double> lat = coordinate(t, gpsIfd, 1, 'N', 'S');
            Optional<Double> lon = coordinate(t, gpsIfd, 3, 'E', 'W');
            if (lat.isEmpty() || lon.isEmpty()) return Optional.empty();
            if (lat.get() == 0.0 && lon.get() == 0.0) {
                violation(m, "noFix");
                return Optional.empty();
            }
            fix = new Coordinates(lat.get(), lon.get());
        } catch (Malformed e) {
            return malformed(m);
        }
        if (!m.observable(fix, "fix").observe(Level.INFO).violations().isEmpty()) return Optional.empty();
        return Optional.of(fix);
    }

    /**
     * The image with its metadata blocks removed and every other block copied byte for byte: a
     * JPEG keeps only {@code APP0} (JFIF), {@code APP2} (ICC) and {@code APP14} (Adobe) of its
     * {@code APPn} segments, losing {@code APP1} (Exif and XMP), {@code APP11} (JUMBF and C2PA),
     * {@code APP13} (IPTC) and every other one, and every {@code COM}, before the scan or after
     * it, while every codec segment (tables, frame and scan headers, restart interval) stays; a
     * PNG loses its {@code eXIf}, {@code tEXt}, {@code zTXt} and {@code iTXt} chunks; a WebP its
     * {@code EXIF} and {@code XMP } chunks, with the RIFF size and the {@code VP8X} flags brought
     * into line. A JPEG keeps its Exif orientation, rewritten as
     * {@link #orientationOnlyApp1} after the {@code APP0} segments that open the file, or straight
     * after {@code SOI} when none does; that is where a stripped image's own {@code APP1} sits, so
     * stripping it again changes nothing.
     *
     * <p>The image ends at its terminator: JPEG {@code EOI}, PNG {@code IEND}, the WebP RIFF
     * extent. Whatever follows, a phone's trailer or a motion photo's video, is dropped and
     * observed as {@code trailer}. Between JPEG segments, {@code FF} fill bytes are kept with the
     * segment they precede, and bytes that are not a marker at all are skipped to the next one,
     * the way a decoder resynchronises, dropped and observed as {@code junk}. When the walk breaks
     * on a block, it is observed as {@code wellFormed}; the rest of the image from that block on
     * is dropped when the block is a metadata type and copied as it stands otherwise, since
     * nothing says it is not image data.
     * So a strip can leave bytes that no longer sniff as an image, when the input was not really
     * one; the caller validates what comes back. Returns {@code image} itself when there was
     * nothing to remove, and for any other format.
     */
    static byte[] withoutMetadata(byte[] image, MethodObserver m) {
        Blocks blocks = Blocks.of(image, m);
        if (blocks == null) return image;
        Format format = blocks.format();
        byte[] window = blocks.image();
        ByteArrayOutputStream leadingApp0 = new ByteArrayOutputStream();
        ByteArrayOutputStream kept = new ByteArrayOutputStream(window.length);
        boolean removed = false;
        boolean exifSeen = false;
        Optional<Integer> orientation = Optional.empty();
        int flagsAt = -1;
        for (Block block; (block = blocks.next()) != null; ) {
            if (format.metadata().contains(block.type())) {
                removed = true;
                Optional<byte[]> tiff = format == Format.JPEG && !exifSeen ? blocks.exif(block) : Optional.empty();
                if (tiff.isPresent()) {
                    exifSeen = true;
                    orientation = orientation(tiff.get(), m);
                }
                continue;
            }
            if (block.type().equals(APP0) && kept.size() == 0) {
                block.copyTo(window, leadingApp0);
                continue;
            }
            if (block.type().equals("VP8X") && block.payloadEnd() > block.payload()) flagsAt = kept.size() + 8;
            block.copyTo(window, kept);
        }
        boolean droppedRest = finish(blocks.stop(), window, image.length, format, kept, m);
        if (!removed && !droppedRest && !blocks.skippedJunk()) return image;

        ByteArrayOutputStream out = new ByteArrayOutputStream(window.length);
        out.write(window, 0, (int) blocks.start());
        out.writeBytes(leadingApp0.toByteArray());
        orientation.filter(o -> o >= 2 && o <= 8).map(ImageMetadataSegments::orientationOnlyApp1)
                .ifPresent(out::writeBytes);
        int keptAt = out.size();
        out.writeBytes(kept.toByteArray());
        byte[] stripped = out.toByteArray();
        if (format == Format.WEBP) {
            if (flagsAt >= 0) stripped[keptAt + flagsAt] &= (byte) ~VP8X_EXIF_AND_XMP;
            ByteBuffer.wrap(stripped).order(ByteOrder.LITTLE_ENDIAN).putInt(4, stripped.length - 8);
        }
        return stripped;
    }

    /**
     * Settles what follows the last block a strip walked: the rest of a broken image (copied, or
     * dropped when the walk broke on a metadata block) and any bytes past the image's end (always
     * dropped). True when something was dropped.
     */
    private static boolean finish(Stop stop, byte[] window, int length, Format format,
                                  ByteArrayOutputStream kept, MethodObserver m) {
        boolean dropped = false;
        int imageEnd = window.length;
        if (stop.broke()) {
            violation(m, "wellFormed");
            if (stop.brokenOn() != null && format.metadata().contains(stop.brokenOn())) dropped = true;
            else kept.write(window, (int) stop.at(), window.length - (int) stop.at());
        } else {
            imageEnd = (int) stop.at();
        }
        if (length > imageEnd) {
            violation(m, "trailer");
            dropped = true;
        }
        return dropped;
    }

    /**
     * The Exif {@code APP1} a stripped JPEG carries: the {@code Exif\0\0} header and a big-endian
     * TIFF block whose IFD0 holds one entry, the orientation, and links to no further IFD. Its
     * orientation reads back as {@code orientation}, which is what makes a second strip a no-op.
     */
    static byte[] orientationOnlyApp1(int orientation) {
        observer.arguments("orientationOnlyApp1", c -> c.inRange(orientation, 1, 8, "orientation"))
                .throwWhenInvalid();
        int tiffLength = 8 + 2 + 12 + 4;
        return ByteBuffer.allocate(4 + EXIF_HEADER.length + tiffLength).order(ByteOrder.BIG_ENDIAN)
                .put((byte) 0xFF).put((byte) 0xE1).putShort((short) (2 + EXIF_HEADER.length + tiffLength))
                .put(EXIF_HEADER)
                .put((byte) 'M').put((byte) 'M').putShort((short) 42).putInt(8)
                .putShort((short) 1)
                .putShort((short) TAG_ORIENTATION).putShort((short) TYPE_SHORT).putInt(1)
                .putShort((short) orientation).putShort((short) 0)
                .putInt(0)
                .array();
    }

    // ---- containers ---------------------------------------------------------------------------

    /** The three containers, each with the block types that carry metadata, which a strip drops. */
    private enum Format {
        JPEG(JPEG_METADATA),
        PNG(Set.of("eXIf", "tEXt", "zTXt", "iTXt")),
        WEBP(Set.of("EXIF", "XMP "));

        private final Set<String> metadata;

        Format(Set<String> metadata) {
            this.metadata = metadata;
        }

        Set<String> metadata() {
            return metadata;
        }
    }

    /**
     * One block of a container: a JPEG segment (its type the marker, {@code FFE1}) or the
     * entropy-coded data of a scan, a PNG chunk, a RIFF chunk. It spans {@code start} to
     * {@code end}, header, CRC and padding included; its payload is {@code payload} to
     * {@code payloadEnd}.
     */
    private record Block(String type, long start, long payload, long payloadEnd, long end) {

        void copyTo(byte[] image, ByteArrayOutputStream out) {
            out.write(image, (int) start, (int) (end - start));
        }
    }

    /**
     * Where a walk ended: at the end of the image ({@code at} is one past its terminator, or the
     * end of the bytes), or broken at the block starting at {@code at}, whose type is
     * {@code brokenOn} when the walk read that far, else {@code null}.
     */
    private record Stop(long at, boolean broke, String brokenOn) {
    }

    /**
     * The blocks of one container in file order, as an iterator: {@link #next} hands out each block
     * once it is known to lie within the bytes, and {@code null} when the walk has ended, after
     * which {@link #stop} says how. The walk never throws.
     *
     * <ul>
     *   <li>JPEG, from offset 2: length-bearing segments; after each start-of-scan header, the
     *   entropy-coded data up to the next marker (stuffed {@code FF 00}, restart markers and fill
     *   bytes are part of it) as one {@link #SCAN} block; {@code EOI} ends the image. Before a
     *   marker, any number of {@code FF} fill bytes belong to the block the marker opens (T.81
     *   B.1.1.2); bytes that do not open a marker are skipped to the next {@code FF xx} with
     *   {@code xx} neither {@code 00} nor {@code FF}, as libjpeg does, and observed as
     *   {@code junk}, so whatever follows them is still walked. A restart marker or {@code TEM}
     *   between segments carries no length and is a block of its own, as a decoder skips it. A
     *   run of junk with no marker after it, a length below 2, a segment running past the bytes,
     *   or bytes ending before {@code EOI} other than inside a scan break the walk.</li>
     *   <li>PNG, after the signature: chunks (length, type, data, CRC) up to {@code IEND}, which
     *   ends the image; bytes ending before it break the walk.</li>
     *   <li>WebP, after the {@code WEBP} form type: chunks (four-character code, size, payload, a
     *   padding byte after an odd payload) to the RIFF extent, {@code 8 +} the RIFF size, which is
     *   where the image ends.</li>
     * </ul>
     */
    private static final class Blocks {

        private final Format format;

        private final Bytes b;

        private final MethodObserver m;

        private boolean skippedJunk;

        private final long start;

        private long pos;

        private boolean inScan;

        private String pending;

        private Stop stop;

        private Blocks(Format format, Bytes b, long start, MethodObserver m) {
            this.format = format;
            this.b = b;
            this.start = start;
            this.m = m;
            this.pos = start;
        }

        /**
         * The walk over {@code image}, or {@code null} when it is not a JPEG, PNG or WebP; what it
         * skips is observed through {@code m}.
         */
        static Blocks of(byte[] image, MethodObserver m) {
            Bytes bytes = new Bytes(image == null ? new byte[0] : image, ByteOrder.BIG_ENDIAN);
            if (bytes.startsWith(0, JPEG_SOI)) return new Blocks(Format.JPEG, bytes, JPEG_SOI.length, m);
            if (bytes.startsWith(0, PNG_SIGNATURE)) return new Blocks(Format.PNG, bytes, PNG_SIGNATURE.length, m);
            if (bytes.startsWith(0, RIFF) && bytes.startsWith(8, WEBP)) {
                Bytes le = bytes.withOrder(ByteOrder.LITTLE_ENDIAN);
                long extent = Math.max(12, Math.min(8 + le.u32(4), image.length));
                byte[] riff = extent < image.length ? Arrays.copyOf(image, (int) extent) : image;
                return new Blocks(Format.WEBP, new Bytes(riff, ByteOrder.LITTLE_ENDIAN), 12, m);
            }
            return null;
        }

        Format format() {
            return format;
        }

        /** The bytes the walk covers: the whole upload, or a WebP's RIFF extent. */
        byte[] image() {
            return b.data();
        }

        /** Where the first block starts: after {@code SOI}, the PNG signature or the RIFF header. */
        long start() {
            return start;
        }

        Stop stop() {
            return stop;
        }

        /** True when the walk skipped bytes that were neither a block nor fill: the strip drops them. */
        boolean skippedJunk() {
            return skippedJunk;
        }

        Block next() {
            if (stop != null) return null;
            try {
                return switch (format) {
                    case JPEG -> nextJpeg();
                    case PNG -> nextPng();
                    case WEBP -> nextWebp();
                };
            } catch (Malformed e) {
                stop = new Stop(pos, true, pending);
                return null;
            }
        }

        /**
         * The TIFF block {@code block} carries, when it is the format's Exif block: an
         * {@code APP1} opening with {@code Exif\0\0}, an {@code eXIf} or an {@code EXIF} chunk.
         * Neither of the last two calls for the JPEG header, but some writers repeat it there; it
         * is skipped.
         */
        Optional<byte[]> exif(Block block) {
            boolean headed = block.payloadEnd() - block.payload() >= EXIF_HEADER.length
                    && b.startsWith(block.payload(), EXIF_HEADER);
            long from = headed ? block.payload() + EXIF_HEADER.length : block.payload();
            boolean exif = switch (format) {
                case JPEG -> block.type().equals(APP1) && headed;
                case PNG -> block.type().equals("eXIf");
                case WEBP -> block.type().equals("EXIF");
            };
            return exif ? Optional.of(b.slice(from, block.payloadEnd())) : Optional.empty();
        }

        private Block nextJpeg() {
            if (inScan) {
                inScan = false;
                Block scan = bare(SCAN, scanEnd(pos));
                if (scan.end() == b.data().length) stop = new Stop(scan.end(), false, null);
                return scan;
            }
            pending = null;
            long at = pos;
            while (at + 1 < b.data().length && b.u8(at) == 0xFF && b.u8(at + 1) == 0xFF) at++;
            if (b.u8(at) != 0xFF || b.u8(at + 1) == 0x00) {
                long marker = nextMarker(at);
                if (marker < 0) throw new Malformed();
                violation(m, "junk");
                skippedJunk = true;
                pos = marker;
                at = marker;
            }
            int code = b.u8(at + 1);
            pending = String.format("FF%02X", code);
            if (pending.equals(EOI)) {
                Block eoi = bare(EOI, at + 2);
                stop = new Stop(eoi.end(), false, null);
                return eoi;
            }
            if ((code >= 0xD0 && code <= 0xD7) || code == 0x01) return bare(pending, at + 2);
            int length = b.u16(at + 2);
            if (length < 2) throw new Malformed();
            b.require(at + 4, length - 2);
            Block segment = new Block(pending, pos, at + 4, at + 2 + length, at + 2 + length);
            pos = segment.end();
            inScan = pending.equals(SOS);
            return segment;
        }

        /**
         * Where the next marker starts at or after {@code from}: an {@code FF} followed by
         * neither {@code 00} nor {@code FF}, the last of a fill run. {@code -1} when none does.
         */
        private long nextMarker(long from) {
            byte[] d = b.data();
            for (long i = from; i + 1 < d.length; i++) {
                int next = d[(int) i + 1] & 0xFF;
                if ((d[(int) i] & 0xFF) == 0xFF && next != 0x00 && next != 0xFF) return i;
            }
            return -1;
        }

        /** The end of the entropy-coded data from {@code from}: the next marker, or the end of the bytes. */
        private long scanEnd(long from) {
            byte[] d = b.data();
            long i = from;
            while (i + 1 < d.length) {
                if ((d[(int) i] & 0xFF) != 0xFF) {
                    i++;
                    continue;
                }
                int next = d[(int) i + 1] & 0xFF;
                if (next == 0x00 || (next >= 0xD0 && next <= 0xD7)) i += 2;
                else if (next == 0xFF) i++;
                else return i;
            }
            return d.length;
        }

        private Block nextPng() {
            pending = null;
            long length = b.u32(pos);
            pending = b.ascii(pos + 4, 4);
            b.require(pos + 8, length + 4);
            Block chunk = new Block(pending, pos, pos + 8, pos + 8 + length, pos + 8 + length + 4);
            pos = chunk.end();
            if (pending.equals("IEND")) stop = new Stop(chunk.end(), false, null);
            return chunk;
        }

        private Block nextWebp() {
            if (pos >= b.data().length) {
                stop = new Stop(pos, false, null);
                return null;
            }
            pending = null;  // until the type is read, a break is on no known type
            pending = b.ascii(pos, 4);
            long size = b.u32(pos + 4);
            b.require(pos + 8, size);
            long end = Math.min(pos + 8 + size + (size & 1), b.data().length);
            Block chunk = new Block(pending, pos, pos + 8, pos + 8 + size, end);
            pos = end;
            return chunk;
        }

        /** A block without a length field, from here to {@code end}: a bare marker or a scan's data. */
        private Block bare(String type, long end) {
            Block block = new Block(type, pos, pos, end, end);
            pos = end;
            return block;
        }
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
        violation(m, "wellFormed");
        return Optional.empty();
    }

    private static void violation(MethodObserver m, String name) {
        m.state(c -> c.isTrue(false, name)).observe(Level.INFO);
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
