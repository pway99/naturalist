package com.naturalist.test.context;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * A test field holding a hand-written stand-in, set by its initialiser. Before each test
 * {@code NaturalistTestExtension} hands its value to the context registered for the test class's own
 * package, as the fake for one port, so the whole graph uses it.
 *
 * <p>The port is {@link #value()} when set, and the field's value must be an instance of it; otherwise it is
 * the field's declared type, which must then be an interface or abstract class. A fake registers under that
 * exact type only, so a field declared as the fake's concrete class ({@code @Fake ScriptedVisionService v})
 * would replace nothing: the extension refuses it in {@code beforeEach}. Declare the field as the port, or
 * write {@code @Fake(VisionService.class)}.
 */
@Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
@Target(java.lang.annotation.ElementType.FIELD)
public @interface Fake {
    /**
     * The port the fake replaces. Leave it out only when the field's declared type is that port, an
     * interface or abstract class.
     */
    Class<?> value() default Void.class;
}
