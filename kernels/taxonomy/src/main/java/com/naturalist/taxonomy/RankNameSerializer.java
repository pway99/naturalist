package com.naturalist.taxonomy;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;

/** Writes a {@link RankName} as a self-describing {@code {"rank":…,"value":…}} object. */
public class RankNameSerializer extends JsonSerializer<RankName> {
    @Override
    public void serialize(RankName value, JsonGenerator gen, SerializerProvider serializers)
            throws IOException {
        gen.writeStartObject();
        gen.writeStringField("rank", value.rank().name());
        gen.writeStringField("value", value.value());
        gen.writeEndObject();
    }
}
