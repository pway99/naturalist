package com.naturalist.console;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A page that throws for a signed-out visitor reaches them as an error. The container dispatches
 * the failure to {@code /error}, which only a real port does: behind a login, that dispatch redirects
 * to {@code /login}, and a broken login page then redirects to itself for ever.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ErrorPageForAnonymousVisitorsTest {

    private static final String FAILURE_DETAIL = "detail-a-visitor-must-not-see";

    @TestConfiguration
    static class FailingPublicPage {
        @Bean
        FailingController failingController() {
            return new FailingController();
        }
    }

    @RestController
    static class FailingController {
        @GetMapping("/insects/test-only-failure")
        String fail() {
            throw new IllegalStateException(FAILURE_DETAIL);
        }
    }

    @LocalServerPort
    int port;

    @Test
    void aFailingPublicPage_answers500_withoutTheFailureDetail() throws Exception {
        HttpClient client = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NEVER).build();

        HttpResponse<String> response = client.send(
                HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/insects/test-only-failure")).GET().build(),
                HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(500);
        assertThat(response.body()).doesNotContain(FAILURE_DETAIL);
    }
}
