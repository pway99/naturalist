package com.naturalist.console;

import jakarta.servlet.Filter;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

/**
 * A test-only servlet filter, ordered ahead of Spring Security, that answers
 * {@value #PATH} with what the container decided about the request:
 * {@code <remoteAddr> <isSecure>}. Tomcat's {@code RemoteIpValve} runs before every filter, so
 * this is the address and scheme {@code ClientKey} and the session cookie would see.
 */
@TestConfiguration
class RemoteAddressProbe {

    static final String PATH = "/__probe/remote-address";

    @Bean
    FilterRegistrationBean<Filter> remoteAddressProbeFilter() {
        Filter probe = (request, response, chain) -> {
            if (request instanceof jakarta.servlet.http.HttpServletRequest http
                    && PATH.equals(http.getRequestURI())) {
                response.setContentType("text/plain;charset=UTF-8");
                response.getWriter().write(request.getRemoteAddr() + " " + request.isSecure());
                return;
            }
            chain.doFilter(request, response);
        };
        FilterRegistrationBean<Filter> registration = new FilterRegistrationBean<>(probe);
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        registration.addUrlPatterns(PATH);
        return registration;
    }

    /** GETs the probe over a real loopback socket on {@code port}, with the given header pairs. */
    static String probe(int port, String... headerPairs) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + PATH));
        for (int i = 0; i < headerPairs.length; i += 2) {
            request.header(headerPairs[i], headerPairs[i + 1]);
        }
        try (HttpClient client = HttpClient.newHttpClient()) {
            return client.send(request.build(), HttpResponse.BodyHandlers.ofString()).body();
        }
    }
}
