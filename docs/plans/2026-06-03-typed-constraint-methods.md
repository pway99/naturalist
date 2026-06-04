# Typed Constraint Methods Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add `aggregate`, `aggregateOrNull`, `behavioralCollection`, and `whenNotNull` methods to `Constraints`, then update `Insect` and rank aggregate `invariants()` to use them.

**Architecture:** Pure API-surface expansion — all new descent methods create the same `ObservableConstraint` (or a new `AggregateOrNullConstraint` for the nullable case). `whenNotNull` is a conditional guard on the builder, not a new constraint type. The constraint graph is identical before and after; existing tests are the green-to-green safety net.

**Tech Stack:** Java records, `kernels/framework` observability, `domains/insects/insects-api`

---

### Task 1: Create AggregateOrNullConstraint

**Files:**
- Create: `kernels/framework/src/main/java/com/naturalist/observability/constraints/AggregateOrNullConstraint.java`

- [ ] **Step 1: Create AggregateOrNullConstraint record**

This follows the identical structure of `ValueObjectOrNullConstraint` and `NamedEntityOrNullConstraint`, but type-bound to `Aggregate`.

```java
package com.naturalist.observability.constraints;

import com.naturalist.ddd.Aggregate;
import com.naturalist.observability.Constraint;
import com.naturalist.observability.ConstraintCollection;
import com.naturalist.observability.Constraints;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Constraint for a nullable {@link Aggregate} component -- the canonical encoding of
 * "this child aggregate is either null or a meaningful aggregate."
 * <p>
 * A null reference passes: the field's {@code @Nullable} declaration is respected, and
 * the absent state carries no invariants. A non-null reference is descended into,
 * surfacing the referenced aggregate's own {@link Aggregate#invariants()} to the
 * graph walker.
 * <p>
 * <b>Meaningfulness is the child's responsibility.</b> This constraint guarantees the
 * child's invariants are actually walked when the reference is present, so a
 * semantically-empty aggregate cannot hide behind a silent parent.
 * <p>
 * Distinct from {@link ObservableConstraint} -- which requires a non-null reference --
 * as its own type rather than a boolean flag so the intent is visible at call sites
 * and in stack traces, and so review tools can grep for the pattern. Sibling to
 * {@link ValueObjectOrNullConstraint} and {@link NamedEntityOrNullConstraint}, which
 * carry the same shape for their respective branches.
 */
public record AggregateOrNullConstraint<O, V extends Aggregate>(
        O o,
        Function<O, V> valueFunction,
        String name
) implements Constraint<V>, ConstraintCollection {

    @Override
    public V value() {
        return o == null ? null : valueFunction.apply(o);
    }

    @Override
    public boolean isValid() {
        return true;
    }

    @Override
    public Set<Constraint<?>> constraints() {
        if (o == null) {
            return Set.of();
        }
        V v = valueFunction.apply(o);
        if (v == null) {
            return Set.of();
        }
        Constraints accumulator = new Constraints();
        @SuppressWarnings({"unchecked", "rawtypes"})
        Consumer<Constraints> consumer = (Consumer) v.invariants();
        consumer.accept(accumulator);
        return new LinkedHashSet<>(accumulator.collected());
    }

    @Override
    public AggregateOrNullConstraint<O, V> withName(String name) {
        return new AggregateOrNullConstraint<>(o, valueFunction, name);
    }
}
```

- [ ] **Step 2: Stage**

```bash
git add kernels/framework/src/main/java/com/naturalist/observability/constraints/AggregateOrNullConstraint.java
```

---

### Task 2: Add new methods to Constraints

**Files:**
- Modify: `kernels/framework/src/main/java/com/naturalist/observability/Constraints.java`

- [ ] **Step 1: Add aggregate methods**

Add after the existing `namedEntityOrNull` block (around line 36), before the `valueObject` block:

