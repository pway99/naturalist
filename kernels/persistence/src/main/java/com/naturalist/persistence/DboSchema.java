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
    // Raw `Named` bound (not `Named<?>`) so a kernel-generic entity — OrganismObservation,
    // OrganismImage, OrganismFeatureAssignment — can be named by its raw class literal (a
    // parameterized class literal is illegal in Java). Inert documentation; the validator ignores it.
    @SuppressWarnings("rawtypes")
    Class<? extends Named> entity();
    String[] unique() default {};
    Fk[] foreignKeys() default {};
}
