package com.naturalist.console.secrets;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SecretsConfigurationTest {

    private final SecretsConfiguration configuration = new SecretsConfiguration();

    @Test
    void defaultSource_isTheProcessEnvironment() {
        assertThat(configuration.secretProvider(new SecretsProperties(null, null)).source()).isEqualTo("environment");
    }

    @Test
    void source_isTrimmedAndCaseInsensitive() {
        assertThat(new SecretsProperties(" Environment ", null).source()).isEqualTo("environment");
    }

    @Test
    void fileSource_readsTheConfiguredDirectory(@TempDir Path dir) {
        assertThat(configuration.secretProvider(new SecretsProperties("file", dir.toString())).source())
                .isEqualTo("file:" + dir);
    }

    @Test
    void fileSource_withoutADirectory_refusesToBoot() {
        assertThatThrownBy(() -> configuration.secretProvider(new SecretsProperties("file", " ")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("naturalist.secrets.directory");
    }

    @Test
    void unknownSource_refusesToBoot_namingTheAllowedValues() {
        assertThatThrownBy(() -> configuration.secretProvider(new SecretsProperties("vault", null)))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("naturalist.secrets.source")
                .hasMessageContaining("vault")
                .hasMessageContaining("environment")
                .hasMessageContaining("file");
    }
}
