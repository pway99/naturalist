package com.naturalist.vision;

/**
 * Default for tests and apps that don't wire vision. Throws on use —
 * vision is an explicit opt-in, not a silent degradation.
 */
public class NoOpVisionService implements VisionService {

    @Override
    public ToolResult identify(Image image, ToolSchema tool, String systemPrompt) {
        throw new UnsupportedOperationException(
                "VisionService is not configured. Set ANTHROPIC_API_KEY and wire the Anthropic adapter.");
    }
}
