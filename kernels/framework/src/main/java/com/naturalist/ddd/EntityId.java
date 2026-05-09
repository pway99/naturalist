package com.naturalist.ddd;

import com.fasterxml.jackson.annotation.JsonValue;
import com.github.f4b6a3.uuid.UuidCreator;
import org.jspecify.annotations.Nullable;

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

    @JsonValue
    public UUID value() {
        return value;
    }

    /**
     * The single kernel entry point for generating {@link EntityId} values.
     * Returns a monotonic-within-ms UUIDv7 (RFC 9562) from
     * {@code com.github.f4b6a3:uuid-creator}.
     *
     * <p>Concrete subclasses call this from their {@code create()} factory —
     * {@code UUID.randomUUID()} and hand-rolled v7 generators are forbidden
     * in domain and adapter code (ADR-022).
     */
    public static UUID newUUID() {
        return UuidCreator.getTimeOrderedEpoch();
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

    public String errorMessage() {
        if (isValid()) return "";
        return value == null
                ? "null value"
                : "Found UUID Version:%d, expected:7".formatted(value.version());
    }

    /**
     * Null-safe accessor for {@link #errorMessage()} — returns {@code "null value"}
     * when {@code id} itself is null, otherwise delegates to the instance method.
     * Use this from constraint code that must render a message regardless of
     * whether the EntityId reference is present.
     */
    public static String errorMessageFor(@Nullable EntityId id) {
        return id == null ? "null value" : id.errorMessage();
    }

    /**
     * Null-safe validity check — {@code false} when {@code id} is null, otherwise
     * delegates to {@link #isValid()}.
     */
    public static boolean isValid(@Nullable EntityId id) {
        return id != null && id.isValid();
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
