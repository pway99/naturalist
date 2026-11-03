package com.naturalist.vision.anthropic;

/**
 * Configuration for the Anthropic Vision adapter. All fields have sensible
 * defaults; the credential comes from the kernel {@code SecretProvider}, not
 * from this config (ADR-028).
 */
public record AnthropicVisionConfig(
        String model,
        int maxTokens
) {
    public static AnthropicVisionConfig defaults() {
        return new AnthropicVisionConfig("claude-sonnet-4-6", 4096);
    }
}
