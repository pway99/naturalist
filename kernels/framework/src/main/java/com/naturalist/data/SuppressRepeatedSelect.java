package com.naturalist.data;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares that this method repeats selects on purpose, because they cannot be batched. The N+1 test
 * gate does not count any read made while this method is on the stack. Use it as a last resort: most
 * repeats are N+1s to fix.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface SuppressRepeatedSelect {

    /** Why these selects cannot be batched. */
    String reason();
}
