package com.naturalist.console.auth;

import jakarta.servlet.http.HttpServletRequest;
import java.net.InetAddress;
import java.net.UnknownHostException;
import org.apache.commons.lang3.StringUtils;

/**
 * The key a per-client rate limit is taken under: the request's remote address, with an IPv6
 * address reduced to its /64 prefix.
 *
 * <p>Deliberately <em>only</em> {@link HttpServletRequest#getRemoteAddr()}. The app never
 * reads {@code CF-Connecting-IP}, {@code X-Forwarded-For} or {@code Forwarded} itself — a
 * header any client can send is not an identity. Behind the Cloudflare Tunnel connector the fix
 * is the container's, not this class's: {@code server.forward-headers-strategy: native} with
 * {@code server.tomcat.remoteip.remote-ip-header: CF-Connecting-IP} lets Tomcat's
 * {@code RemoteIpValve} rewrite {@code getRemoteAddr()}, and only for a peer matching
 * {@code internal-proxies} (ADR-027 D4). Never the {@code framework} strategy, which trusts
 * forwarded headers from any peer. See {@code apps/CLAUDE.md}, "Client identity behind a proxy".
 *
 * <p>IPv6 keys on the /64 because one subscriber holds a whole /64 and can rotate inside it for
 * free; IPv4 (including an IPv4-mapped IPv6 address) keys on the address. A key is opaque to the
 * kernel {@code RateLimiter.forKey}, is PII, and is never a metric tag.
 */
public final class ClientKey {

    /** What a request with no remote address (a test harness, an in-process call) limits under. */
    static final String UNKNOWN = "unknown";

    private static final int IPV6_BYTES = 16;
    private static final int IPV6_PREFIX_BYTES = 8;

    private ClientKey() {
    }

    public static String of(HttpServletRequest request) {
        String address = request.getRemoteAddr();
        return StringUtils.isBlank(address) ? UNKNOWN : keyOf(address);
    }

    /**
     * The key for one remote-address literal: an IPv6 address becomes its /64 prefix
     * ({@code 2001:db8:1:2:0:0:0:0/64}), an IPv4-mapped IPv6 address its dotted IPv4 form, and
     * IPv4 or anything that does not parse as an IP literal stays exactly as given.
     */
    static String keyOf(String address) {
        if (address.indexOf(':') < 0) {
            return address;
        }
        try {
            // ofLiteral never consults DNS: an address that is not an IP literal is refused, not resolved.
            InetAddress literal = InetAddress.ofLiteral(address);
            byte[] bytes = literal.getAddress();
            if (bytes.length != IPV6_BYTES) {
                return literal.getHostAddress();
            }
            byte[] prefix = new byte[IPV6_BYTES];
            System.arraycopy(bytes, 0, prefix, 0, IPV6_PREFIX_BYTES);
            return InetAddress.getByAddress(prefix).getHostAddress() + "/64";
        } catch (IllegalArgumentException | UnknownHostException notALiteral) {
            return address;
        }
    }
}
