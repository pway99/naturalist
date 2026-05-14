# Nullable Component + Optional Accessor — Tradeoff Analysis

> Context for ADR amendment. Captures the design tension between Java
> records' auto-generated accessors and the project's preference for
> `Optional`-shaped return types, and the alternatives considered before
> settling on the established `@Nullable` component + freestanding
> `xOptional()` accessor pattern.

## The tension

Java records auto-generate accessors directly from component declarations:
a component `Foo foo` produces an accessor `foo() : Foo` with no way to
intervene on its shape or return type. This is a deliberate design choice
in the records JEP — records are transparent carriers of their components,
and the auto-generated accessor *is* the component.

The project's conventions point in a different direction for nullable
fields. Across the codebase, the established pattern (chemistry domain's
"Optional Profile Methods") is:

- Storage uses `@Nullable T t` — JSpecify annotation, plain reference,
  consistent with the four `@Nullable` life-stage components already on
  `InsectFamily` (`egg`, `larva`, `pupa`, `adult`).
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

So the two-accessor pattern isn't a flaw in the project's design — it's
the *least bad* response to a constraint that Java records impose on any
domain that wants both compact nullable storage and ergonomic Optional
access. Acknowledging this explicitly is the point of this document.

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

Each alternative trades the two-accessor friction for a different,
typically larger, cost.

### A — Keep the pattern (current proposal)

`@Nullable T t` component, freestanding `Optional<T> tOptional()` method.

**Cost.** Two accessors on the same field. Small ongoing readability tax.

**Benefit.** Aligned with existing chemistry-domain idiom. Consistent
with the other `@Nullable` components on the same record. JSON wire
format stays clean (`"t": null` or omitted). Jackson handles it
natively. Equals and hashCode behave correctly.

### B — Drop the Optional accessor entirely

Just `@Nullable T t`. Consumers wrap with `Optional.ofNullable(...)` at
call sites when they want Optional ergonomics.

**Cost.** Every consumer doing more than a null check writes
`Optional.ofNullable(family.placedIn()).map(...)`. Across the codebase
this is consistent noise. The Optional accessor exists because Optional's
API surface (`map`, `filter`, `orElse`, `ifPresent`) is genuinely useful;
forcing every caller to construct the Optional themselves loses that
ergonomic win.

**Verdict.** Cleaner inside the record, worse at every call site.

### C — Drop the nullable accessor (use only Optional)

Rename the component to free the canonical name for an Optional-returning
method: `@Nullable Clade placedInClade` component (auto-accessor
`placedInClade() : @Nullable Clade`), plus public
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

```java
public final class InsectClades {
    public static Optional<Clade> placementOf(InsectFamily family) { … }
}
```

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

**Adopt Option A** — `@Nullable T t` component plus freestanding
`Optional<T> tOptional()` accessor. Continue the existing
chemistry-domain idiom for all nullable fields on records.

The two-accessor surface is the smallest available cost. Every
alternative trades it for a larger one — anti-patterns, wire-format
pollution, inconsistency with sibling `@Nullable` components on the
same record, or breaking the project's identity-on-the-entity pattern.

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
*and* the project's preferred external-access shape is `Optional<T>`.
The available options reduce to:

1. Declare the component as `Optional<T>` — rejected as an anti-pattern
   (Effective Java Item 55), produces three-state field semantics
   (`null`, `Optional.empty()`, `Optional.of(value)`), causes Jackson
   serialization friction, and makes `equals`/`hashCode` behave
   surprisingly across "absent" representations.
2. Declare the component as `@Nullable T` and expose only the
   auto-generated accessor — forces every consumer to write
   `Optional.ofNullable(...)` at the call site, losing the ergonomic
   point of Optional.
3. Declare the component as `@Nullable T` and add a freestanding
   `Optional<T> tOptional()` method — produces two accessors on the
   same field but keeps each in its idiomatic role: `@Nullable` for
   storage, `Optional` for ergonomic access.

The project chooses option 3. The cost is a small readability tax
(two accessors visible to consumers); the benefit is correctness and
consistency with every other nullable component on records across the
codebase. This is not a flaw in the project's conventions — it is the
least bad response to a constraint that Java records impose by design.

Consumers may use either accessor. The auto-generated `t() : @Nullable T`
is appropriate for null-check sites and Jackson serialization. The
freestanding `tOptional() : Optional<T>` is appropriate for fluent
chains (`map`, `filter`, `orElse`). Both are public; neither is
preferred over the other.

JSON wire format is driven by the component name. Components are named
for the domain concept (`placedIn`, `egg`, `larva`), never for the
accessor shape — `placedInOptional` is never a component name.

Reference implementations: `domains/chemistry/...` ("Optional Profile
Methods" section in chemistry's CLAUDE.md), and as of Phase 3 of the
clades kernel integration, the `placedIn` component on `InsectFamily`,
`InsectGenus`, and `InsectSpecies`.

---

End of suggested ADR text.
