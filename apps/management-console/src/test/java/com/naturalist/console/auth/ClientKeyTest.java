package com.naturalist.console.auth;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * {@link ClientKey}: the remote address as the rate-limit key, IPv6 reduced to its /64 so a
 * subscriber cannot rotate through its own prefix to reset the per-client budget.
 */
class ClientKeyTest {

    @Test
    void ipv4_isTheKeyAsGiven() {
        assertThat(ClientKey.of(from("203.0.113.9"))).isEqualTo("203.0.113.9");
    }

    @Test
    void ipv6_inOneSlash64_sharesOneKey() {
        String first = ClientKey.of(from("2001:db8:1:2:aaaa:bbbb:cccc:dddd"));
        String rotated = ClientKey.of(from("2001:db8:1:2::1"));

        assertThat(first).isEqualTo("2001:db8:1:2:0:0:0:0/64").isEqualTo(rotated);
    }

    @Test
    void ipv6_inAnotherSlash64_isAnotherKey() {
        assertThat(ClientKey.of(from("2001:db8:1:3::1")))
                .isNotEqualTo(ClientKey.of(from("2001:db8:1:2::1")));
    }

    @Test
    void ipv6_spelledExpandedOrCompressed_isOneKey() {
        assertThat(ClientKey.of(from("2001:0db8:0001:0002:0000:0000:0000:0001")))
                .isEqualTo(ClientKey.of(from("2001:db8:1:2::1")));
    }

    @Test
    void ipv4MappedIpv6_keysAsTheIpv4Address() {
        assertThat(ClientKey.of(from("::ffff:203.0.113.9"))).isEqualTo("203.0.113.9");
    }

    @Test
    void anAddressThatIsNotAnIpLiteral_isTheKeyAsGiven() {
        assertThat(ClientKey.of(from("not:an:address"))).isEqualTo("not:an:address");
    }

    @Test
    void aBlankAddress_isUnknown() {
        assertThat(ClientKey.of(from(""))).isEqualTo(ClientKey.UNKNOWN);
    }

    private static MockHttpServletRequest from(String remoteAddr) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr(remoteAddr);
        return request;
    }
}
