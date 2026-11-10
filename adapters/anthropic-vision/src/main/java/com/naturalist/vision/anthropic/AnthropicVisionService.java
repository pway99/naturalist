package com.naturalist.vision.anthropic;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import com.anthropic.core.JsonValue;
import com.anthropic.models.messages.Base64ImageSource;
import com.anthropic.models.messages.CacheControlEphemeral;
import com.anthropic.models.messages.ContentBlockParam;
import com.anthropic.models.messages.ImageBlockParam;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.MessageParam;
import com.anthropic.models.messages.TextBlockParam;
import com.anthropic.models.messages.Tool;
import com.anthropic.models.messages.ToolChoice;
import com.anthropic.models.messages.ToolChoiceTool;
import com.anthropic.models.messages.ToolResultBlockParam;
import com.anthropic.models.messages.ToolUseBlock;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.naturalist.resilience.Resilience;
import com.naturalist.resilience.Resilient;
import com.naturalist.secrets.Secret;
import com.naturalist.secrets.SecretName;
import com.naturalist.secrets.SecretProvider;
import com.naturalist.vision.Image;
import com.naturalist.vision.ToolResult;
import com.naturalist.vision.ToolSchema;
import com.naturalist.vision.VisionExchange;
import com.naturalist.vision.VisionService;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.apache.commons.lang3.StringUtils;

/**
 * Anthropic Java SDK implementation of {@link VisionService}.
 *
 * <p>Uses tool use to return structured identification data. Prompt caching is enabled:
 * the system prompt block is marked with {@code cache_control: ephemeral} so that the
 * fixed prompt prefix is cached across calls and only the image bytes vary per request.
 *
 * <p>The credential is the {@link #CREDENTIAL} secret, supplied by the kernel
 * {@link SecretProvider} and resolved once at construction. It is never read from the
 * process environment, nor through the SDK's own environment lookup (ADR-028).
 * Construction fails with {@code MissingSecretException} when the source lacks it.
 *
 * <h2>Image bound</h2>
 * Before encoding, the image passes through {@link ImageDownscaler#fit}: a decodable image
 * larger than {@value ImageDownscaler#MAX_EDGE}&nbsp;px on its longest edge is sent as a JPEG
 * at that edge, and the media type sent is the one that describes the bytes actually sent.
 * WebP and undecodable bytes go as uploaded, bounded by {@code ImageUpload.MAX_BYTES}.
 *
 * <h2>Resilience</h2>
 * The HTTP call is bounded by the {@value #STRATEGY} timeout, applied through the
 * {@link Resilience} facade. Only {@code client.messages().create(...)} is wrapped —
 * base64 encoding, tool-schema construction, and response parsing stay on the calling
 * thread, so the adapter's worker-thread hop covers the network call and nothing else.
 *
 * <p>The {@value #STRATEGY} rate limit is enforced by the caller
 * ({@code InsectIdentificationCommandImpl}, behind {@code InsectCommand.identification()}), one permit per identification, before this
 * service is ever invoked — not here, and not per vision turn. This adapter no longer
 * wraps the call in a rate limiter.
 */
@Resilient(name = AnthropicVisionService.STRATEGY)
public class AnthropicVisionService implements VisionService {

    static final String STRATEGY = "vision.identification";

    /** The one secret this adapter resolves, at construction (ADR-028). */
    static final SecretName CREDENTIAL = SecretName.of("model-credential");

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final AnthropicVisionConfig config;
    private final AnthropicClient client;
    private final Resilience resilience;

    public AnthropicVisionService(AnthropicVisionConfig config, SecretProvider secrets, Resilience resilience) {
        this.config = Objects.requireNonNull(config, "config");
        this.resilience = Objects.requireNonNull(resilience, "resilience");
        Secret credential = Objects.requireNonNull(secrets, "secrets").require(CREDENTIAL);
        this.client = AnthropicOkHttpClient.builder().apiKey(credential.value()).build();
    }

    @Override
    public VisionExchange identify(Image image, ToolSchema tool, String systemPrompt) {
        // 1. Fit the image within the vendor's longest edge, then base64-encode it
        var sized = ImageDownscaler.fit(image.bytes(), image.mediaType());
        var base64Data = Base64.getEncoder().encodeToString(sized.bytes());

        // 2. Determine media type
        var mediaType = resolveMediaType(sized.mediaType());

        // 3. Build the image content block
        var imageBlock = ContentBlockParam.ofImage(
                ImageBlockParam.builder()
                        .source(Base64ImageSource.builder()
                                .data(base64Data)
                                .mediaType(mediaType)
                                .build())
                        .build());

        // 4. Build the user text block (with optional location context)
        var userText = buildUserText(image);
        var textBlock = ContentBlockParam.ofText(
                TextBlockParam.builder()
                        .text(userText)
                        .build());

        // 5. Build the system prompt block with cache_control for prompt caching.
        //    The system prompt and tool schema are cache-stable across calls;
        //    only the image varies, so caching the system prefix reduces cost on
        //    repeated identifications.
        var systemBlock = TextBlockParam.builder()
                .text(systemPrompt)
                .cacheControl(CacheControlEphemeral.builder().build())
                .build();

        // 6. Seed the conversation with the user's image message and issue turn 1.
        var userMessage = MessageParam.builder()
                .role(MessageParam.Role.USER)
                .contentOfBlockParams(List.of(imageBlock, textBlock))
                .build();

        return issueRequest(List.of(userMessage), tool, systemBlock);
    }

