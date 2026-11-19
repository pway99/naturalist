# Named values: one serialization strategy on the supertype

**Status:** design, 2026-11-18. Agreed with Pat in conversation; not yet planned.

## Why

A single value that names something (a slug, a file name, an email address, a surrogate id) is
read in two places today, and each type reads itself:

- **JSON** (catalog files, stored rows): 92 `@JsonCreator` annotations, one per subtype's `of`.
- **Web handlers**: about 25 endpoints in 9 controllers take `@PathVariable String name` and
  call `XName.of(name)` in the body. Nothing observes the name at the edge; an invalid slug
  flows into the query. `serve` was the same until `8dc6b59e` made it take
  `@PathVariable FileName`.

Pat's direction: the supertype owns the strategy, the value is handed on as given, and the
consumer chooses whether it needs a valid one, so the count lands where the context is
(P6, C1, J4). A rewrite recipe keeps it standard.

## Goals

- `EntityName`, `NamedValue` and `EntityId` declare their deserialization once, on the
  supertype. Serialization stays as it is (`@JsonValue` on `value()`).
- The deserializer **hands on** what it is given (Pat, option A): an invalid slug is built
  and returned; the consumer observes it with its kind's constraint (M14) and decides.
- Web handlers take the typed value as the parameter, and the edge observes it as an
  argument (`arguments(i -> i.entityName(name, "name")).observe(Level.WARN)`, B1, M1, M2).
- OpenRewrite recipes enforce the standard at the dry-run gate.

## Non-goals

