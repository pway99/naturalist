package com.naturalist.console;

import java.net.http.HttpResponse;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpClient;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

/**
 * The failure this guards against: the two ports set equal, which puts the actuator on the public
 * port. This test's configuration has no separate management port, so that is the state it runs in.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = "management.endpoints.web.exposure.include=health,prometheus")
class ActuatorOnPublicPortTest {
    @ParameterizedTest
    @ValueSource(strings = {"/actuator/prometheus", "/actuator/health"})
    void onTheSamePort_theActuatorIsRefused(String path, @LocalServerPort int port) throws Exception {
        HttpResponse<String> response;
        try (HttpClient client = HttpClient.newHttpClient()) {
            response = client.send(
                    HttpRequest.newBuilder(URI.create("http://localhost:" + port + path)).build(),
                    HttpResponse.BodyHandlers.ofString());
        }

        Assertions.assertThat(response.statusCode()).isEqualTo(403);
    }
}
