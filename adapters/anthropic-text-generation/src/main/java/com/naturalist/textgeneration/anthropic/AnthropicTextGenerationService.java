package com.naturalist.textgeneration.anthropic;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.anthropic.core.JsonValue;
import com.anthropic.models.messages.CacheControlEphemeral;
import com.anthropic.models.messages.ContentBlockParam;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.TextBlockParam;
import com.anthropic.models.messages.Tool;
import com.anthropic.models.messages.ToolChoice;
import com.anthropic.models.messages.ToolChoiceTool;
import com.anthropic.models.messages.ToolUseBlock;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.naturalist.resilience.Resilient;
import com.naturalist.textgeneration.TextGenerationService;
import com.naturalist.vision.ToolResult;
import com.naturalist.vision.ToolSchema;

import java.util.List;
import java.util.Map;

/**
 * Anthropic Java SDK implementation of {@link TextGenerationService}.
 *
 * <p>Mirrors {@code AnthropicVisionService} but without image content blocks —
 * text-only messages with tool use for structured output. Used for parent rank
 * enrichment (Durrell descriptions grounded in authority content).
 *
 * <p>Prompt caching is enabled: the system prompt block is marked with
 * {@code cache_control: ephemeral} so that the fixed prompt prefix is cached
 * across calls.
 *
 * <p>API key read from {@code ANTHROPIC_API_KEY} environment variable at
 * construction. Refuses to construct if the key is absent.
 */
@Resilient(name = "textgeneration.enrichment")
public class AnthropicTextGenerationService implements TextGenerationService {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final AnthropicTextGenerationConfig config;
    private final AnthropicClient client;

    public AnthropicTextGenerationService(AnthropicTextGenerationConfig config) {
        this.config = config;
        var apiKey = System.getenv("ANTHROPIC_API_KEY");
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException(
                    "ANTHROPIC_API_KEY environment variable is required for text generation");
        }
        this.client = AnthropicOkHttpClient.builder().apiKey(apiKey).build();
    }

    @Override
    public ToolResult generate(ToolSchema tool, String systemPrompt, String userPrompt) {
        // 1. Build the tool definition from the ToolSchema
        var toolDef = buildTool(tool);

        // 2. Build the system prompt block with cache_control for prompt caching
        var systemBlock = TextBlockParam.builder()
                .text(systemPrompt)
                .cacheControl(CacheControlEphemeral.builder().build())
                .build();

        // 3. Build the user text block
        var textBlock = ContentBlockParam.ofText(
                TextBlockParam.builder()
                        .text(userPrompt)
                        .build());

        // 4. Send the request, forcing the model to use the named tool
        var params = MessageCreateParams.builder()
                .model(config.model())
                .maxTokens(config.maxTokens())
                .systemOfTextBlockParams(List.of(systemBlock))
                .addUserMessageOfBlockParams(List.of(textBlock))
                .addTool(toolDef)
                .toolChoice(ToolChoice.ofTool(
                        ToolChoiceTool.builder()
                                .name(tool.name())
                                .build()))
                .build();

        var message = client.messages().create(params);

        // 5. Extract the tool_use block from the response
        var toolUseBlock = message.content().stream()
                .flatMap(block -> block.toolUse().stream())
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "Text generation response contained no tool_use block. " +
                        "stop_reason=" + message.stopReason()));

        // 6. Serialize the raw input JsonValue to a JSON string
        var argumentsJson = serializeInput(toolUseBlock);

        return new ToolResult(toolUseBlock.name(), argumentsJson);
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    @SuppressWarnings("unchecked")
    private static Tool buildTool(ToolSchema schema) {
        try {
            Map<String, Object> schemaMap = MAPPER.readValue(
                    schema.parametersJson(),
                    new TypeReference<>() {});

            var inputSchemaBuilder = Tool.InputSchema.builder();

            var properties = (Map<String, Object>) schemaMap.get("properties");
            if (properties != null && !properties.isEmpty()) {
                var propsBuilder = Tool.InputSchema.Properties.builder();
                for (var entry : properties.entrySet()) {
                    propsBuilder.putAdditionalProperty(
                            entry.getKey(),
                            JsonValue.from(entry.getValue()));
                }
                inputSchemaBuilder.properties(propsBuilder.build());
            }

            var required = (List<String>) schemaMap.get("required");
            if (required != null && !required.isEmpty()) {
                inputSchemaBuilder.required(required);
            }

            return Tool.builder()
                    .name(schema.name())
                    .description(schema.description())
                    .inputSchema(inputSchemaBuilder.build())
                    .build();

        } catch (Exception e) {
            throw new IllegalArgumentException(
                    "Failed to parse ToolSchema.parametersJson for tool '" + schema.name() + "': " + e.getMessage(), e);
        }
    }

    private static String serializeInput(ToolUseBlock toolUseBlock) {
        try {
            return MAPPER.writeValueAsString(toolUseBlock._input());
        } catch (Exception e) {
            throw new IllegalStateException(
                    "Failed to serialize tool use input to JSON: " + e.getMessage(), e);
        }
    }
}
