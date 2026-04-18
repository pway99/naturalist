package com.naturalist.ddd;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME) // Available at runtime via reflection
@Target(ElementType.FIELD)
public @interface PersistenceIdentifier {
}
