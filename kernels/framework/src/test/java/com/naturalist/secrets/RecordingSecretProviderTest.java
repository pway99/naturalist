package com.naturalist.secrets;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RecordingSecretProviderTest {

    private static final SecretName SECRET = SecretName.of("example-secret");

    private final Clock clock = Clock.fixed(Instant.parse("2026-11-03T10:00:00Z"), ZoneOffset.UTC);

    @Test
    void successfulLookup_isRecordedWithFingerprintAndSource_neverTheValue() {
        var recording = new RecordingSecretProvider(
                new EnvironmentSecretProvider(Map.of("EXAMPLE_SECRET", "not-a-real-secret")), clock);
        recording.require(SECRET);
        assertThat(recording.resolutions()).containsExactly(
                new SecretResolution(SECRET, "environment", "ed24bc0b", Instant.parse("2026-11-03T10:00:00Z")));
        assertThat(recording.resolutions().toString()).doesNotContain("not-a-real");
    }

    @Test
    void absentLookup_recordsNothing_andStillThrowsOnRequire() {
        var recording = new RecordingSecretProvider(new EnvironmentSecretProvider(Map.of()), clock);
        assertThat(recording.lookup(SECRET)).isEmpty();
        assertThatThrownBy(() -> recording.require(SECRET)).isInstanceOf(MissingSecretException.class);
        assertThat(recording.resolutions()).isEmpty();
    }

    @Test
    void repeatedLookups_recordOnceAndKeepTheFirstTimestamp() {
        var recording = new RecordingSecretProvider(
                new EnvironmentSecretProvider(Map.of("EXAMPLE_SECRET", "not-a-real-secret")), clock);
        recording.require(SECRET);
        recording.require(SECRET);
        assertThat(recording.resolutions()).hasSize(1);
    }

    @Test
    void resolutions_areSortedByName_andUnmodifiable() {
        var recording = new RecordingSecretProvider(new EnvironmentSecretProvider(Map.of(
                "OTHER_SECRET", "t", "EXAMPLE_SECRET", "a")), clock);
        recording.require(SecretName.of("other-secret"));
        recording.require(SECRET);
        assertThat(recording.resolutions()).extracting(r -> r.name().value())
                .containsExactly("example-secret", "other-secret");
        assertThatThrownBy(() -> recording.resolutions().clear()).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void source_delegates() {
        assertThat(new RecordingSecretProvider(new EnvironmentSecretProvider(Map.of()), clock).source())
                .isEqualTo("environment");
    }
}
