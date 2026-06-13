# Authority Seam — Identification Phase 1 (Design)

Promotes **Phase 1** of the identification roadmap
([`identification.md`](identification.md)) to its own design. Phase 0
(taxonomic reorganization + family/genus console) is complete; this is
the next slice.

**Scope this slice:** the `kernels/authority` port + value objects, and
the EOL fixture client (`external-authorities/eol/eol-client-mock`),
*only*. No console, no storage on `InsectSpecies`, no real HTTP client,
no bibliography shape. The port lands ahead of its consumer — a
sanctioned shape under the kernel-testing convention
([`kernels/CLAUDE.md`](../../kernels/CLAUDE.md), "No immediate consumer").

---

## Decisions (settled in brainstorming — do not re-derive)

| Question                         | Decision                                                                 |
|----------------------------------|--------------------------------------------------------------------------|
| What ships now                   | `kernels/authority` + `eol-api` + `eol-client-mock`. Console deferred.    |
| Port type                        | `ExternalAuthority` in `kernels/authority` (`com.naturalist.authority`); non-generic |
| Port operation                   | `AuthoritySource source()` + `Set<AuthorityReference> lookup(EntityName subject)` |
| Kernel breadth                   | **Domain-neutral.** Not taxon-specific; chemistry could reuse it later.  |
| Lookup key                       | Abstract framework `EntityName` (the slug→scientific-name bridge lives in each real client) |
| Reference shape                  | **`(AuthoritySource source, URI url)`** — a deep-link pointer. **No external id crosses the kernel port.** |
| Identity location                | **Per-provider, in the provider's `*-api` — never the kernel.** The kernel models the boundary surface, not external-record identity. `EolPageId` lives in `eol-api`, shaped to EOL's API; GBIF/iNat/BugGuide model their ids however their APIs work. No shared kernel id type. |
| Provider representation          | **Open `AuthoritySource` value object** (id + displayName); kernel names no provider. Mirrors the open `DomainId` precedent (ADR-023). |
| Aggregation across providers     | **None.** The consumer chooses a specific authority client and acquires its `AuthoritySource`. No fan-out composite, no merge. |
| Client family                    | A top-level `external-authorities/` grouping, one folder per provider, each with a real client + its mock. `eol-client-mock` now; real `eol-client` + other providers later. |
| No-op default in kernel          | **No.** The mock seeded with an empty map covers the unwired case.       |

### Why identity lives in the provider api, not the kernel

External-record identity is each provider's own concern, shaped to *its*
API — EOL has a page id, GBIF a numeric usage key, BugGuide a node id,
iNat a taxon id. Forcing them through one kernel id interface
(`ExternalId`) would flatten real differences and impose a uniform shape
the clients don't share. So the kernel models only the **boundary
surface** — a labeled deep-link `AuthorityReference(source, url)` — and
each provider's `*-api` models its own identity.

`EolPageId` therefore lives in `eol-api`, shaped to EOL's API: a
`NamedValue<String>` (the framework's single-value-wrapper strategy, like
`TaxonomicOrder`), **not** a `ValueObject`, and implementing **no** kernel
id type (there is none). It never crosses the kernel port — its job is
internal to the EOL family (building the deep-link now; keying the trait
fetch in Phase 4).

This keeps the reference and the (future) fetched-and-summarized payload
as two distinct concerns: this slice captures the **pointer**; Phase 4's
client fetches the **payload** (curated `TaxonCharacteristic` statements
with `LiteratureReference` citations, ADR-009) *through* it.

### Why this module shape

`kernels/authority` holds only the abstraction (port + VOs) on the
kernel light-dependency set. `external-authorities/` is a new top-level
grouping (sibling to `kernels/`, `domains/`, `adapters/`) collecting
every provider's clients, grouped **by provider**:

```
kernels/
  authority/                         port + value objects — deps: framework only
    ExternalAuthority.java           port interface
    AuthorityReference.java          ValueObject (source, url)
    AuthoritySource.java             ValueObject (id, displayName)

external-authorities/                new top-level grouping (aggregator pom)
  eol/                               per-provider grouping (aggregator pom)
    eol-api/                         EOL contract — THIS slice
      Eol.java                       `eol` AuthoritySource + deepLink(EolPageId)
      EolPageId.java                 record EolPageId(String) implements NamedValue<String>
    eol-client-mock/                 fixture client — THIS slice
      EolClientMock.java             ExternalAuthority impl over a fixture map
    eol-client/                      real HTTP client — Phase 4 (not built now)
  inat/                              later (inat-api / inat-client / inat-client-mock)
    …
```

Nothing here is in `kernels/`, so ADR-024's intent holds — heavy client
dependencies (Phase 4) never reach the kernel.

