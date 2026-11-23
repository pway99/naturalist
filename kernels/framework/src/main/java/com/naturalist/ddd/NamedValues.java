package com.naturalist.ddd;

import com.naturalist.observability.Handling;
import com.naturalist.observability.MethodObserver;
import com.naturalist.observability.Observer;
import org.jspecify.annotations.Nullable;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * The one way a named value ({@link EntityName}, {@link NamedValue}, {@link EntityId}) is read from text,
 * whether the text came from JSON ({@link NamedValueDeserializer}) or a URL (the Spring converter).
 *
 * <p>The subtype's factory is its single-argument {@code static of(String | BigDecimal | UUID)} ({@code public}
 * in main source, which {@code NamedValueHasFactory} enforces; a test fixture's may be package-private);
 * an {@code of} taking anything else, or more than one argument, composes a name and is not the factory. A type
 * with no factory, or with two (which one ran would depend on reflection order), is refused.
 * The value is handed on as given: a slug or file name is always built, valid or not, because only the
 * consumer knows whether it needs a valid one (P6, J4). Text that cannot be read as the factory's value
 * type (not a number, not a UUID) is {@code null}, like any lookup of an unrecognised value (C1), and the
 * consumer's kind constraint counts it.
 */
public final class NamedValues {

    private static final Observer observer = Observer.forClass(NamedValues.class);

    private static final Pattern DECIMAL = Pattern.compile("[+-]?(\\d+(\\.\\d*)?|\\.\\d+)([eE][+-]?\\d{1,4})?");
    private static final Pattern UUID_TEXT = Pattern.compile(
            "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}");

    /** Every qualifying factory: one is the factory, none or several is a type {@code read} refuses. */
    private static final ClassValue<List<Method>> FACTORIES = new ClassValue<>() {
        @Override
        protected List<Method> computeValue(Class<?> type) {
            return Arrays.stream(type.getDeclaredMethods())
                    .filter(m -> m.getName().equals("of"))
                    .filter(m -> Modifier.isStatic(m.getModifiers()))
                    .filter(m -> m.getParameterCount() == 1)
                    .filter(m -> isValueType(m.getParameterTypes()[0]))
                    .filter(m -> type.isAssignableFrom(m.getReturnType()))
                    .peek(Method::trySetAccessible)
                    .toList();
        }
    };

    private NamedValues() {
    }

    /**
     * Reads {@code text} as a {@code type} through its factory: built as given, or {@code null} when the
     * text is null or cannot be read as the factory's value type. A type with no factory is refused.
     */
    public static <T> @Nullable T read(Class<T> type, @Nullable String text) {
        MethodObserver methodObserver = observer.forMethod("read");
        methodObserver.arguments(i -> i
                .notNull(type, "type"))
                .throwWhenInvalid();
        List<Method> factories = FACTORIES.get(type);
        methodObserver.state(i -> i
                .isTrue(!factories.isEmpty(), "factoryFound")
                .atMost(factories.size(), 1, "factoryUnique"))
                .throwWhenInvalid();
        if (text == null) {
            return null;
        }
        Method of = factories.getFirst();
        Object value = valueOf(of.getParameterTypes()[0], text);
        if (value == null) {
            return null;
        }
        try {
            return type.cast(of.invoke(null, value));
        } catch (InvocationTargetException e) {
            // the factory itself threw: count its own exception, not the reflective wrapper
            Throwable thrown = e.getCause() != null ? e.getCause() : e;
            methodObserver.noticeError("built", thrown, Handling.WRAPPED);
            throw new IllegalStateException("could not build " + type.getName() + " through of(...)", thrown);
        } catch (IllegalAccessException e) {
            methodObserver.noticeError("built", e, Handling.WRAPPED);
            throw new IllegalStateException("could not build " + type.getName() + " through of(...)", e);
        }
    }

    private static boolean isValueType(Class<?> parameter) {
        return parameter == String.class || parameter == BigDecimal.class || parameter == UUID.class;
    }

    /** The text as the factory's value type, or null when it is not one; decided by pattern, so nothing throws. */
    private static @Nullable Object valueOf(Class<?> valueType, String text) {
        if (valueType == String.class) {
            return text;
        }
        String trimmed = text.strip();
        if (valueType == BigDecimal.class) {
            return DECIMAL.matcher(trimmed).matches() ? new BigDecimal(trimmed) : null;
        }
        return UUID_TEXT.matcher(trimmed).matches() ? UUID.fromString(trimmed) : null;
    }
}