    /**
     * Issues one turn: forces {@code tool}, sends {@code messages} (with the cached
     * {@code systemBlock}), and wraps the outcome in a {@link VisionExchange} that can
     * continue the conversation. Only {@code client.messages().create(...)} is bounded
     * by the resilience timeout; block extraction stays on the calling thread.
     */
    private VisionExchange issueRequest(
            List<MessageParam> messages, ToolSchema tool, TextBlockParam systemBlock) {

        var toolDef = buildTool(tool);

        var params = MessageCreateParams.builder()
                .model(config.model())
                .maxTokens(config.maxTokens())
                .systemOfTextBlockParams(List.of(systemBlock))
                .messages(messages)
                .addTool(toolDef)
                .toolChoice(ToolChoice.ofTool(
                        ToolChoiceTool.builder()
                                .name(tool.name())
                                .build()))
                .build();

        var message = resilience.timeout(STRATEGY).execute(() -> client.messages().create(params));

        var toolUseBlock = message.content().stream()
                .flatMap(block -> block.toolUse().stream())
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "Vision identification response contained no tool_use block. " +
                        "stop_reason=" + message.stopReason()));

        return new AnthropicVisionExchange(messages, systemBlock, toolUseBlock);
    }

    /**
     * A tool-use conversation over the accumulated {@link MessageParam} list and the
     * latest assistant {@link ToolUseBlock}. {@link #respond} appends the assistant
     * tool_use and the user tool_result (keyed by the tool_use id) and re-issues the
     * request, threading the growing message list into the next exchange.
     */
    private final class AnthropicVisionExchange implements VisionExchange {

        private final List<MessageParam> messages;
        private final TextBlockParam systemBlock;
        private final ToolUseBlock block;

        AnthropicVisionExchange(
                List<MessageParam> messages, TextBlockParam systemBlock, ToolUseBlock block) {
            this.messages = messages;
            this.systemBlock = systemBlock;
            this.block = block;
        }

        @Override
        public ToolResult result() {
            return new ToolResult(block.name(), serializeInput(block));
        }

        @Override
        public VisionExchange respond(String toolResultJson, ToolSchema nextTool) {
            // Replay the assistant's tool_use, then answer it with the user's tool_result.
            // Only the tool_use block is replayed (no sibling text): forced toolChoice
            // always yields a lone tool_use block, so nothing is lost -- revisit if
            // toolChoice ever becomes non-forced.
            var assistantMessage = MessageParam.builder()
                    .role(MessageParam.Role.ASSISTANT)
                    .contentOfBlockParams(List.of(ContentBlockParam.ofToolUse(block.toParam())))
                    .build();
            var toolResultBlock = ToolResultBlockParam.builder()
                    .toolUseId(block.id())
                    .content(toolResultJson)
                    .build();
            var userMessage = MessageParam.builder()
                    .role(MessageParam.Role.USER)
                    .contentOfBlockParams(List.of(ContentBlockParam.ofToolResult(toolResultBlock)))
                    .build();

            var extended = new ArrayList<MessageParam>(messages);
            extended.add(assistantMessage);
            extended.add(userMessage);

            return issueRequest(List.copyOf(extended), nextTool, systemBlock);
        }
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private static Base64ImageSource.MediaType resolveMediaType(String mediaType) {
        return switch (mediaType.toLowerCase()) {
            case "image/png" -> Base64ImageSource.MediaType.IMAGE_PNG;
            case "image/gif" -> Base64ImageSource.MediaType.IMAGE_GIF;
            case "image/webp" -> Base64ImageSource.MediaType.IMAGE_WEBP;
            default -> Base64ImageSource.MediaType.IMAGE_JPEG;
        };
    }

    private static String buildUserText(Image image) {
        var location = image.metadata().location();
        if (StringUtils.isNotBlank(location)) {
            return "Identify this insect. Location: " + location;
        }
        return "Identify this insect.";
    }

    /**
     * Converts a {@link ToolSchema} into an Anthropic SDK {@link Tool}.
     *
     * <p>The {@code parametersJson} is a JSON object string such as:
     * {@code {"type":"object","properties":{...},"required":[...]}}.
     * We parse it with Jackson, then build the {@code InputSchema} by populating
     * its {@code properties} with individual {@link JsonValue} entries and its
     * {@code required} list, matching the Anthropic SDK's builder contract.
     */
    @SuppressWarnings("unchecked")
    static Tool buildTool(ToolSchema schema) {
        try {
            // Parse the full schema JSON into a Map so we can pull out properties
            // and required fields individually.
            Map<String, Object> schemaMap = MAPPER.readValue(
                    schema.parametersJson(),
                    new TypeReference<>() {});

            var inputSchemaBuilder = Tool.InputSchema.builder();

            // Populate individual property definitions
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

            // Populate required field list
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
