package com.naturalist.textgeneration.anthropic;

import java.util.Map;

import com.naturalist.secrets.EnvironmentSecretProvider;
import com.naturalist.secrets.MissingSecretException;
import com.naturalist.secrets.SecretName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AnthropicTextGenerationServiceTest {

    @Test
    void constructs_withAResolvableCredential_andNoNetwork() {
        var secrets = new EnvironmentSecretProvider(Map.of(
                AnthropicTextGenerationService.CREDENTIAL.environmentVariable(), "not-a-real-secret"));
        assertThat(new AnthropicTextGenerationService(AnthropicTextGenerationConfig.defaults(), secrets)).isNotNull();
    }

    @Test
    void missingCredential_failsConstruction_namingTheSecretAndTheSource() {
        assertThatThrownBy(() -> new AnthropicTextGenerationService(
                AnthropicTextGenerationConfig.defaults(), new EnvironmentSecretProvider(Map.of())))
                .isInstanceOf(MissingSecretException.class)
                .hasMessageContaining(AnthropicTextGenerationService.CREDENTIAL.value())
                .hasMessageContaining("environment");
    }

    @Test
    void credentialName_isTheSharedContractName() {
        assertThat(AnthropicTextGenerationService.CREDENTIAL).isEqualTo(SecretName.of("model-credential"));
    }
}
