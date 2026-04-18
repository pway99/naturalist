package com.naturalist.ddd;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a <em>secondary</em> {@link EntityName} component as a unique identifier
 * within its data source.
 * <p>
 * The canonical {@code name()} of any {@link Entity} is automatically enforced as
 * unique by {@code TestEntitySource} — do not annotate it here.
 * Use {@code @EntityIdentifier} only on additional {@code EntityName} fields on the
 * same entity that must also be unique. Cross-domain FK {@code EntityName} references
 * carry no annotation. For plain value fields ({@code String}, {@code int}, enums) that
 * must be unique but are not {@code EntityName} subclasses, use {@link UniqueValue} instead.
 * <p>
 * Secondary fields annotated with {@code @EntityIdentifier} must still be declared
 * explicitly in the subclass {@code uniqueConstraints()} method. For plain value fields
 * ({@code String}, {@code int}, enums) that must be unique, use {@link UniqueValue} instead.
 *
 * @see UniqueValue
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface EntityIdentifier {
}
