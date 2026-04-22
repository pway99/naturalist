package com.naturalist.ddd;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Stable natural key for a {@link NamedEntity} — a URL-safe kebab-case slug.
 *
 * <p>A subclass fixes its {@link #maxLength()} and otherwise carries no behaviour of
 * its own. Equality is value-based, qualified by concrete class so two distinct slug
 * types holding the same string never compare equal.
 */
public abstract class EntityName {

    private static final Pattern LOWER_KEBAB = Pattern.compile("^[a-z0-9]+(-[a-z0-9]+)*$");

    final String value;

    protected EntityName(String value) {
        this.value = value;
    }

    protected abstract int maxLength();

    public String value() {
        return value;
    }

    public boolean isValid() {
        return value != null
                && value.length() <= maxLength()
                && LOWER_KEBAB.matcher(value).matches();
    }

    public boolean isNotValid() {
        return !isValid();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EntityName that = (EntityName) o;
        return Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
