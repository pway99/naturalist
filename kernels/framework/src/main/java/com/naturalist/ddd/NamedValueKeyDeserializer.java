package com.naturalist.ddd;

import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.KeyDeserializer;
import com.fasterxml.jackson.databind.deser.ContextualKeyDeserializer;
import org.jspecify.annotations.Nullable;

/**
 * Reads a named value used as a map key through {@link NamedValues}, the same strategy its values use.
 * Declared once ({@code keyUsing}) on {@link EntityName}, {@link NamedValue} and {@link EntityId}; writing a
 * key needs nothing, because Jackson writes it through the supertype's {@code @JsonValue}.
 *
 * <p>The contextual type is the map's type, so the concrete key class is its key type. As with values, the
 * key is handed on as given; an id or numeric key whose text cannot be read is a {@code null} key.
 */
public final class NamedValueKeyDeserializer extends KeyDeserializer implements ContextualKeyDeserializer {

    private final @Nullable Class<?> type;

    /** For {@code @JsonDeserialize(keyUsing = ...)}; Jackson then asks for a contextual instance. */
    public NamedValueKeyDeserializer() {
        this(null);
    }

    private NamedValueKeyDeserializer(@Nullable Class<?> type) {
        this.type = type;
    }

    @Override
    public KeyDeserializer createContextual(DeserializationContext ctxt, @Nullable BeanProperty property) {
        JavaType map = ctxt.getContextualType();
        if (map == null && property != null) {
            map = property.getType();
        }
        return map == null || !map.isMapLikeType() ? this : new NamedValueKeyDeserializer(map.getKeyType().getRawClass());
    }

    @Override
    public @Nullable Object deserializeKey(String key, DeserializationContext ctxt) {
        return NamedValues.read(type, key);
    }
}
