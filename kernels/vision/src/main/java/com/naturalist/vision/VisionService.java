package com.naturalist.vision;

/**
 * Vendor-neutral port for vision-based identification. Domain code calls this
 * interface; the Anthropic adapter (or any future provider) implements it.
 * Follows the Resilience facade precedent: thin kernel port, adapter in
 * {@code adapters/}.
 */
public interface VisionService {

    ToolResult identify(Image image, ToolSchema tool, String systemPrompt);
}
