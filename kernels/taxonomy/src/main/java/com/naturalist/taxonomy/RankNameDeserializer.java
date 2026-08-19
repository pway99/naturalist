package com.naturalist.taxonomy;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;

import java.io.IOException;

/**
 * Reads a {@code {"rank":…,"value":…}} object and rebuilds the typed permit via the
 * {@link RankNameReconstructor} injected on the mapper. Fails loudly if no reconstructor
 * was registered — a mapper reading observation JSON must supply one.
 */
public class RankNameDeserializer extends JsonDeserializer<RankName> {
    @Override
    public RankName deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        JsonNode node = p.readValueAsTree();
        String rankText = node.get("rank").asText();
        String slug = node.get("value").asText();
        Object injected = ctxt.findInjectableValue(
                RankNameReconstructor.class.getName(), null, null);
        if (!(injected instanceof RankNameReconstructor reconstructor)) {
            throw new IllegalStateException(
                    "No RankNameReconstructor registered on this ObjectMapper; "
                            + "register one via InjectableValues before reading RankName subjects.");
        }
        return reconstructor.reconstruct(slug, LinealRank.valueOf(rankText));
    }
}
