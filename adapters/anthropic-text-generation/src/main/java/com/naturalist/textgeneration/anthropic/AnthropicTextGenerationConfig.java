package com.naturalist.textgeneration.anthropic;

/**
 * Configuration for the Anthropic text generation adapter. All fields have
 * sensible defaults; the API key comes from the {@code ANTHROPIC_API_KEY}
 * environment variable, not from this config.
 */
public record AnthropicTextGenerationConfig(
        String model,
        int maxTokens
) {
    public static AnthropicTextGenerationConfig defaults() {
        return new AnthropicTextGenerationConfig("claude-haiku-4-5-20251001", 4096);
    }
}
