package com.naturalist.ddd;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.deser.ContextualDeserializer;
import com.naturalist.observability.Observer;
import org.jspecify.annotations.Nullable;

import java.io.IOException;

/**
 * Reads every named value through {@link NamedValues}: declared once on {@link EntityName},
 * {@link NamedValue} and {@link EntityId}, so no subtype needs its own {@code @JsonCreator}.
 *
 * <p>The concrete subtype comes from the contextual type, which for a list element or a map value is the
 * element type (a {@code BeanProperty}'s type would be the container). A scalar token is read as text and
 * handed on, valid or not; anything else (an object, an array) is refused and counted ({@code scalarToken}). A property-level
 * {@code @JsonDeserialize} (the {@code RankName} fields) still wins over this class-level one.
 */
public final class NamedValueDeserializer extends JsonDeserializer<Object> implements ContextualDeserializer {

    private static final Observer observer = Observer.forClass(NamedValueDeserializer.class);

    private final @Nullable Class<?> type;

    /** For {@code @JsonDeserialize(using = ...)}; Jackson then asks for a contextual instance. */
    public NamedValueDeserializer() {
        this(null);
    }

    private NamedValueDeserializer(@Nullable Class<?> type) {
        this.type = type;
    }

    @Override
    public JsonDeserializer<?> createContextual(DeserializationContext ctxt, @Nullable BeanProperty property) {
        JavaType contextual = ctxt.getContextualType();
        return contextual == null ? this : new NamedValueDeserializer(contextual.getRawClass());
    }

    @Override
    public @Nullable Object deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        JsonToken token = p.currentToken();
        // an object or array where a named value belongs: a malformed file or request, refused with a count (B2)
        observer.arguments("deserialize", i -> i
                        .isTrue(token != null && token.isScalarValue(), "scalarToken"))
                .throwWhenInvalid();
        return NamedValues.read(type, p.getValueAsString());
    }
}
