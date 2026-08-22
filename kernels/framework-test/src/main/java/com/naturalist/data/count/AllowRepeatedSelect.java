package com.naturalist.data.count;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Whitelists a known-legitimate repeated select on a single test method, so the N+1 gate
 * does not fail it. {@code query} matches the head query FQN and {@code select} the repository
 * select FQN — by exact string, {@code .}-suffix, or simple name.
 */
@Repeatable(AllowRepeatedSelect.List.class)
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface AllowRepeatedSelect {

    String query();

    String select();

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.METHOD)
    @interface List {
        AllowRepeatedSelect[] value();
    }
}