- Changing what an endpoint answers for a bad name. An invalid slug gets the answer an
  unknown one gets today, plus a WARN count. Making those answers say "not found" (B1, "a
  request for one thing") is a separate fix.
- Request records for many-argument commands (`InsectPhotoCommandImpl.add`,
  `InsectIdentificationCommandImpl.identify`). Separate design, next.
- Core code that legitimately builds a name from a String (`InsectRankName.of(slug, rank)`,
  `buildIdentifiedEntity`, Dbo mappers, `Nutrients.chemistry`).
- `RankName` fields: they keep their property-level `RankNameSerializer`/`RankNameDeserializer`
  (`{"rank","value"}` objects), which win over a class-level annotation.
- `Clade.of(slug)` / `Bioregion.of(slug)`: sealed lookups, not named values; both recipes ignore
  them, and they stay the C1 known gap.

## Anti-goals

- A deserializer that refuses an invalid value. One bad slug in a catalog file would fail the
  whole load, and the type cannot know whether its consumer cares.
- A made-up placeholder value for something unreadable (C1).

## Approaches considered

1. **Refuse in the deserializer** (B2 custom-deserializer style: `notBlank`, `kebabFormat`,
   `throwWhenInvalid()`). Free 400s at a JSON edge, but the same deserializer reads catalogs
   and stored rows, so a bad value fails every read. Rejected.
2. **Hand on; the consumer observes** — chosen (Pat, 2026-11-18).
3. **Keep per-subtype `@JsonCreator`**, add only the controller fixes. Leaves 92 copies of the
   strategy and nothing for a recipe to standardize on. Rejected.

## Design

### The strategy (`kernels/framework`, package `com.naturalist.ddd`)

One strategy, two entry points (Jackson and Spring's conversion), so a value reads the same
from a catalog file, a stored row and a URL.

- **`NamedValues.read(Class<T> type, String text)`** finds the subtype's
  `public static X of(<value type>)` by reflection (cached per class) and reads `text` as that
  value type: `String` as is, `BigDecimal` (the 17 `NumericNamedValue` records in measurements,
  soil and chemistry), `UUID` (the 39 `EntityId` subtypes). Text that cannot be read as the
  value type (`"abc"` for a number or an id) → `null`, like a C1 lookup; the consumer's kind
  constraint (`entityId`, `namedValue`, …) counts it. `null` → `null`.
- **`NamedValueDeserializer`**: `@JsonDeserialize(using = NamedValueDeserializer.class)` on
  `EntityName`, `NamedValue` and `EntityId`. Contextual: `createContextual` takes the concrete
  subtype from `ctxt.getContextualType()` (not the `BeanProperty`'s type: for a list element or
  a map value that is the container). It reads the token as the factory's value type (so a JSON
  number reaches a `BigDecimal` factory, a string a `String` or `UUID` one) and hands the
  result on, valid or not.
- A slug or file name is always built; validity is the consumer's question.

Spike (scratchpad, Jackson 2.21.5): a supertype `@JsonDeserialize` wins over a subtype's own
`@JsonCreator`, for a field, a list element and a map value; serialization is unchanged. Not yet
proven: an annotation on an *interface* (`NamedValue`) is picked up the same way — the first
test settles it.

### Edges

```java
@GetMapping("/{name}")
String detail(@PathVariable InsectSpeciesName name, ...) {
    MethodObserver methodObserver = observer.forMethod("detail");
    if (invalidName(methodObserver, name)) {
        return "redirect:/insects";   // today's answer for an unknown name
    }
    ...
}

// beside notesTooLong; one per controller
private static boolean invalidName(MethodObserver methodObserver, EntityName name) {
    return !methodObserver.arguments(i -> i.entityName(name, "name"))
            .observe(Level.WARN)
            .violations().isEmpty();
}
```

**`NamedValueConverterFactory`** (`ConverterFactory<String, …>` for each of the three
supertypes, Spring side, so it cannot live in `kernels/framework`) delegates to
`NamedValues.read`. It is registered in `apps/management-console` `WebConfiguration` and in the
console slice-test configuration (`adapters/spring-console-test`), so `@WebMvcTest` slices
convert the same way. Without it an id cannot convert at all: id types have only `of(UUID)`,
which Spring's default `of(String)` lookup does not match, so a malformed
`@RequestParam InsectObservationId` would be an uncounted 400 instead of today's redirect
(`InsectsController.updateNotes` today parses `observationId` by hand). With it, a malformed id
converts to `null` and the edge's `entityId(id, "id")` counts it — **only when the parameter is
`required = false` (or `Optional`)**: Spring 7 refuses a `null` conversion of a required parameter
itself (`handleMissingValueAfterConversion`, an uncounted 400) before the handler runs. A
`String`-backed name never converts to `null`, so its parameters stay required; step 3's
`TypedPathVariable` marks an id or numeric handler parameter that is still required. (Final
review, 2026-11-19.)

Where the body used the String (`"redirect:/insects/" + name`), it uses `name.value()`.

### Recipes (`tooling/naturalist-rewrite`)

| Recipe | Enforces | Fix | Today |
|---|---|---|---|
| `NamedValueHasFactory` | Every concrete subtype of `EntityName`, `NamedValue` (numeric included), `EntityId` declares exactly one `public static X of(<value type>)` — the strategy's contract, caught at build instead of at runtime. | Marker | expected 0; counted when built |
| `NoSubtypeJsonCreator` | A subtype does not redeclare `@JsonCreator` on `of`; the supertype owns it. | Removes it | ~92; counted when built |
| `TypedPathVariable` | A web handler's `@PathVariable` / `@RequestParam` of type `String` that the method turns into a named value (`XName.of(p)`, `XId.of(UUID.fromString(p))`) takes that type instead. Handler methods only. | Marker | ~25; the recipe's first report-only run is the baseline |

## Order (each a small diff that passes the full gate)

1. `NamedValues.read` + `NamedValueDeserializer` on the three supertypes +
   `NamedValueConverterFactory` registered in the app and the slice-test config, with tests
   (a String, a numeric and an id subtype as a field, list element and map value; an invalid
   slug is handed on; non-numeric text and a non-UUID are null; a path variable and a request
   param convert through the factory), plus `NamedValueHasFactory` armed in
   `EnforceArchitecture`.
2. `NoSubtypeJsonCreator`, then `mvn rewrite:run`: 92 mechanical deletions and nothing else;
   armed in `EnforceArchitecture`.
3. `TypedPathVariable` lands report-only (its own composite, outside `EnforceArchitecture`),
   then one controller per diff, insects first (7 endpoints), so each diff watches the count
   drop. Armed in `EnforceArchitecture` with the last controller.

## Decisions

- Hand on; the consumer observes (option A). Pat, 2026-11-18.
- The order above. Pat, 2026-11-18.
- Numeric named values are in: the strategy reads the factory's value type. Pat, 2026-11-19.
- A Spring `ConverterFactory` delegates to the same strategy, so ids convert and a malformed one
  is counted at the edge. Pat, 2026-11-19.
- *My call:* `Clade` / `Bioregion` lookups are out of scope (not named values).
- Map keys are the strategy's too (Pat, 2026-11-19): `keyUsing = NamedValueKeyDeserializer.class` on the
  three supertypes reads a key through `NamedValues.read`; writing a key already goes through `@JsonValue`.
  Without it, a name key read only through the subtype's `@JsonCreator` (which step 2 removes) and an id or
  numeric key could not be read at all (spike, Jackson 2.21.5). *My call:* an unreadable id or numeric key is a
  `null` key, like a value.
- *My call:* `invalidName` is a per-controller helper, like `notesTooLong`, rather than a shared
  console module.
- *My call:* the deserializer observes nothing for an invalid value. A missing factory (a defect
  `NamedValueHasFactory` prevents) and a failed reflective call are noticed (`state` /
  `noticeError ... WRAPPED`) and thrown.

## Left to the builder

- Recipe test style: follow the existing recipes' tests.
- Whether an interface-level annotation needs a mix-in or `@JsonDeserialize` on each
  `NamedValue` record instead, if the first test shows Jackson ignores it.
