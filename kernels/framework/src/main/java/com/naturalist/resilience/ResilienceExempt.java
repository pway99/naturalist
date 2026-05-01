package com.naturalist.resilience;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares that a class or method is intentionally exempt from resilience
 * protection — typically because the call is in-process, side-effect-free, and
 * not subject to timeout. The {@code reason} is required and surfaces in
 * source review and the M9 compliance report.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface ResilienceExempt {

    String reason();
}
