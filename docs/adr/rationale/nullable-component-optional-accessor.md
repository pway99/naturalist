# Nullable Component + Optional Accessor — Tradeoff Analysis

> Context for ADR amendment. Captures the design tension between Java
> records' auto-generated accessors and the project's instinct toward
> `Optional`-shaped return types, and explains why the project does
> **not** ship a paired `xOptional()` accessor alongside an `@Nullable`
> record component.
>
> **Revised 2026-05-14.** An earlier draft of this doc advocated for the
> paired-accessor pattern (Option A below). After living with the
> pattern for a few days under the clades-kernel work, the awkwardness
> showed up most loudly in `InsectLifeStages#resolve(Optional<Clade>)`
> — an Optional parameter, which is an Effective Java Item 55
> anti-pattern that no private-method wrapping can fully disguise. The
> alternatives analysis below is preserved; the decision is now
> Option B.

## The tension

Java records auto-generate accessors directly from component declarations:
a component `Foo foo` produces an accessor `foo() : Foo` with no way to
intervene on its shape or return type. This is a deliberate design choice
in the records JEP — records are transparent carriers of their components,
and the auto-generated accessor *is* the component.

The project's conventions point in a different direction for nullable
fields. Across the codebase, the chemistry domain ships a "Optional
Profile Methods" pattern:

- Storage uses `@Nullable T t` — JSpecify annotation, plain reference.
- External access goes through `Optional<T> tOptional()` — freestanding
  method, returns `Optional.ofNullable(t)`.

This produces two accessors on the same field: `t()` (auto-generated,
returns `@Nullable T`) and `tOptional()` (freestanding, returns
`Optional<T>`). Both are public. Consumers pick the one matching their
style.

That two-accessor shape is the tension. It reads as awkward in the
abstract — "why are there two methods for one field?" — and the question
keeps surfacing in design review.

## Why the tension exists

The friction isn't caused by the project's conventions. It's caused by
**Java records' design itself**: records make the component and the
accessor the same declaration. There's no way to write a record component
of type `T` and have it expose externally as `Optional<T>` without either:

- Naming the component something other than the accessor you want
  (component `tValue`, accessor `t() : Optional<T>` declared separately
  — pollutes the Jackson wire format), or
