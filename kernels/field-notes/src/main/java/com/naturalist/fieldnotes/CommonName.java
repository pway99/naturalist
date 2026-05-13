package com.naturalist.fieldnotes;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.naturalist.ddd.ValueObject;
import com.naturalist.observability.Constraints;

import java.util.Locale;
import java.util.function.Consumer;

/**
 * A vernacular name for a catalog entity, paired with the locale in which the
 * label is meaningful.
 * <p>
 * Common names are the field-naturalist's everyday vocabulary — "pipevine
 * swallowtail", "California Dutchman's pipe", "borraja". They are
 * deliberately textual (not a controlled vocabulary), often regional, and
 * frequently ambiguous. The catalog search index harvests them as additional
 * surface forms under which an entity is findable; the entity that owns the
 * name carries it as a {@code Set<CommonName>} alongside its scientific
 * identifiers.
 * <p>
 * Locale is a presentation and disambiguation concern, not a search concern —
 * a user searching for "clover" should hit any English variant; a future UI
 * may present the locale-appropriate label first. {@link java.util.Locale}
 * covers the regional-variant case ({@code en-US}, {@code en-GB},
 * {@code es-MX}) without inventing a parallel type, and Jackson natively
 * round-trips {@code Locale} as a BCP-47 tag.
 * <p>
 * {@code CommonName} carries no back-pointer to an {@code EntityName}; the
 * association is held by the entity that owns the name. This keeps
 * {@code field-notes} purely textual and dependent only on {@code framework}.
 */
public record CommonName(String label, Locale locale) implements ValueObject {

    public static CommonName of(String label) {
        return new CommonName(label, Locale.ENGLISH);
    }

    @JsonCreator
    public static CommonName of(
            @JsonProperty("label") String label,
            @JsonProperty("locale") Locale locale) {
        return new CommonName(label, locale);
    }

    @Override
    public Consumer<? extends Constraints> invariants() {
        return i -> i
                .notBlank(label, "label")
                .notNull(locale, "locale");
    }
}
