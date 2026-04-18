# Shared Kernels

The kernels are the foundation every other module builds on. Changes here ripple across
the entire codebase — treat them as stable contracts, not convenient places to add things.

## Four Kernels

### framework
DDD building blocks. `Entity`, `Aggregate`, `ValueObject`, `Observable`, `Invariants`,
`EntityId`, `EntityName`, `Observer`. No domain knowledge — pure structural vocabulary.
Everything else depends on this.

### framework-test
Test infrastructure. `TestEntitySource`, `TestEntitySourceTest`, `UniqueConstraint`.
Used by all `<domain>-repository-test` modules. Never a compile-scope dependency.

### field-notes  (`com.naturalist.fieldnotes`)
The Durrell principle in code. `Description` carries the same truth at four levels of
understanding — preschool, elementary, secondary, university — for any entity the system
can describe. Chemistry compounds, climate thresholds, plants, organisms: all of them.

**Every domain api depends on this.** If you are building a new catalog entity and it does
not have a `Description`, that is a deliberate omission worth questioning.

```java
import com.naturalist.fieldnotes.Description;
```

### taxonomy  (`com.naturalist.taxonomy`)
Linnaean classification. `TaxonomicClassification` holds order, family, genus, and species
with `binomialName()` and `isSpeciesLevel()` convenience methods. Nullable genus and species
accommodate family-level field identifications where species cannot be confirmed.

**Organism domain apis only** — insects, arachnids, worms, microbes, molluscs, vertebrates,
plants. Chemistry, climate, soil, zone, and sensors have no use for Linnaean taxonomy.

```java
import com.naturalist.taxonomy.TaxonomicClassification;
```

## DAG Position

```
field-notes  →  framework
taxonomy     →  framework

<any>-api         →  framework, identifiers, field-notes
<organism>-api    →  framework, identifiers, field-notes, taxonomy
```

## Hard Rules

- `identifiers` contains typed IDs and names only — `EntityId` and `EntityName` subclasses.
  No value objects. No behavior. If it is not an identifier, it does not belong there.
- `field-notes` and `taxonomy` contain shared value objects only.
  No entity definitions. No domain-specific logic.
- Do not add a new class to any kernel without considering whether it is truly
  cross-cutting. Kingdom-specific concerns belong in the domain module, not here.
