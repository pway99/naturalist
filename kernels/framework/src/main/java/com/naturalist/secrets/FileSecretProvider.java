package com.naturalist.secrets;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

/**
 * Reads a secret from {@code <directory>/<name>} (ADR-028), the shape of systemd
 * credentials ({@code $CREDENTIALS_DIRECTORY}), Docker and Kubernetes secret
 * mounts and Cloud Run secret volumes. Trailing carriage returns and newlines
 * are removed, nothing else; a file that is then blank is treated as absent.
 */
public final class FileSecretProvider implements SecretProvider {
    private final Path directory;

    public FileSecretProvider(Path directory) {
        this.directory = directory;
    }

    @Override
    public Optional<Secret> lookup(SecretName name) {
        Path path = directory.resolve(name.value());
        if (Files.notExists(path)) {
            return Optional.empty();
        }
        try {
            if (!Files.isRegularFile(path)) {
                throw new IOException("not a regular file");
            }
            String value = stripTrailingLineBreaks(Files.readString(path, StandardCharsets.UTF_8));
            return value.isBlank() ? Optional.empty() : Optional.of(Secret.of(value));
        } catch (IOException e) {
            throw new IllegalStateException("secret file could not be read: " + path, e);
        }
    }

    @Override
    public String source() {
        return "file:" + directory;
    }

    private static String stripTrailingLineBreaks(String text) {
        int end = text.length();
        while (end > 0 && (text.charAt(end - 1) == '\n' || text.charAt(end - 1) == '\r')) {
            end--;
        }
        return text.substring(0, end);
    }
}
