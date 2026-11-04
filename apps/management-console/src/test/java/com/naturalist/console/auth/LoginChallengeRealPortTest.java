package com.naturalist.console.auth;

import com.naturalist.challenge.ChallengeSubmission;
import com.naturalist.challenge.ChallengeVerdict;
import com.naturalist.challenge.ChallengeVerifier;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import java.net.CookieManager;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The login filters on a real Tomcat. MockMvc leaves the servlet path empty where a container sets
 * it, so a filter that matches differently from Spring's login filter can pass one and fail the
 * other; a filter that silently stopped matching here would leave login unchallenged.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class LoginChallengeRealPortTest {

    private static final Pattern CSRF_TOKEN = Pattern.compile("name=\"_csrf\" value=\"([^\"]+)\"");

    @TestConfiguration
    static class RefusingChallenge {
        @Bean
        @Primary
        ChallengeVerifier refusingLoginVerifier() {
            return (ChallengeSubmission submission) -> ChallengeVerdict.failed("invalid-input-response");
        }
    }

    @LocalServerPort
    int port;

    @Test
    void aFailedChallenge_redirectsBeforeThePasswordCheck() throws Exception {
        HttpClient client = HttpClient.newBuilder()
                .cookieHandler(new CookieManager())
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
        URI login = URI.create("http://localhost:" + port + "/login");

        String page = client.send(HttpRequest.newBuilder(login).GET().build(),
                HttpResponse.BodyHandlers.ofString()).body();
        Matcher csrf = CSRF_TOKEN.matcher(page);
        assertThat(csrf.find()).as("the login form carries a CSRF token").isTrue();

        String form = "username=test-admin&password=test-password&cf-turnstile-response=a-token&_csrf="
                + URLEncoder.encode(csrf.group(1), StandardCharsets.UTF_8);
        HttpResponse<String> response = client.send(
                HttpRequest.newBuilder(login)
                        .header("Content-Type", "application/x-www-form-urlencoded")
                        .POST(HttpRequest.BodyPublishers.ofString(form))
                        .build(),
                HttpResponse.BodyHandlers.ofString());

        assertThat(response.statusCode()).isEqualTo(303);
        assertThat(response.headers().firstValue("Location")).hasValue("/login?challenge");
    }
}
