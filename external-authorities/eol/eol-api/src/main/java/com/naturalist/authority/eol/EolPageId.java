package com.naturalist.authority.eol;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.naturalist.ddd.NamedValue;

/**
 * EOL's native record identifier — a page id, shaped to EOL's API.
 * A {@link NamedValue}, not a {@code ValueObject}, and not a kernel id
 * type: it never crosses the authority port. Used inside the EOL family
 * to build deep-links (and to key the trait fetch in Phase 4).
 */
public record EolPageId(String value) implements NamedValue<String> {

    @JsonCreator
    public static EolPageId of(String value) {
        return new EolPageId(value);
    }

    @Override
    public boolean isValid() {
        return value != null && !value.isBlank();
    }
}