### Why no fan-out aggregator

The catalog kernel fans out across providers because a single search
should sweep every domain at once. Authorities are different: a consumer
deliberately chooses *which* authority to consult (EOL for traits, GBIF
for nomenclature, BugGuide for a deep-link) and works with that one. So
each client implements the port directly and the consumer selects it —
there is no `Composite*` and no provider SPI.

### Why bibliography is *not* here

The roadmap places `kernels/bibliography` (`LiteratureReference`) in
Phase 2, alongside the workflow that first consumes it. This slice
deliberately returns **authority references (deep-links)** — not parsed
curated-trait/citation payloads — so no half-defined citation type is
left dangling for Phase 2 to reshape.

---

## Contracts (`kernels/authority`)

### `ExternalAuthority` (port)

A service interface — *not* an identity type (it implements none of
`NamedEntity` / `Entity` / `Aggregate` / `ReadModel` / `ValueObject` /
`BehavioralCollection`). Each client implements it directly and declares
the single authority it speaks for.

```java
public interface ExternalAuthority {

    /** The authority this client consults (EOL, iNaturalist, …). */
    AuthoritySource source();

    /**
     * Deep-link references this source knows for {@code subject}.
     * Never null; an empty set means "nothing known", not an error.
     *
     * Network-backed clients MUST be Resilience-wrapped (bulkhead +
     * timeout + retry) per ADR-026. The in-memory mock is
     * @ResilienceExempt because it performs no I/O.
     */
    Set<AuthorityReference> lookup(EntityName subject);
}
```

- `subject` is the abstract framework `EntityName`. The kernel never
  sees a concrete domain identifier, so it depends on `framework` only
  and stays domain-neutral. Each real client does its own
  slug→scientific-name bridge internally.
- `source()` lets the consumer "acquire the `AuthoritySource`" from a
  chosen client — for labelling and resilience naming.
- `lookup` returns an empty set (never null) when nothing is known.

### `AuthorityReference` (ValueObject)

```java
public record AuthorityReference(AuthoritySource source, URI url)
        implements ValueObject { … }
```

Invariants:
- `source` — non-null `ValueObject` child (`valueObject(this, AuthorityReference::source, "source")`); descends into `AuthoritySource`'s invariants.
- `url` — non-null.

`url` is the fully resolvable deep-link, precomputed by the client. A
reference carries its `source` (not just the owning client carrying it)
so a stored or passed-around reference is self-describing.

### `AuthoritySource` (ValueObject — open provider)

```java
public record AuthoritySource(String id, String displayName)
        implements ValueObject { … }
```

Invariants: `id` not-blank, `displayName` not-blank.

- `id` — stable provider key, e.g. `"eol"`, `"bugguide"`, `"inaturalist"`, `"gbif"`.
- The kernel ships **no** concrete provider constants. Each provider's
  `*-api` defines its own — `eol-api` defines the `eol` `AuthoritySource`
  (`Eol.SOURCE`) — the same way `DomainId` subtypes live outside the
  catalog kernel.

---

## EOL contract (`external-authorities/eol/eol-api`)

```java
public record EolPageId(String value) implements NamedValue<String> {
    @Override public boolean isValid() { return value != null && !value.isBlank(); }
}

public final class Eol {
    public static final AuthoritySource SOURCE =
            new AuthoritySource("eol", "Encyclopedia of Life");

    /** EOL's deep-link URL pattern — EOL knowledge stays in eol-api. */
    public static URI deepLink(EolPageId id) {
        return URI.create("https://eol.org/pages/" + id.value());
    }

    private Eol() {}
}
```

`EolPageId` is EOL's native identity — a `NamedValue<String>`, not a
`ValueObject`, implementing no kernel id type. It never crosses the
kernel port; it is internal to the EOL family. `eol-api` depends on
`kernels/authority` + `framework` only, and is the shared contract the
real `eol-client` (Phase 4) will also depend on.

## EOL fixture client (`external-authorities/eol/eol-client-mock`)

```java
@ResilienceExempt
public final class EolClientMock implements ExternalAuthority {

    private final Map<EntityName, Set<AuthorityReference>> fixtures;

    private EolClientMock(Map<EntityName, Set<AuthorityReference>> fixtures) { … }

    public static EolClientMock seededWith(
            Map<EntityName, Set<AuthorityReference>> fixtures) { … }   // defensive copy

    @Override public AuthoritySource source() { return Eol.SOURCE; }

    @Override
    public Set<AuthorityReference> lookup(EntityName subject) {
        // validate subject non-null at the boundary, then:
        return fixtures.getOrDefault(subject, Set.of());
    }
}
```

