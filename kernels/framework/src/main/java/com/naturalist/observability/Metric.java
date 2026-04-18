package com.naturalist.observability;

import io.micrometer.core.instrument.Metrics;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * A named metric with typed tags, following Micrometer naming conventions:
 * lowercase, dot-separated metric names and tag keys. Tag values are preserved
 * verbatim — observation scopes and constraint names carry mixed case by design.
 *
 * <p>Invalid input is silently normalised rather than rejected — the observability
 * framework must never throw from metric emission.
 *
 * <p>Usage:
 * <pre>{@code
 * Metric.counter("naturalist.invariant.violation")
 *       .tag("constraint", "NotBlankConstraint")
 *       .tag("class", "CompoundQuery")
 *       .tag("method", "getByName")
 *       .incrementCounter();
 * }</pre>
 */
public final class Metric {

    private final String name;
    private final List<Tag> tags;

    /**
     * A key-value tag pair. Keys are normalised to Micrometer convention
     * (lowercase, dot-separated). Values are preserved as-is.
     */
    public record Tag(String key, String value) {
        public Tag {
            key = normalizeName(key);
            value = value == null ? "none" : value;
        }
    }

    private Metric(String name) {
        this.name = normalizeName(name);
        this.tags = new ArrayList<>();
    }

    /**
     * Create a counter metric with the given name.
     */
    public static Metric counter(String name) {
        return new Metric(name);
    }

    public String name() {
        return name;
    }

    public List<Tag> tags() {
        return List.copyOf(tags);
    }

    public Metric tag(String key, String value) {
        tags.add(new Tag(key, value));
        return this;
    }

    public Metric tag(String key, boolean value) {
        return tag(key, String.valueOf(value));
    }

    /**
     * Terminal operation — increments this counter on Micrometer's global
     * {@link Metrics#globalRegistry}. The global registry is a
     * {@code CompositeMeterRegistry} that delegates to all registered backends.
     * When no backends are configured it is effectively a no-op.
     */
    public void incrementCounter() {
        Metrics.counter(name, tagsAsArray()).increment();
    }

    /**
     * Normalise a metric name or tag key to Micrometer convention: lowercase,
     * dot-separated, no leading/trailing dots, no consecutive dots. Spaces and
     * underscores become dots. Null or blank input becomes "unknown".
     */
    static String normalizeName(String input) {
        if (input == null || input.isBlank()) {
            return "unknown";
        }
        return input.strip()
                .toLowerCase(Locale.ROOT)
                .replaceAll("[\\s_]+", ".")
                .replaceAll("\\.{2,}", ".")
                .replaceAll("^\\.|\\.$", "");
    }

    private String[] tagsAsArray() {
        String[] result = new String[tags.size() * 2];
        for (int i = 0; i < tags.size(); i++) {
            result[i * 2] = tags.get(i).key();
            result[i * 2 + 1] = tags.get(i).value();
        }
        return result;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Metric m)) return false;
        return name.equals(m.name) && tags.equals(m.tags);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, tags);
    }

    @Override
    public String toString() {
        return name + tags;
    }
}
