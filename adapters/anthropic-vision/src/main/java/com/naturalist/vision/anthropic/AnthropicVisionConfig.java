package com.naturalist.vision.anthropic;

/**
 * Configuration for the Anthropic Vision adapter. All fields have sensible
 * defaults; the API key comes from the {@code ANTHROPIC_API_KEY} environment
 * variable, not from this config.
 */
public record AnthropicVisionConfig(
        String model,
        int maxTokens
) {
    public static AnthropicVisionConfig defaults() {
        return new AnthropicVisionConfig("claude-sonnet-4-6", 4096);
    }
}