```java
/**
 * Validate a non-null {@link Aggregate} child and descend into its invariants.
 * Type-specific counterpart to {@link #observable} for aggregate children.
 */
public <A extends Aggregate> Constraints aggregate(A aggregate, String name) {
    return aggregate(aggregate, Function.identity(), name);
}

public <O, A extends Aggregate> Constraints aggregate(O o, Function<O, A> valueFunction, String name) {
    return add(new ObservableConstraint<>(o, valueFunction, name));
}

/**
 * Null-tolerant variant of {@link #aggregate}. A null reference passes
 * (the field's {@code @Nullable} declaration is respected); a non-null
 * reference is descended into and its own {@code invariants()} are walked
 * by the graph walker. Use for {@code @Nullable Aggregate} record
 * components -- most commonly nullable child aggregates on a parent
 * aggregate (e.g. {@code @Nullable InsectSpeciesAggregate species} on
 * {@code Insect}).
 */
public <A extends Aggregate> Constraints aggregateOrNull(A aggregate, String name) {
    return aggregateOrNull(aggregate, Function.identity(), name);
}

public <O, A extends Aggregate> Constraints aggregateOrNull(O o, Function<O, A> valueFunction, String name) {
    return add(new AggregateOrNullConstraint<>(o, valueFunction, name));
}
```

- [ ] **Step 2: Add behavioralCollection methods**

Add after the `valueObjectCollection` block (around line 61), before the `observable` block:

```java
/**
 * Validate a non-null {@link BehavioralCollection} child and descend into its
 * invariants. Type-specific counterpart to {@link #observable} for behavioral
 * collection children -- most commonly an {@code ImageCollection} or
 * {@code LifeStageCollection} held by an aggregate.
 */
public <B extends BehavioralCollection<?>> Constraints behavioralCollection(B collection, String name) {
    return behavioralCollection(collection, Function.identity(), name);
}

public <O, B extends BehavioralCollection<?>> Constraints behavioralCollection(O o, Function<O, B> valueFunction, String name) {
    return add(new ObservableConstraint<>(o, valueFunction, name));
}
```

- [ ] **Step 3: Add whenNotNull method**

Add before the `collected()` method (around line 222):

```java
/**
 * Conditional constraint guard -- runs the block only when {@code value} is
 * non-null. Constraints added inside the block are appended to the same
 * builder (flat, not nested). Use for invariants that are conditional on a
 * nullable field's presence -- most commonly monotonic-fill rules where one
 * rank's presence implies another's.
 *
 * <p>Not a constraint type -- no {@link Constraint} is created. This is pure
 * control flow over the builder.
 */
public Constraints whenNotNull(@Nullable Object value, Consumer<Constraints> block) {
    if (value != null) {
        block.accept(this);
    }
    return this;
}
```

- [ ] **Step 4: Add missing import**

Add `org.jspecify.annotations.Nullable` to the import block — needed by `whenNotNull`'s parameter annotation. The `AggregateOrNullConstraint` import is already covered by the existing wildcard `import com.naturalist.observability.constraints.*;`. Also add `java.util.function.Consumer` if not already present (needed by `whenNotNull`'s parameter type) — verify against existing imports first.

- [ ] **Step 5: Stage**

```bash
git add kernels/framework/src/main/java/com/naturalist/observability/Constraints.java
```

---

### Task 3: Update rank aggregate invariants to use behavioralCollection

**Files:**
- Modify: `domains/insects/insects-api/src/main/java/com/naturalist/insects/InsectOrderAggregate.java:32-36`
- Modify: `domains/insects/insects-api/src/main/java/com/naturalist/insects/InsectFamilyAggregate.java:32-36`
- Modify: `domains/insects/insects-api/src/main/java/com/naturalist/insects/InsectGenusAggregate.java:32-36`
- Modify: `domains/insects/insects-api/src/main/java/com/naturalist/insects/InsectSpeciesAggregate.java:32-36`

- [ ] **Step 1: Update InsectOrderAggregate.invariants()**

Change:
```java
@Override
public Consumer<? extends Constraints> invariants() {
    return i -> i
            .namedEntity(order, "order")
            .observable(images, "images");
}
```

To:
```java
@Override
public Consumer<? extends Constraints> invariants() {
    return i -> i
            .namedEntity(order, "order")
            .behavioralCollection(images, "images");
}
```

- [ ] **Step 2: Update InsectFamilyAggregate.invariants()**

Change `.observable(images, "images")` to `.behavioralCollection(images, "images")`. Same pattern as step 1.

- [ ] **Step 3: Update InsectGenusAggregate.invariants()**

Same change.

