package com.naturalist.secrets;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import com.naturalist.exception.InvariantViolationException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FileSecretProviderTest {

    private static final SecretName SECRET = SecretName.of("example-secret");

    @TempDir
    Path dir;

    @Test
    void present_isReadFromTheFileNamedAfterTheSecret() throws IOException {
        Files.writeString(dir.resolve("example-secret"), "not-a-real-secret");
        assertThat(new FileSecretProvider(dir).require(SECRET)).isEqualTo(Secret.of("not-a-real-secret"));
    }

    @Test
    void trailingNewline_isTrimmed() throws IOException {
        Files.writeString(dir.resolve("example-secret"), "not-a-real-secret\n");
        assertThat(new FileSecretProvider(dir).require(SECRET).fingerprint()).isEqualTo("ed24bc0b");
    }

    @Test
    void trailingCrLf_isTrimmed() throws IOException {
        Files.writeString(dir.resolve("example-secret"), "not-a-real-secret\r\n");
        assertThat(new FileSecretProvider(dir).require(SECRET).value()).isEqualTo("not-a-real-secret");
    }

    @Test
    void innerWhitespace_isKept() throws IOException {
        Files.writeString(dir.resolve("example-secret"), " a b\n");
        assertThat(new FileSecretProvider(dir).require(SECRET).value()).isEqualTo(" a b");
    }

    @Test
    void missingFile_isEmpty_andRequireNamesTheDirectory() {
        FileSecretProvider provider = new FileSecretProvider(dir);
        assertThat(provider.lookup(SECRET)).isEmpty();
        assertThatThrownBy(() -> provider.require(SECRET))
                .isInstanceOf(MissingSecretException.class)
                .hasMessageContaining("example-secret")
                .hasMessageContaining("file:" + dir);
    }

    @Test
    void emptyFile_isTreatedAsAbsent() throws IOException {
        Files.writeString(dir.resolve("example-secret"), "\n");
        assertThat(new FileSecretProvider(dir).lookup(SECRET)).isEmpty();
    }

    @Test
    void aDirectoryInPlaceOfTheFile_isAnError() throws IOException {
        Files.createDirectory(dir.resolve("example-secret"));
        assertThatThrownBy(() -> new FileSecretProvider(dir).lookup(SECRET))
                .isInstanceOf(InvariantViolationException.class)
                .hasMessageContaining("[FileSecretProvider.lookup.regularFile]");
    }

    @Test
    void source_namesTheDirectory() {
        assertThat(new FileSecretProvider(dir).source()).isEqualTo("file:" + dir);
    }

    @Test
    void nullDirectory_isRefused() {
        assertThatThrownBy(() -> new FileSecretProvider(null))
                .isInstanceOf(InvariantViolationException.class)
                .hasMessageContaining("[FileSecretProvider.constructor.directory]");
    }

    @Test
    void nullName_isRefused() {
        FileSecretProvider provider = new FileSecretProvider(dir);
        assertThatThrownBy(() -> provider.lookup(null))
                .isInstanceOf(InvariantViolationException.class)
                .hasMessageContaining("[FileSecretProvider.lookup.name]");
        assertThatThrownBy(() -> provider.require(null))
                .isInstanceOf(InvariantViolationException.class)
                .hasMessageContaining("[FileSecretProvider.require.name]");
    }
}
