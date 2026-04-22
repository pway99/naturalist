package com.naturalist.ddd;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a plain value field as unique within its data source. Fields annotated
 * with {@code @UniqueValue} produce unique constraints in {@code NamedTestEntitySource} —
 * no two entities may share the same value for this field.
 * <p>
 * Use on non-identity fields ({@code String}, {@code int}, enums) that must be
 * unique across all instances. For {@code EntityName} fields, use
 * {@link EntityIdentifier} instead.
 *
 * @see EntityIdentifier
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface UniqueValue {
}
