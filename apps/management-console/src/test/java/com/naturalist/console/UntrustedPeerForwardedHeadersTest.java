package com.naturalist.console;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The trust boundary of ADR-027 D4: when the TCP peer is <em>not</em> an internal proxy, its
 * forwarded headers are ignored and it keeps its own address and scheme. Here
 * {@code internal-proxies} is narrowed to a documentation address the test never connects from,
 * so the loopback test client plays a stranger connecting straight to the origin — and cannot
 * choose its own rate-limit key by sending {@code CF-Connecting-IP}.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "server.tomcat.remoteip.internal-proxies=192\\.0\\.2\\.1")
@Import(RemoteAddressProbe.class)
class UntrustedPeerForwardedHeadersTest {

    @LocalServerPort
    int port;

    @Test
    void fromAnUntrustedPeer_forwardedHeadersAreIgnored() throws Exception {
        String seen = RemoteAddressProbe.probe(port,
                "CF-Connecting-IP", "203.0.113.9",
                "X-Forwarded-Proto", "https");

        assertThat(seen).isEqualTo("127.0.0.1 false");
    }
}
