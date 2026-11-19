package com.naturalist.spring;

import com.naturalist.ddd.EntityId;
import com.naturalist.ddd.EntityName;
import com.naturalist.ddd.NamedValue;
import com.naturalist.ddd.NamedValues;
import org.jspecify.annotations.Nullable;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.ConverterRegistry;
import org.springframework.core.convert.converter.GenericConverter;

import java.util.Set;

/**
 * Converts a request's text (a path variable, a request parameter) to a named value through {@link NamedValues},
 * the strategy JSON reads use, so a slug, a file name, a number or an id reads the same from a URL as from a
 * catalog file. The value is handed on as given; an id or number whose text cannot be read is {@code null}.
 *
 * <p>Spring refuses a {@code null} conversion of a <em>required</em> {@code @PathVariable} / {@code @RequestParam}
 * itself (a {@code MissingPathVariableException} / {@code MissingServletRequestParameterException}, an uncounted
 * 400) before the handler runs. So a handler that takes an id or numeric named value declares the parameter
 * {@code required = false} (or {@code Optional}), and observes it (B1). A {@code String}-backed name or file name
 * is never {@code null}, so its parameters stay required.
 */
public final class NamedValueConversion implements GenericConverter {

    private static final Set<ConvertiblePair> PAIRS = Set.of(
            new ConvertiblePair(String.class, EntityName.class),
            new ConvertiblePair(String.class, EntityId.class),
            new ConvertiblePair(String.class, NamedValue.class));

    /** Registers the conversion; ahead of Spring's own {@code of(String)} lookup, which cannot build an id. */
    public static void register(ConverterRegistry registry) {
        registry.addConverter(new NamedValueConversion());
    }

    @Override
    public Set<ConvertiblePair> getConvertibleTypes() {
        return PAIRS;
    }

    @Override
    public @Nullable Object convert(@Nullable Object source, TypeDescriptor sourceType, TypeDescriptor targetType) {
        return NamedValues.read(targetType.getType(), (String) source);
    }
}
