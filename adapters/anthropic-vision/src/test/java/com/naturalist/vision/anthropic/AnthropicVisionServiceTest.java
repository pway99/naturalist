package com.naturalist.vision.anthropic;

import java.util.List;
import java.util.Map;

import com.naturalist.resilience.Resilience;
import com.naturalist.secrets.EnvironmentSecretProvider;
import com.naturalist.secrets.MissingSecretException;
import com.naturalist.secrets.SecretName;
import com.naturalist.secrets.SecretProvider;
import com.naturalist.vision.ToolSchema;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AnthropicVisionServiceTest {

    private static SecretProvider withCredential() {
        return new EnvironmentSecretProvider(Map.of(
                AnthropicVisionService.CREDENTIAL.environmentVariable(), "not-a-real-secret"));
    }

    @Test
    void constructs_withAResolvableCredential_andNoNetwork() {
        var service = new AnthropicVisionService(AnthropicVisionConfig.defaults(), withCredential(), Resilience.noOp());
        assertThat(service).isNotNull();
    }

    @Test
    void missingCredential_failsConstruction_namingTheSecretAndTheSource() {
        assertThatThrownBy(() -> new AnthropicVisionService(
                AnthropicVisionConfig.defaults(), new EnvironmentSecretProvider(Map.of()), Resilience.noOp()))
                .isInstanceOf(MissingSecretException.class)
                .hasMessageContaining(AnthropicVisionService.CREDENTIAL.value())
                .hasMessageContaining("environment");
    }

    @Test
    void credentialName_isTheSharedContractName() {
        assertThat(AnthropicVisionService.CREDENTIAL).isEqualTo(SecretName.of("model-credential"));
    }

    @Test
    void buildTool_mapsNameDescriptionPropertiesAndRequired() {
        var tool = AnthropicVisionService.buildTool(new ToolSchema("propose", "Propose.",
                """
                {"type":"object","required":["name"],"properties":{"name":{"type":"string"}}}
                """));
        assertThat(tool.name()).isEqualTo("propose");
        assertThat(tool.description()).contains("Propose.");
        assertThat(tool.inputSchema().required()).contains(List.of("name"));
        assertThat(tool.inputSchema().properties()).isPresent();
    }
}
