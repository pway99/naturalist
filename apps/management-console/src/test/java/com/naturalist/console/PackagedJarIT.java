package com.naturalist.console;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Runs the artifact a deployment runs: {@code java -jar}, from a directory outside the source tree.
 * Every other test here runs on a plain classpath beside the sources, so none of them can see a jar
 * that starts cleanly and then fails to render. Only reads the standing test database.
 */
class PackagedJarIT {

    private static final Duration STARTUP_LIMIT = Duration.ofSeconds(90);
    private static final HttpClient HTTP = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NEVER)
            .connectTimeout(Duration.ofSeconds(2))
            .build();

    @TempDir
    static Path workingDirectory;

    private static Process app;
    private static Path output;
    private static int port;

    @BeforeAll
    static void startTheJar() throws Exception {
        try (ServerSocket socket = new ServerSocket(0); ServerSocket management = new ServerSocket(0)) {
            port = socket.getLocalPort();
            managementPort = management.getLocalPort();
        }
        output = workingDirectory.resolve("app.log");
        String java = ProcessHandle.current().info().command().orElseThrow();
        app = new ProcessBuilder(List.of(java, "-jar", System.getProperty("packagedJar"),
                "--server.port=" + port,
                "--management.server.port=" + managementPort,
                "--naturalist.admin.username=test-admin",
                "--naturalist.admin.password=not-a-real-secret",
                "--naturalist.vision.provider=none",
                "--naturalist.email.log-only=true",
                "--naturalist.challenge.log-only=true"))
                .directory(workingDirectory.toFile())
                .redirectErrorStream(true)
                .redirectOutput(output.toFile())
                .start();
        awaitListening();
    }

    @AfterAll
    static void stopTheJar() throws Exception {
        if (app != null) {
            app.destroy();
            if (!app.waitFor(10, TimeUnit.SECONDS)) {
                app.destroyForcibly();
            }
        }
    }

    @Test
    void anAppTemplate_renders() throws Exception {
        HttpResponse<String> login = get("/login");

        assertThat(login.statusCode()).as(log()).isEqualTo(200);
        assertThat(login.body()).contains("name=\"username\"");
    }

    @Test
    void aDomainConsoleTemplate_renders() throws Exception {
        HttpResponse<String> orders = get("/insects/orders");

        assertThat(orders.statusCode()).as(log()).isEqualTo(200);
        assertThat(orders.body()).contains("Insect");
    }

    /** A console module missing from the POM's gather-templates would otherwise fail only when a page renders. */
    @Test
    void everyTemplateInTheSourceTree_isInTheJar() throws Exception {
        Path jar = Path.of(System.getProperty("packagedJar"));
        Path projectRoot = jar.resolve("../../../..").normalize();

        long inSource;
        try (Stream<Path> files = Files.walk(projectRoot)) {
            inSource = files.filter(file -> file.toString().endsWith(".jte"))
                    .filter(file -> file.toString().contains("/src/main/jte/"))
                    .filter(file -> !file.toString().contains("/target/"))
                    .count();
        }
        long inJar;
        try (ZipFile zip = new ZipFile(jar.toFile())) {
            inJar = zip.stream().map(ZipEntry::getName)
                    .filter(name -> name.startsWith("BOOT-INF/classes/gg/jte/generated/precompiled/"))
                    .filter(name -> name.endsWith("Generated.class"))
                    .count();
        }

        assertThat(inSource).isPositive();
        assertThat(inJar).isEqualTo(inSource);
    }

    private static void awaitListening() throws Exception {
        long deadline = System.nanoTime() + STARTUP_LIMIT.toNanos();
        while (System.nanoTime() < deadline) {
            assertThat(app.isAlive()).as(log()).isTrue();
            try {
                get("/login");
                return;
            } catch (IOException notListeningYet) {
                Thread.sleep(250);
            }
        }
        throw new AssertionError("The jar was not listening after " + STARTUP_LIMIT + ": " + log());
    }

    private static HttpResponse<String> get(String path) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .timeout(Duration.ofSeconds(30))
                .GET()
                .build();
        return HTTP.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private static String log() {
        try {
            List<String> lines = Files.readAllLines(output);
            return String.join("\n", lines.subList(Math.max(0, lines.size() - 40), lines.size()));
        } catch (IOException e) {
            return "no app log: " + e.getMessage();
        }
    }

    private static int managementPort;

    private static HttpResponse<String> getFromPort(int fromPort, String path) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + fromPort + path))
                .timeout(Duration.ofSeconds(30))
                .GET()
                .build();
        return HTTP.send(request, HttpResponse.BodyHandlers.ofString());
    }

    /**
     * Log-only mode reports a turnstile violation at startup, so this counter has a value with no traffic.
     * It only reaches the scrape if Boot's registry joined the global one the kernel emits through.
     */
    @Test
    void theManagementPort_servesTheInvariantCounter() throws Exception {
        HttpResponse<String> scrape = getFromPort(managementPort, "/actuator/prometheus");

        assertThat(scrape.statusCode()).as(log()).isEqualTo(200);
        assertThat(scrape.body())
                .contains("naturalist_invariant_check_total{")
                .contains("name=\"ChallengeConfiguration.challengeVerifier.turnstile\"")
                .contains("outcome=\"violated\"");
    }

    @Test
    void thePublicPort_servesNoMetrics() throws Exception {
        HttpResponse<String> scrape = get("/actuator/prometheus");

        assertThat(scrape.statusCode()).as(log()).isNotEqualTo(200);
        assertThat(scrape.body()).doesNotContain("naturalist_invariant_check");
    }

    /**
     * framework-test ships for TestEntitySource (see the POM), but the test graph's subclass proxies never run in
     * the app, so the library that generates them stays out of the deployable.
     */
    @Test
    void theTestGraphLibrary_isNotInTheJar() throws Exception {
        try (ZipFile zip = new ZipFile(System.getProperty("packagedJar"))) {
            assertThat(zip.stream().map(ZipEntry::getName).filter(name -> name.startsWith("BOOT-INF/lib/byte-buddy")))
                    .isEmpty();
        }
    }
}
