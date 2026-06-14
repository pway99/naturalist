package com.naturalist.clades;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.naturalist.fieldnotes.Description;

import java.util.Optional;

/**
 * A node in the evolutionary tree of life — Eukaryota at the root, with named
 * branches like Holometabola or Papilionidae below it. Each permit is a
 * stateless record carrying its slug, display name, four-level Durrell
 * {@link Description}, and an {@link Optional} reference to its parent
 * clade ({@link Optional#empty} at Eukaryota). Adding a clade is a
 * deliberate kernel change rather than a data-entry task — the catalog
 * is curated, reviewable, and small enough to live as code.
 * <p>
 * The clade DAG is parallel to the Linnaean rank DAG in
 * {@code kernels/taxonomy}. Ranks (Family, Genus, Species, …) are a
 * classification scheme with a fixed number of named tiers; clades are the
 * branch points biology hangs traits on. Domains attach trait declarations
 * to clades at the level where the trait originated; descendants resolve
 * those traits via {@link CladeTraversal#findTrait}, which walks the parent
 * chain looking for the nearest declaration.
 * <p>
 * The kernel holds no trait declarations of its own — those live in the
 * consuming domains (e.g. insects-api decides that Holometabola is
 * holometabolous). The kernel is pure structure: who's whose parent, and
 * the descriptive metadata that lets a UI talk about each clade. See
 * {@code docs/plans/clades-kernel.md} for the multi-phase plan.
 *
 * <h2>Jackson</h2>
 * Serialised as the {@link #slug()} string ({@link JsonValue}); deserialised
 * via {@link #of(String)} ({@link JsonCreator}). Unknown slugs throw
 * {@link IllegalArgumentException}.
 */
public sealed interface Clade
        permits Eukaryota,
        Animalia,
        Anthophila,
        Apoidea,
        Arthropoda,
        Blattodea,
        DrosophilaSensuStricto,
        Drosophilinae,
        Hemiptera,
        Holometabola,
        Insecta,
        Lepidoptera,
        Papilionidae,
        Papilionoidea,
        Sophophora,
        Termitoidae,
        Troidini {

    @JsonValue
    String slug();

    String displayName();

    Description description();

    /**
     * The parent clade, or empty at the root (Eukaryota). Cross-permit
     * references are by record instance — every record is stateless and
     * value-equal to any other instance of the same permit, so consumers
     * can compare and key on {@code parent().get()} freely. Returning
     * {@link Optional} rather than a nullable reference keeps traversal
     * call sites NPE-free without per-site null checks.
     */
    Optional<Clade> parent();

    @JsonCreator
    static Clade of(String slug) {
        return switch (slug) {
            case "anthophila" -> new Anthophila();
            case "animalia" -> new Animalia();
            case "apoidea" -> new Apoidea();
            case "arthropoda" -> new Arthropoda();
            case "blattodea" -> new Blattodea();
            case "drosophila-sensu-stricto" -> new DrosophilaSensuStricto();
            case "drosophilinae" -> new Drosophilinae();
            case "eukaryota" -> new Eukaryota();
            case "hemiptera" -> new Hemiptera();
            case "holometabola" -> new Holometabola();
            case "insecta" -> new Insecta();
            case "lepidoptera" -> new Lepidoptera();
            case "papilionidae" -> new Papilionidae();
            case "papilionoidea" -> new Papilionoidea();
            case "sophophora" -> new Sophophora();
            case "termitoidae" -> new Termitoidae();
            case "troidini" -> new Troidini();
            default -> throw new IllegalArgumentException("Unknown clade: " + slug);
        };
    }
}
