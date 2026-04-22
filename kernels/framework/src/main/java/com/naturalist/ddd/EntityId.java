package com.naturalist.ddd;

import java.util.Objects;
import java.util.UUID;

/**
 * Surrogate identifier for an {@link Entity} — a UUIDv7 assigned at record
 * construction.
 *
 * <p>UUIDv7 encodes a Unix-ms timestamp in its leading 48 bits, so values
 * sort chronologically and keep B-tree indexes dense under sustained insert
 * load. The kernel commits to v7 specifically: {@link #isValid()} rejects any
 * other UUID version.
 *
 * <p>{@code EntityId} is abstract. Each concrete subtype (e.g.
 * {@code AmendmentEventId}, {@code LabAnalysisId}) is a distinct type at
 * compile time — equality is qualified by {@code getClass()}, so two distinct
 * id types holding the same UUID never compare equal.
 */
public abstract class EntityId {

    final UUID value;

    protected EntityId(UUID value) {
        this.value = value;
    }

    public UUID value() {
        return value;
    }

    /**
     * True when {@link #value()} is non-null and carries UUID version 7.
     */
    public boolean isValid() {
        return value != null && value.version() == 7;
    }

    public boolean isNotValid() {
        return !isValid();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EntityId that = (EntityId) o;
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
