package com.naturalist.taxonomy;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.naturalist.observability.Observer;

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
        OBSERVER.state("deserialize", s -> s
                .isTrue(injected instanceof RankNameReconstructor, "rankNameReconstructor"))
                .throwWhenInvalid();
        return ((RankNameReconstructor) injected).reconstruct(slug, LinealRank.valueOf(rankText));
    }

    private static final Observer OBSERVER = Observer.forClass(RankNameDeserializer.class);
}
