package com.naturalist.textgeneration;

import com.naturalist.vision.ToolResult;
import com.naturalist.vision.ToolSchema;

/**
 * Text-only generation port — symmetric with {@link com.naturalist.vision.VisionService}
 * but without an image. Used for generating structured content (Durrell descriptions,
 * diagnostic features) from textual source material.
 */
public interface TextGenerationService {

    ToolResult generate(ToolSchema tool, String systemPrompt, String userPrompt);
}
