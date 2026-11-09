package com.naturalist.observation.test;

import com.naturalist.observation.Coordinates;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.zip.CRC32;
import java.util.zip.Deflater;

/**
 * Byte-level JPEG, PNG and WebP images, with and without the metadata a camera or an editor
 * writes, for tests that read or strip it. Every container is laid out as its specification says:
 * JPEG segments carry their own lengths, PNG chunks their CRCs, WebP chunks their padding and the
 * {@code VP8X} flags the chunks that follow. The PNG and WebP decode as a one-pixel picture; the
 * JPEG only walks as segments, which is all a metadata reader looks at.
 *
 * <p>The three formats share one Exif block, {@link #tiff}, so a test can put the same GPS fix or
 * orientation in any of them.
 */
public final class ImageFixtures {

    /** The fix {@link #webp} carries when asked for Exif. */
    public static final Coordinates PORTLAND = new Coordinates(45.52, -122.68);

    private static final byte[] EXIF_HEADER = "Exif\0\0".getBytes(StandardCharsets.US_ASCII);

    private static final byte[] XMP_NAMESPACE = "http://ns.adobe.com/xap/1.0/\0".getBytes(StandardCharsets.US_ASCII);

    private static final byte[] PNG_SIGNATURE = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};

    /** A one-pixel lossless WebP bitstream with alpha; thirteen bytes, so its chunk is padded. */
    private static final byte[] VP8L_ONE_PIXEL =
            {0x2F, 0, 0, 0, 0x10, 0x07, 0x10, 0x11, 0x11, (byte) 0x88, (byte) 0x88, (byte) 0xFE, 0x07};

    /** The {@code VP8X} flag the alpha in {@link #VP8L_ONE_PIXEL} requires. */
    private static final int ALPHA = 0x10;

    private static final int TIFF_ASCII = 2;

    private static final int TIFF_SHORT = 3;

    private static final int TIFF_LONG = 4;

    private static final int TIFF_RATIONAL = 5;

    private ImageFixtures() {
    }

    // ---- JPEG ---------------------------------------------------------------------------------

    /**
     * A JPEG: {@code SOI}, the given segments in order, then a start-of-scan with four bytes of
     * entropy-coded data and {@code EOI}.
     */
    public static byte[] jpeg(byte[]... segments) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.writeBytes(new byte[]{(byte) 0xFF, (byte) 0xD8});
        for (byte[] segment : segments) out.writeBytes(segment);
        out.writeBytes(new byte[]{(byte) 0xFF, (byte) 0xDA, 0x00, 0x02, 0x12, 0x34, 0x56, 0x78});
        out.writeBytes(new byte[]{(byte) 0xFF, (byte) 0xD9});
        return out.toByteArray();
    }

    /** The JFIF {@code APP0} segment: version 1.01, no units, 1:1 density, no thumbnail. */
    public static byte[] app0Jfif() {
        return segment(0xE0, new byte[]{'J', 'F', 'I', 'F', 0, 1, 1, 0, 0, 1, 0, 1, 0, 0});
    }

    /**
     * An Exif {@code APP1} segment around {@link #tiff}: orientation when given, a GPS fix when
     * either coordinate is given.
     */
    public static byte[] app1Exif(ByteOrder order, Integer orientation, Double lat, Double lon) {
        return segment(0xE1, concat(EXIF_HEADER, tiff(order, orientation, lat, lon)));
    }

    /** An XMP {@code APP1} segment carrying a one-line description. */
    public static byte[] app1Xmp() {
        return segment(0xE1, concat(XMP_NAMESPACE, xmpPacket()));
    }

    /** An ICC profile {@code APP2} segment: one part of one, a profile with a header and no tags. */
    public static byte[] app2Icc() {
        ByteBuffer profile = ByteBuffer.allocate(132);
        profile.putInt(0, 132);
        profile.put(36, "acsp".getBytes(StandardCharsets.US_ASCII));
        return segment(0xE2, concat("ICC_PROFILE\0".getBytes(StandardCharsets.US_ASCII), new byte[]{1, 1}, profile.array()));
    }

    /** A Photoshop {@code APP13} segment holding one IPTC resource with a caption. */
    public static byte[] app13Iptc() {
        byte[] caption = "a beetle on a leaf".getBytes(StandardCharsets.US_ASCII);
        byte[] iptc = concat(new byte[]{0x1C, 0x02, 0x78, 0x00, (byte) caption.length}, caption);
        ByteBuffer resource = ByteBuffer.allocate(12 + iptc.length + iptc.length % 2);
        resource.put("8BIM".getBytes(StandardCharsets.US_ASCII)).putShort((short) 0x0404).putShort((short) 0)
                .putInt(iptc.length).put(iptc);
        return segment(0xED, concat("Photoshop 3.0\0".getBytes(StandardCharsets.US_ASCII), resource.array()));
    }

    /** The Adobe {@code APP14} segment: DCT version 100, YCbCr transform. */
    public static byte[] app14Adobe() {
        return segment(0xEE, new byte[]{'A', 'd', 'o', 'b', 'e', 0, 100, 0, 0, 0, 0, 1});
    }

    /** A {@code COM} segment holding {@code text} as UTF-8. */
    public static byte[] com(String text) {
        return segment(0xFE, text.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * A length-bearing JPEG segment: {@code FF}, {@code marker}, the big-endian length (payload
     * plus the two length bytes) and {@code payload}.
     */
    public static byte[] segment(int marker, byte[] payload) {
        return ByteBuffer.allocate(4 + payload.length)
                .put((byte) 0xFF).put((byte) marker).putShort((short) (payload.length + 2)).put(payload)
                .array();
    }

    // ---- PNG ----------------------------------------------------------------------------------

    /** A PNG: the signature, then the given chunks in order. */
    public static byte[] png(byte[]... chunks) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.writeBytes(PNG_SIGNATURE);
        for (byte[] chunk : chunks) out.writeBytes(chunk);
        return out.toByteArray();
    }

    /** A PNG chunk: length, type, data and the CRC-32 of type and data. */
    public static byte[] chunk(String type, byte[] data) {
        byte[] name = type.getBytes(StandardCharsets.US_ASCII);
        CRC32 crc = new CRC32();
        crc.update(name);
        crc.update(data);
        return ByteBuffer.allocate(12 + data.length)
                .putInt(data.length).put(name).put(data).putInt((int) crc.getValue())
                .array();
    }

    /** The header of a one-pixel, 8-bit truecolour, non-interlaced image. */
    public static byte[] ihdr() {
        return chunk("IHDR", ByteBuffer.allocate(13).putInt(1).putInt(1).put((byte) 8).put((byte) 2).array());
    }

    /** The image data for {@link #ihdr}: one green pixel, zlib-compressed. */
    public static byte[] idat() {
        Deflater deflater = new Deflater();
        deflater.setInput(new byte[]{0, 0x2E, 0x7D, 0x32});
        deflater.finish();
        byte[] buffer = new byte[64];
        int length = deflater.deflate(buffer);
        deflater.end();
        byte[] compressed = new byte[length];
        System.arraycopy(buffer, 0, compressed, 0, length);
        return chunk("IDAT", compressed);
    }

    /** The closing chunk. */
    public static byte[] iend() {
        return chunk("IEND", new byte[0]);
    }

    /** An {@code eXIf} chunk carrying {@code tiff} as it stands, with no Exif header. */
    public static byte[] exif(byte[] tiff) {
        return chunk("eXIf", tiff);
    }

    /** A {@code tEXt} chunk: Latin-1 keyword, a NUL, Latin-1 text. */
    public static byte[] text(String key, String value) {
        return chunk("tEXt", concat(key.getBytes(StandardCharsets.ISO_8859_1), new byte[]{0},
                value.getBytes(StandardCharsets.ISO_8859_1)));
    }

    // ---- WebP ---------------------------------------------------------------------------------

    /**
     * A one-pixel extended WebP: {@code VP8X} with the alpha, Exif and XMP flags set to match, a lossless
     * {@code VP8L} image, then an {@code EXIF} chunk carrying {@link #PORTLAND} and an {@code XMP }
     * chunk, each only when asked for. The image and the XMP payloads are odd in length, so their
     * chunks carry a padding byte, and the RIFF size counts it.
     */
    public static byte[] webp(boolean exif, boolean xmp) {
        ByteArrayOutputStream chunks = new ByteArrayOutputStream();
        int flags = ALPHA | (exif ? 0x08 : 0) | (xmp ? 0x04 : 0);
        chunks.writeBytes(riffChunk("VP8X", new byte[]{(byte) flags, 0, 0, 0, 0, 0, 0, 0, 0, 0}));
        chunks.writeBytes(riffChunk("VP8L", VP8L_ONE_PIXEL));
        if (exif) {
            chunks.writeBytes(riffChunk("EXIF",
                    tiff(ByteOrder.BIG_ENDIAN, null, PORTLAND.latitude(), PORTLAND.longitude())));
        }
        if (xmp) chunks.writeBytes(riffChunk("XMP ", xmpPacket()));
        byte[] body = chunks.toByteArray();
        return ByteBuffer.allocate(12 + body.length).order(ByteOrder.LITTLE_ENDIAN)
                .put("RIFF".getBytes(StandardCharsets.US_ASCII)).putInt(4 + body.length)
                .put("WEBP".getBytes(StandardCharsets.US_ASCII)).put(body)
                .array();
    }

    private static byte[] riffChunk(String fourCc, byte[] payload) {
        return ByteBuffer.allocate(8 + payload.length + payload.length % 2).order(ByteOrder.LITTLE_ENDIAN)
                .put(fourCc.getBytes(StandardCharsets.US_ASCII)).putInt(payload.length).put(payload)
                .array();
    }

    // ---- Exif ---------------------------------------------------------------------------------

    /**
     * A TIFF block as Exif lays it out: header in {@code order}, then IFD0 with the orientation
     * tag ({@code 0x0112}) when given and a GPS IFD pointer ({@code 0x8825}) when either coordinate
     * is given, then that GPS IFD with latitude (tags 1 and 2) and longitude (tags 3 and 4) as
     * hemisphere letters and degree-minute-second rationals, seconds to the micro-second.
     */
    public static byte[] tiff(ByteOrder order, Integer orientation, Double lat, Double lon) {
        boolean gps = lat != null || lon != null;
        int ifd0Entries = (orientation != null ? 1 : 0) + (gps ? 1 : 0);
        int gpsEntries = (lat != null ? 2 : 0) + (lon != null ? 2 : 0);
        int gpsIfd = 8 + ifdSize(ifd0Entries);
        int gpsData = gpsIfd + ifdSize(gpsEntries);
        int size = gps ? gpsData + 24 * (gpsEntries / 2) : gpsIfd;

        ByteBuffer b = ByteBuffer.allocate(size).order(order);
        b.put((order == ByteOrder.BIG_ENDIAN ? "MM" : "II").getBytes(StandardCharsets.US_ASCII))
                .putShort((short) 42).putInt(8);

        b.putShort((short) ifd0Entries);
        if (orientation != null) entry(b, 0x0112, TIFF_SHORT).putShort(orientation.shortValue()).putShort((short) 0);
        if (gps) entry(b, 0x8825, TIFF_LONG).putInt(gpsIfd);
        b.putInt(0);

        if (gps) {
            b.putShort((short) gpsEntries);
            int data = gpsData;
            if (lat != null) {
                hemisphere(entry(b, 1, TIFF_ASCII), lat >= 0 ? 'N' : 'S');
                entry(b, 2, TIFF_RATIONAL).putInt(data);
                data += 24;
            }
            if (lon != null) {
                hemisphere(entry(b, 3, TIFF_ASCII), lon >= 0 ? 'E' : 'W');
                entry(b, 4, TIFF_RATIONAL).putInt(data);
            }
            b.putInt(0);
            if (lat != null) degreesMinutesSeconds(b, lat);
            if (lon != null) degreesMinutesSeconds(b, lon);
        }
        return b.array();
    }

    private static int ifdSize(int entries) {
        return 2 + 12 * entries + 4;
    }

    /** Writes an entry's tag, type and count; the caller writes its four value bytes. */
    private static ByteBuffer entry(ByteBuffer b, int tag, int type) {
        int count = switch (type) {
            case TIFF_ASCII -> 2;
            case TIFF_RATIONAL -> 3;
            default -> 1;
        };
        return b.putShort((short) tag).putShort((short) type).putInt(count);
    }

    private static void hemisphere(ByteBuffer b, char letter) {
        b.put((byte) letter).put((byte) 0).putShort((short) 0);
    }

    private static void degreesMinutesSeconds(ByteBuffer b, double decimal) {
        double abs = Math.abs(decimal);
        long degrees = (long) Math.floor(abs);
        double minutes = (abs - degrees) * 60;
        long wholeMinutes = (long) Math.floor(minutes);
        long microSeconds = Math.round((minutes - wholeMinutes) * 60 * 1_000_000);
        b.putInt((int) degrees).putInt(1)
                .putInt((int) wholeMinutes).putInt(1)
                .putInt((int) microSeconds).putInt(1_000_000);
    }

    // ---- shared -------------------------------------------------------------------------------

    /** An XMP packet, odd in length so a WebP chunk around it needs padding. */
    private static byte[] xmpPacket() {
        String packet = "<x:xmpmeta xmlns:x=\"adobe:ns:meta/\"><rdf:RDF"
                + " xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\"><rdf:Description"
                + " xmlns:dc=\"http://purl.org/dc/elements/1.1/\" dc:description=\"a beetle\"/>"
                + "</rdf:RDF></x:xmpmeta>";
        byte[] bytes = packet.getBytes(StandardCharsets.UTF_8);
        return bytes.length % 2 == 1 ? bytes : concat(bytes, new byte[]{'\n'});
    }

    private static byte[] concat(byte[]... parts) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        for (byte[] part : parts) out.writeBytes(part);
        return out.toByteArray();
    }
}
