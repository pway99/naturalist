package com.naturalist.vision;

/**
 * Default for tests and apps that don't wire vision. Throws on use —
 * vision is an explicit opt-in, not a silent degradation.
 */
public class NoOpVisionService implements VisionService {

    @Override
    public VisionExchange identify(Image image, ToolSchema tool, String systemPrompt) {
        throw new UnsupportedOperationException(
                "VisionService is not configured: set naturalist.vision.provider=anthropic"
                        + " and supply the model-credential secret (see ADR-028).");
    }
}
