# Glossary auto-linking — jargon in insect field marks → glossary

**Date:** 2026-07-16
**Status:** approved (v1 scope), pending spec review
**Scope:** insects-console (feature rendering) + app CSS
**Builds on:** `docs/plans/2026-07-16-glossary-design.md` (phase 2)

## Problem

Insect **Field Marks** (rendered by `features.jte` from `FeatureGroup.marks()`)
are full of the vernacular the glossary now defines — *dorsum, scutellum,
conspicuous, aposematic*. A reader hitting one of those words should see its
definition **without leaving the insect page** — an inline definition popover,
the same no-JS native-popover pattern as the clade/taxonomy breadcrumb "?"
popovers (`components/infoPopover.jte`), with a "Full entry →" link inside for
when they do want the full glossary page.

## Design — server-side render helper

Coupling is already in place: `insects-console` depends on `library-api` +
`library-test-context`, and `InsectsController` already builds a
`LibraryTestContext`. So the glossary vocabulary is one `.glossaryTermQuery()`
call away — no new cross-domain seam.

### `GlossaryLinker` (new, `insects-console`)

A pure presentation helper, constructed from the glossary vocabulary
(display-form → slug → definition), longest-term-first.

```java
String linkHtml(String text, String idSeed)   // returns safe HTML
GlossaryLinker.none()                          // identity linker: escapes, links nothing
```

`linkHtml` contract:
- **HTML-escapes** the input first — field marks are AI-generated, so the output
  is only ever escaped text plus the elements this helper controls. (Escaping is
  preserved even for `none()`, matching the original `${mark}` behavior.)
- Wraps each match in a native-popover trigger + box:
  `<span class="glossary-term"><button class="glossary-link" popovertarget="…">term</button>`
  `<span popover class="info-popover-box glossary-popover">definition <a href="/glossary/{slug}">Full entry →</a></span></span>`.
  The trigger is a `<button>` (not `<a>`) because `popovertarget` only works on
  buttons; it is styled as a dotted-underline inline link. Reuses
  `.info-popover-box` for the card + top-layer anchor positioning.
- Preserves the matched text's original casing as the trigger label.
- **Whole-word, case-insensitive, exact** match — "dorsum" matches; "dorsal" and
  "aposematism" do not. No stemming/plurals in v1.
- **Longest term first** — a multi-word term ("field mark") wins over a sub-word.
- **First occurrence of each term per text only** — avoids a wall of links.
- `idSeed` (rank slug + mark index) makes each popover's id unique per page.

### Wiring

- `InsectsController` builds one `GlossaryLinker` at construction from
  `libraryContext.glossaryTermQuery().findPage(all)` (vocabulary is stable;
  build once, reuse) and adds it to the model for the four rank pages.
- `features.jte` gains `@param GlossaryLinker glossaryLinker = GlossaryLinker.none()`
  and renders `$unsafe{glossaryLinker.linkHtml(mark, rankSlug + "-" + i)}` in
  place of `${mark}` (indexed loop for the seed).
- The four including pages (`detail.jte`, `genus.jte`, `family.jte`,
  `order.jte`) pass `glossaryLinker = glossaryLinker` to `@template.insects.features`.
- `.glossary-link` (button reset → inline dotted underline) + `.glossary-popover`
  in `naturalist.css`; the popover box reuses `.info-popover-box`.

### Testing

`GlossaryLinkerTest`: whole-word match, case-insensitive match with casing
preserved, first-occurrence-only, longest-term-wins, no-match passthrough,
empty/`none()` vocabulary, and HTML-escaping (a mark containing `<`/`&` and a
term is escaped with only the intended `<a>` injected).

## Rejected alternatives

- **Plain link that navigates to `/glossary/{slug}`** (the first cut): sends the
  reader off the insect page to read one definition and back. The popover keeps
  them in place; the full-entry link is still one click away inside it.
- **Keeping an `<a>` trigger + JavaScript** to drive the popover: breaks the
  no-JS native-popover pattern the rest of the app uses.
- **Client-side JS scanner** (vocab as JSON): no coupling needed — but the
  coupling already exists, and server-side avoids a render flash and is
  unit-testable.
- **Explicit `[[term]]` markup** in data + the vision prompt: precise but
  requires rewriting existing content and the model won't emit markers reliably.

## Out of scope / later

- **Synonyms / aliases and inflections** (dorsal→dorsum, plurals) — a future
  "synonyms" concept: a term carries alternate surface forms the linker also
  matches, all resolving to the one canonical entry. Deliberately deferred.
- Linking the "Why this ID?" evidence and the Durrell descriptions.
- Other domains — when a second consumer appears, promote `GlossaryLinker` out
  of `insects-console` into a shared module.
