package com.naturalist.test.context;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * A test field that {@code NaturalistTestExtension} fills from the test's object graph before each test,
 * after every {@link Fake} field has been handed to the test's context. The field must be neither
 * {@code final} nor {@code static}.
 */
@Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
@Target(java.lang.annotation.ElementType.FIELD)
public @interface Wired {
}
