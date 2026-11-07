package com.naturalist.test.context;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * A test field holding a hand-written stand-in, set by its initialiser. Before each test
 * {@code NaturalistTestExtension} hands its value to the context registered for the test class's own
 * package, as the fake for the field's declared type, so the whole graph uses it.
 */
@Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
@Target(java.lang.annotation.ElementType.FIELD)
public @interface Fake {
}
