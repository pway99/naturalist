package com.naturalist.persistence;

import com.naturalist.ddd.Named;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/** Pure, inert persistence metadata recorded alongside a {@link Dbo}. Generates nothing. */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface DboSchema {
    String table();
    String primaryKey();
    Class<? extends Named<?>> entity();
    String[] unique() default {};
    Fk[] foreignKeys() default {};
}
