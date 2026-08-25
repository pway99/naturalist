package com.naturalist.vision;

/**
 * Vendor-neutral port for vision-based identification. Domain code calls this
 * interface; the Anthropic adapter (or any future provider) implements it.
 * Follows the Resilience facade precedent: thin kernel port, adapter in
 * {@code adapters/}.
 */
public interface VisionService {

    /**
     * Runs one identification turn and returns the multi-turn {@link VisionExchange}.
     * Single-turn callers read {@link VisionExchange#result()}; a caller wanting a
     * second turn continues via {@link VisionExchange#respond}.
     */
    VisionExchange identify(Image image, ToolSchema tool, String systemPrompt);
}