- [ ] **Step 4: Update InsectSpeciesAggregate.invariants()**

Same change.

- [ ] **Step 5: Stage**

```bash
git add domains/insects/insects-api/src/main/java/com/naturalist/insects/InsectOrderAggregate.java \
       domains/insects/insects-api/src/main/java/com/naturalist/insects/InsectFamilyAggregate.java \
       domains/insects/insects-api/src/main/java/com/naturalist/insects/InsectGenusAggregate.java \
       domains/insects/insects-api/src/main/java/com/naturalist/insects/InsectSpeciesAggregate.java
```

---

### Task 4: Update Insect.invariants() to use all three new methods

**Files:**
- Modify: `domains/insects/insects-api/src/main/java/com/naturalist/insects/Insect.java:131-148`

- [ ] **Step 1: Rewrite Insect.invariants()**

Change:
```java
@Override
public Consumer<? extends Constraints> invariants() {
    return i -> {
        // Required collections: null -> violation; non-null -> descend.
        i.observable(observations, "observations");
        i.observable(lifeStages, "lifeStages");
        // Monotonic fill: each level conditional on the level below being present.
        // Reports the immediate missing field; consumer infers the cross-rank semantic.
        if (species != null) i.notNull(genus, "genus");
        if (genus != null) i.notNull(family, "family");
        if (family != null) i.notNull(order, "order");
        // Descent into present rank aggregates.
        if (order != null) i.observable(order, "order");
        if (family != null) i.observable(family, "family");
        if (genus != null) i.observable(genus, "genus");
        if (species != null) i.observable(species, "species");
    };
}
```

To:
```java
@Override
public Consumer<? extends Constraints> invariants() {
    return i -> {
        i.behavioralCollection(observations, "observations");
        i.behavioralCollection(lifeStages, "lifeStages");
        // Monotonic fill: each rank requires the one above.
        i.whenNotNull(species, c -> c.notNull(genus, "genus"));
        i.whenNotNull(genus, c -> c.notNull(family, "family"));
        i.whenNotNull(family, c -> c.notNull(order, "order"));
        // Descent into present rank aggregates.
        i.aggregateOrNull(order, "order");
        i.aggregateOrNull(family, "family");
        i.aggregateOrNull(genus, "genus");
        i.aggregateOrNull(species, "species");
    };
}
```

- [ ] **Step 2: Remove unused import**

The `java.util.function.Consumer` import is still needed (for `invariants()` return type). The `java.util.Optional` import is still needed (for `identifiedTo()`). No import changes needed -- the new `Constraints` methods are already visible through the existing import.

- [ ] **Step 3: Stage**

```bash
git add domains/insects/insects-api/src/main/java/com/naturalist/insects/Insect.java
```

---

### Task 5: Verify green-to-green

This is a behavior-preserving refactoring. The constraint graph is identical -- the same `ObservableConstraint` and `NotNullConstraint` instances are created with the same names. All existing tests must pass unchanged.

- [ ] **Step 1: Verify InsectAggregateTest passes**

The eight tests in `InsectAggregateTest` validate the rank aggregates' invariant paths. The violation names `.agg.order`, `.agg.family`, `.agg.genus`, `.agg.species`, and `.agg.images` are unchanged because `behavioralCollection(images, "images")` creates the same `ObservableConstraint` with the same `"images"` name.

- [ ] **Step 2: Verify InsectTest passes**

The twelve invariant tests in `InsectTest` validate `Insect`'s monotonic-fill and collection invariants. The violation names `.insect.observations`, `.insect.lifeStages`, `.insect.genus`, `.insect.family`, `.insect.order` are unchanged because:
- `behavioralCollection(x, "name")` creates `ObservableConstraint` with `"name"` -- same as `observable(x, "name")`
- `whenNotNull(x, c -> c.notNull(y, "name"))` adds `NotNullConstraint` with `"name"` -- same as `if (x != null) i.notNull(y, "name")`
- `aggregateOrNull(x, "name")` creates `AggregateOrNullConstraint` with `"name"` -- same descent behavior as `ObservableConstraint`, same null-tolerance as the old `if (x != null) i.observable(x, "name")` pattern

**Note:** The user runs `mvn verify` locally. After all tasks are staged, the user should verify the build passes before committing.
