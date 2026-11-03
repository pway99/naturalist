package com.naturalist.textgeneration.anthropic;

/**
 * Configuration for the Anthropic text generation adapter. All fields have
 * sensible defaults; the credential comes from the kernel {@code SecretProvider},
 * not from this config (ADR-028).
 */
public record AnthropicTextGenerationConfig(
        String model,
        int maxTokens
) {
    public static AnthropicTextGenerationConfig defaults() {
        return new AnthropicTextGenerationConfig("claude-haiku-4-5-20251001", 4096);
    }
}