- **EOL-specific.** `source()` is fixed to `Eol.SOURCE`; the factory
  takes only fixtures. A fixture reference is built through the EOL
  identity + URL pattern, e.g.
  `new AuthorityReference(Eol.SOURCE, Eol.deepLink(new EolPageId("1188585")))`.
- **Pure in-memory.** No I/O ⇒ `@ResilienceExempt` documents the
  deliberate exemption from the ADR-026 wrapping requirement.
- **Boundary validation.** `lookup` rejects a null `subject` via the
  `Observer` argument-validation idiom
  (`observer.arguments("lookup", a -> a.notNull(subject, "subject")).throwWhenInvalid()`),
  consistent with repository-mock conventions.
- **Empty for unknowns.** Unknown subject → `Set.of()`, never null.
- **No baked-in domain data.** The demo fixtures (e.g. *Battus
  philenor* → its EOL page deep-link) are supplied by the caller of
  `seededWith`. The module carries no insect data — the first such
  caller is `EolClientMockTest`; the console wires real demo fixtures in
  a later slice.
- `EntityName` subtypes qualify equality by concrete class, so map keys
  across different domains never collide.

**Forward pointer.** The real `eol-client` (Phase 4) is a sibling under
`eol/` that also depends on `eol-api` — reusing `Eol.SOURCE`,
`EolPageId`, and `Eol.deepLink` rather than redefining them.

---

## Testing (kernel-test convention)

No live consumer yet, so per
[`kernels/CLAUDE.md`](../../kernels/CLAUDE.md) we write direct tests
that document the semantics; they fold into the real consumer's tests
once the console arrives.

- **`AuthoritySourceTest`** — canonical `Observer` invariant test: valid
  case asserts `invalidInvariants().isEmpty()`; all-null case asserts
  the exact set `{id, displayName}` via `containsExactlyInAnyOrder`.
- **`AuthorityReferenceTest`** — same pattern; valid case populates
  `source` / `url`; all-null case asserts the domain-relative invariant
  paths (including descent into `source`).
- **`EolPageIdTest`** (in `eol-api`) — `isValid()` semantics: non-blank
  value valid; null/blank invalid. (`NamedValue` is validated by its
  container via `Constraints#namedValue`, but `EolPageId` has no kernel
  container this slice, so its predicate is pinned directly.)
- **`EolClientMockTest`** — stands in as the first consumer:
  - `source()` returns `Eol.SOURCE`;
  - known subject → returns the seeded references;
  - unknown subject → empty set;
  - null subject → `lookup` throws (boundary rejection);
  - `seededWith` defensively copies.

Test fixtures construct concrete `EntityName` subtypes from an existing
domain (e.g. an `InsectSpeciesName`) purely as map keys — the *test* may
depend on a domain identifier even though the kernel does not. If that
pulls an unwanted compile dependency into the mock module's test
classpath, fall back to a tiny test-only `EntityName` subtype defined in
the test source set.

---

## DAG / dependency check

```
kernels/authority                        →  framework   (EntityName, ValueObject, NamedValue, Observer, @ResilienceExempt)
external-authorities/eol/eol-api         →  kernels/authority, framework
external-authorities/eol/eol-client-mock →  external-authorities/eol/eol-api, framework
```

No dependency on `identifiers`, `taxonomy`, or any domain. All modules
stay within the kernel light-dependency set. New modules — the two
`external-authorities` aggregator poms, `kernels/authority`, `eol-api`,
and `eol-client-mock` — are registered in the root `pom.xml` module list.

---

## Out of scope (deferred — not loose ends)

- **Real provider clients** (`eol-client`, plus `inat/`, `bugguide/`,
  `gbif/` folders — each with its own `*-api` and native id shape) under
  `external-authorities/`. Phase 4+. No shared kernel id type is ever
  introduced — identity stays per-provider.
- **Console rendering & authority-link storage on `InsectSpecies`.** The
  next slice. Decides `Set<AuthorityReference>` field vs typed-per-provider
  columns (the roadmap's open Phase 1 question) at the point it has a
  real consumer.
- **API-key skeleton.** No real client needs a key yet; it lands with the
  Phase 4 EOL client and its env-var convention.
- **Image-upload destination story.** Unrelated plumbing; stays in its
  own effort.
- **`kernels/bibliography` / `LiteratureReference`.** Phase 2, with the
  workflow that consumes it.

---

## Work-tracker correction (do alongside this slice)

[`work-tracker.md`](../work-tracker.md) currently labels
`kernels/bibliography` as a Phase 1 candidate (lines 16, 24). The
source-of-truth roadmap places it in Phase 2. Update the tracker so
"current phase" reads *Phase 1 — `kernels/authority` port + EOL mock
client* and bibliography is shown under Phase 2, removing the
discrepancy this slice surfaced.
