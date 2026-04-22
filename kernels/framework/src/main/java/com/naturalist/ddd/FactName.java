package com.naturalist.ddd;

import java.util.Objects;
import java.util.UUID;

/**
 * Surrogate identifier for a {@link FactEntity} — a UUID assigned at creation time.
 *
 * <p>Unlike {@link EntityName}, a fact name is not a natural key: two measurements of
 * the same phenomenon at different moments are distinct facts with distinct identifiers.
 * Fact names are never referenced cross-domain by value; other domains reason about
 * facts through service interfaces, not by slug.
 */
public abstract class FactName {

    final UUID value;

    protected FactName(UUID value) {
        this.value = value;
    }

    public UUID value() {
        return value;
    }

    public boolean isValid() {
        return value != null;
    }

    public boolean isNotValid() {
        return !isValid();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FactName that = (FactName) o;
        return Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(value);
    }

    @Override
    public String toString() {
        return String.valueOf(value);
    }
}
