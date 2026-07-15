package com.naturalist.textgeneration;

import com.naturalist.vision.ToolResult;
import com.naturalist.vision.ToolSchema;

/**
 * Default no-op implementation for unit tests and unwired composition roots.
 */
public class NoOpTextGenerationService implements TextGenerationService {

    @Override
    public ToolResult generate(ToolSchema tool, String systemPrompt, String userPrompt) {
        throw new UnsupportedOperationException(
                "TextGenerationService is not wired — configure an adapter.");
    }
}