- Using `Optional<T>` as the component type itself (the records-of-Optional
  anti-pattern — see Effective Java Item 55, "It is almost always wrong
  to declare a field of type Optional").

So both directions are constrained. The project's choice is which trade
to accept: paired accessors on every nullable component, or Optional
wrapping at each consumer site.

## Why `Optional<T>` as a record component is rejected

A field of type `Optional<T>` has three states, not two:

1. `t == null` — the field itself is null
2. `t == Optional.empty()` — non-null Optional, no value
3. `t == Optional.of(value)` — non-null Optional, with value

States 1 and 2 are functionally equivalent but mechanically distinct.
Every consumer must either trust an invariant that the field is never
null (requiring validation everywhere construction happens, including
Jackson deserialization and reflective code paths) or defensively check
both `t == null` and `t.isEmpty()`. Auto-generated `equals` and
`hashCode` make states 1 and 2 unequal, so the "absent" state isn't
canonical without further care.

`Optional` was designed as a *return type* signaling "the answer may not
exist." It was not designed as a *storage type* signaling "this slot may
or may not be filled." Conflating those roles is the anti-pattern
Effective Java specifically warns against, and records make the
conflation more visible than usual because the component is literally
the field declaration.

Jackson handling compounds the problem: `@Nullable T t` natively handles
"missing JSON field → null component." `Optional<T> t` requires either
configuration to map `null` JSON to `Optional.empty()` (or accept
literal `null` Optional components) and decisions about wire format
(omit field? write `null`? write `{}`?).

## Alternatives considered

### A — Paired accessors (`@Nullable` storage + freestanding `Optional` accessor)

`@Nullable T t` component, freestanding `Optional<T> tOptional()` method.

**Cost.** Two accessors on the same field. Small ongoing readability tax
that compounds:
- Every nullable component gains an `xOptional()` sibling. The api
  surface grows uniformly for an ergonomic gain that the caller could
  also get with a one-line wrap.
- Internal helpers that *take* an `Optional<T>` (e.g.
  `InsectLifeStages#resolve(Optional<Clade>)`) inherit the
  Optional-as-parameter anti-pattern. Private-method scoping reduces
  the harm but doesn't eliminate the smell — the shape still reads
  wrong because the caller is "pre-wrapping" a value the helper
  immediately unwraps.
- Consumers must choose between two equivalent accessors at every
  call site. The two-accessor design promised choice; in practice
  the codebase picks one consistently and the other becomes dead
  ergonomics.

**Benefit.** Saves one `Optional.ofNullable(...)` wrap at consumer
sites that want fluent chaining. The wrap is one method call.

### B — Nullable accessor only; consumer wraps for Optional ergonomics (current decision)

Storage and external API are both `@Nullable T t`. The auto-generated
accessor is the only public surface. Consumers that want fluent
Optional chaining wrap themselves: `Optional.ofNullable(record.t()).map(...)`.

**Cost.** Each call site that wants the Optional API surface writes
`Optional.ofNullable(...)` once. Across the small set of consumers
that actually want Optional chaining, this is a few extra characters
per site.

**Benefit.**
- One accessor per field. No "which one do I call" choice.
- Internal helpers take `@Nullable T`, never `Optional<T>` — no
  parameter-of-Optional anti-pattern.
- The api surface stays uniform with every other `@Nullable`
  component in the codebase (life-stage fields, voltinism,
  identificationFeatures, etc., all of which use plain `@Nullable`
  without sibling Optional accessors).
- Consumer ergonomics are still available — they're just opt-in at
  the call site rather than baked into the type.

### C — Rename the component to free the canonical name for an Optional-returning method

Component `@Nullable Clade placedInClade`, public
`Optional<Clade> placedIn() { return Optional.ofNullable(placedInClade); }`.

**Cost.** Component name drives the Jackson field name, so the JSON
wire format becomes `"placedInClade": "papilionidae"` — uglier than
`"placedIn": "papilionidae"`. Fixing it requires `@JsonProperty("placedIn")`
on the component, which the project's record conventions explicitly do
not use. The renamed component (`placedInClade`) is a name that exists
only because of a naming collision, not because the domain calls it that.

**Verdict.** Trades a Java-API tension for a wire-format tension. Worse.

### D — Sealed type modeling presence

Replace `@Nullable Clade placedIn` with a domain-level sealed type:

```java
sealed interface CladePlacement {
    record Placed(Clade clade) implements CladePlacement {}
    record Unplaced() implements CladePlacement {}
}
```

Component is `CladePlacement placedIn` (always non-null). Consumers
pattern-match exhaustively.

**Cost.** Adds a new type to the kernel surface for what was a single
nullable field. JSON serialization gets messier — Jackson needs a
discriminator or polymorphic-deserialization config to handle the
sealed type. Pattern-matching for a binary present/absent is
significantly heavier than `Optional.ofNullable` or a null check. And
the pattern doesn't match any other nullable component in the project:
`egg`, `larva`, `pupa`, `adult` all use `@Nullable`. Introducing
`CladePlacement` only for `placedIn` is inconsistent inside the same
record.

**Verdict.** Real domain modeling, but over-engineered for the actual
problem and inconsistent with established `@Nullable` usage elsewhere
on the same type.

### E — Move the placement off the record entirely

Don't store `Clade` on `InsectFamily` at all. A domain-level function
maps `InsectFamily → Optional<Clade>` via a separate lookup table.

**Cost.** Breaks the project's identity-on-the-entity pattern. Every
other classification fact about an insect family — order, family,
common names, life stages — lives on the record. Pulling `placedIn`
*out* of the record into a sidecar registry makes it the one
classification fact that isn't owned by the entity, a special case
with no domain justification. Requires a registry to maintain, which
the sealed-records design specifically avoided.

**Verdict.** Mechanically works, but factors classification data the
wrong way for this codebase.

## Decision

**Adopt Option B** — `@Nullable T t` component, no paired
`tOptional()` accessor. Consumers that want Optional ergonomics call
`Optional.ofNullable(record.t())` at the call site.

The two-accessor pattern (Option A) was the project's earlier choice
on the theory that "every consumer doing more than a null check"
would benefit from the pre-wrapped Optional. In practice the consumer
set is small, the wrap is one line, and the alternative cost — paired
api surface that grows linearly with the number of nullable
components, plus internal helpers that take `Optional<T>` and trigger
the parameter anti-pattern — is the larger tax.

## ADR amendment text (suggested)

The following paragraphs are written to be incorporated into the ADR
covering Java record conventions (currently ADR-012 per the project's
shared CLAUDE.md). They acknowledge the tension explicitly so future
readers don't relitigate.

---

### Nullable components on records

Java records auto-generate accessors from component declarations. A
component `T t` produces an accessor `t() : T`; there is no language
mechanism to alter the accessor's return type, name, or nullability
shape from the component declaration alone.

This creates a tension whenever a domain field is conceptually nullable
*and* a consumer would benefit from `Optional<T>` chaining. The
available options reduce to:

1. Declare the component as `Optional<T>` — rejected as an anti-pattern
   (Effective Java Item 55), produces three-state field semantics
   (`null`, `Optional.empty()`, `Optional.of(value)`), causes Jackson
   serialization friction, and makes `equals`/`hashCode` behave
   surprisingly across "absent" representations.
2. Declare the component as `@Nullable T` and add a freestanding
   `Optional<T> tOptional()` method — produces two accessors on the
   same field, grows the api surface uniformly with the number of
   nullable components, and pulls the Optional-as-parameter
   anti-pattern into internal helpers that consume the wrapper form.
3. Declare the component as `@Nullable T` and expose only the
   auto-generated accessor — consumers wrap with
   `Optional.ofNullable(record.t())` at call sites where Optional
   ergonomics are wanted. One accessor per field. No
   parameter-of-Optional anti-pattern.

The project chooses option 3. The cost is one `Optional.ofNullable(...)`
wrap per consumer site that wants Optional chaining; the benefit is a
uniform api surface and internal helpers that operate on `@Nullable T`
directly. This is not a flaw in the project's conventions — it is the
least bad response to a constraint that Java records impose by design.

JSON wire format is driven by the component name. Components are named
for the domain concept (`placedIn`, `egg`, `larva`), never for the
accessor shape — `placedInOptional` is never a component name.

Reference implementations: every `@Nullable` component on
`InsectFamily`, `InsectGenus`, and `InsectSpecies` (life-stage fields,
`placedIn`, voltinism, etc.) follows this shape — auto-generated
nullable accessor, no paired `xOptional()`.

---

End of suggested ADR text.
