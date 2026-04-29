package com.naturalist.atlas;

import com.naturalist.ddd.EntityName;
import com.naturalist.ddd.ValueObject;
import com.naturalist.observability.Constraints;

import java.util.function.Consumer;

/**
 * A typed pointer to a single entity in a participating domain — the value
 * carried by every atlas query result.
 * <p>
 * An {@code EntityRef} couples the entity's natural-key slug
 * ({@link EntityName}) with the {@link DomainId} of the domain that owns it.
 * The pairing is what lets a console renderer follow the reference: the
 * domain decides the URL shape; the name picks the entity within that shape.
 * Atlas itself stores nothing more than this pair — it does not cache the
 * entity's payload, and a stale {@code EntityRef} simply resolves to a
 * &ldquo;not found&rdquo; on the next domain query.
 * <p>
 * Both components are required. A reference with a missing domain or name is
 * an invariant violation, observable via the standard kernel pipeline; it is
 * not silently dropped to an empty result.
 *
 * @param domain the participating domain that owns the entity
 * @param name   the entity's stable natural-key slug
 */
public record EntityRef(DomainId domain, EntityName name) implements ValueObject {

    /**
     * The slug suitable for display in console UI when no richer label is
     * available. Returns the {@link EntityName#value()}; the empty string
     * when {@code name} is null (the invariant pass surfaces the missing
     * name elsewhere).
     */
    public String displayLabel() {
        return name == null ? "" : name.value();
    }

    @Override
    public Consumer<? extends Constraints> invariants() {
        return i -> i
                .valueObject(this, EntityRef::domain, "domain")
                .entityName(name, "name");
    }
}
