package com.naturalist.catalog;

/**
 * The shape of a token match between a search query and a contributed surface
 * form. Carried on every {@link SearchHit} so the UI can both order results
 * and explain to the reader why a particular hit surfaced.
 * <p>
 * The enum order is the intended display order: {@link #EXACT_SLUG} hits
 * first, {@link #PREFIX} hits last. Adding a fourth kind later (e.g. a
 * {@code FUZZY} once a Lucene-backed implementation lands) is an enum
 * extension — append it at the end and the existing ordering still holds.
 */
public enum MatchKind {

    /**
     * The query, after normalisation, equalled the entity's slug exactly.
     * The strongest signal: the reader typed (or pasted) the canonical
     * identifier and the catalog has it.
     */
    EXACT_SLUG,

    /**
     * The query equalled some non-slug token contributed for the entity —
     * a binomial component, a common-name label, an abbreviated form. The
     * reader's input matched a recognised surface form rather than the
     * underlying identifier.
     */
    EXACT_TOKEN,

    /**
     * The query was a prefix of one of the entity's contributed tokens. A
     * weaker signal than an exact match — useful when a reader is still
     * typing or remembers only the leading letters of a name.
     */
    PREFIX
}
