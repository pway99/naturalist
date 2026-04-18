package com.naturalist.data;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.naturalist.ddd.NamedValue;

/**
 * A typed wrapper for a file's base name — the filename as stored, including extension,
 * excluding any directory path.
 *
 * <p>{@code FileName} is a {@link NamedValue}{@code <String>} that belongs in the data
 * infrastructure layer. Any entity that references a stored file (images, documents,
 * sensor exports) uses {@code FileName} rather than a raw {@code String}, eliminating
 * the primitive escape problem and giving the type a stable home for file-related
 * behaviours.
 *
 * <h2>Behaviours</h2>
 * <ul>
 *   <li>{@link #path(String)} — compose this filename with a base directory to produce
 *       the full resource path. The directory separator is handled correctly whether or
 *       not the directory argument carries a trailing slash.</li>
 *   <li>{@link #nameType()} — the file's type token, derived from the extension.
 *       Normalised to upper-case so call sites can match against constants without
 *       case-sensitivity concerns: {@code "HEIC"}, {@code "JPG"}, {@code "PNG"}.
 *       Returns an empty string for filenames with no extension.</li>
 *   <li>{@link #baseName()} — the filename stripped of its extension, useful when
 *       constructing converted or derived filenames.</li>
 * </ul>
 *
 * <h2>JSON round-trip</h2>
 * {@code @JsonValue} on {@link #value()} ensures Jackson serialises this as a plain
 * string. {@code @JsonCreator} on {@link #of(String)} ensures Jackson deserialises a
 * plain string field back into a {@code FileName}. Catalog JSON continues to store
 * filenames as unadorned strings: {@code "resourceName": "IMG_9047.HEIC"}.
 *
 * <h2>Conventions</h2>
 * The stored value is the filename only — never a full path. The directory context is
 * a stable convention of the owning bounded context (e.g. {@code insects/images/} for
 * the insects module) and is not persisted in the catalog. Use {@link #path(String)}
 * to compose at the point of use.
 */
public record FileName(String value) implements NamedValue<String> {

    @JsonCreator
    public static FileName of(String value) {
        return new FileName(value);
    }

    @Override
    @JsonValue
    public String value() {
        return value;
    }

    @Override
    public boolean isValid() {
        return value != null && !value.isBlank();
    }

    /**
     * Compose this filename with a base directory to produce the full resource path.
     *
     * <pre>{@code
     * FileName.of("IMG_9047.HEIC").path("insects/images/")
     *     // → "insects/images/IMG_9047.HEIC"
     *
     * FileName.of("IMG_9047.HEIC").path("insects/images")
     *     // → "insects/images/IMG_9047.HEIC"
     * }</pre>
     *
     * @param directory the base directory; neither null nor blank
     * @return the composed path string
     */
    public String path(String directory) {
        if (directory == null || directory.isBlank()) {
            return value;
        }
        return directory.endsWith("/") ? directory + value : directory + "/" + value;
    }

    /**
     * The file's type token, derived from the extension and normalised to upper-case.
     *
     * <pre>{@code
     * FileName.of("IMG_9047.HEIC").nameType()  // → "HEIC"
     * FileName.of("photo.jpg").nameType()       // → "JPG"
     * FileName.of("report").nameType()          // → ""
     * }</pre>
     *
     * @return the upper-cased extension, or an empty string if no extension is present
     */
    public String nameType() {
        if (value == null) return "";
        int dot = value.lastIndexOf('.');
        return dot >= 0 && dot < value.length() - 1
                ? value.substring(dot + 1).toUpperCase()
                : "";
    }

    /**
     * The filename without its extension.
     *
     * <pre>{@code
     * FileName.of("IMG_9047.HEIC").baseName()  // → "IMG_9047"
     * FileName.of("report").baseName()          // → "report"
     * }</pre>
     */
    public String baseName() {
        if (value == null) return "";
        int dot = value.lastIndexOf('.');
        return dot >= 0 ? value.substring(0, dot) : value;
    }
}
