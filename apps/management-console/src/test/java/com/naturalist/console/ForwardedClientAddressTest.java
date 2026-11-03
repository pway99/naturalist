package com.naturalist.console;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The client-identity configuration (ADR-027 D4) from a <em>trusted</em> peer: the test's own
 * loopback connection stands in for the local Cloudflare Tunnel connector, which Boot's default
 * {@code internal-proxies} trusts. {@code CF-Connecting-IP} becomes {@code getRemoteAddr()} and
 * {@code X-Forwarded-Proto: https} makes the request secure. {@link UntrustedPeerForwardedHeadersTest}
 * is the other half of the boundary.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(RemoteAddressProbe.class)
class ForwardedClientAddressTest {

    @LocalServerPort
    int port;

    @Test
    void fromTheConnector_cfConnectingIpIsTheClientAddress_andForwardedHttpsIsSecure() throws Exception {
        String seen = RemoteAddressProbe.probe(port,
                "CF-Connecting-IP", "203.0.113.9",
                "X-Forwarded-Proto", "https");

        assertThat(seen).isEqualTo("203.0.113.9 true");
    }

    @Test
    void withNoForwardedHeaders_theTcpPeerIsTheClientAddress() throws Exception {
        assertThat(RemoteAddressProbe.probe(port)).isEqualTo("127.0.0.1 false");
    }

    @Test
    void xForwardedFor_isNotTheClientAddressHeader() throws Exception {
        // Only CF-Connecting-IP is read. X-Forwarded-For is client-appendable through any proxy
        // chain, so even from a trusted peer it never chooses the rate-limit key.
        assertThat(RemoteAddressProbe.probe(port, "X-Forwarded-For", "198.51.100.4"))
                .isEqualTo("127.0.0.1 false");
    }
}
