package com.naturalist.test;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Stream;

/**
 * Temporary directories for tests, deleted when the JVM exits. Each one is a fresh directory, so a test still
 * writes in isolation, but all of them sit under one root per JVM, and a shutdown hook removes that root and
 * everything in it. {@link Files#createTempDirectory(String, java.nio.file.attribute.FileAttribute[])} alone
 * leaves every directory behind in the system temp directory.
 */
public final class TestDirectories {
    private static final Path ROOT = createRoot();

    private TestDirectories() {
    }

    /** A new, empty directory whose name starts with {@code prefix}, deleted with the rest when the JVM exits. */
    public static Path create(String prefix) throws IOException {
        return Files.createTempDirectory(ROOT, prefix);
    }

    private static Path createRoot() {
        try {
            Path root = Files.createTempDirectory("naturalist-tests-");
            Runtime.getRuntime().addShutdownHook(new Thread(() -> deleteTree(root), "naturalist-test-directories"));
            return root;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** Deletes children before parents. A file that cannot be deleted at exit stays; no later run reads it. */
    private static void deleteTree(Path root) {
        try (Stream<Path> walk = Files.walk(root)) {
            walk.sorted(Comparator.reverseOrder()).forEach(TestDirectories::deleteQuietly);
        } catch (IOException e) {
            // the JVM is exiting: there is no one left to report to
        }
    }

    private static void deleteQuietly(Path path) {
        try {
            Files.deleteIfExists(path);
        } catch (IOException e) {
            // see deleteTree: left behind, harmlessly
        }
    }
}
