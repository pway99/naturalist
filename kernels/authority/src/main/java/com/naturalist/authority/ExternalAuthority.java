package com.naturalist.authority;

import com.naturalist.ddd.EntityName;

import java.util.Set;

/**
 * A single external authority a consumer can consult for deep-links to
 * its catalogue. Consumers choose <em>which</em> authority to call;
 * there is no fan-out across providers.
 *
 * <p>Network-backed implementations MUST be Resilience-wrapped
 * (bulkhead + timeout + retry) per ADR-026. An in-memory implementation
 * carries {@code @ResilienceExempt} instead, because it performs no I/O.
 */
public interface ExternalAuthority {

    /** The authority this client consults (e.g. EOL). */
    AuthoritySource source();

    /**
     * Deep-link references this source knows for {@code subject}.
     * Never null; an empty set means "nothing known", not an error.
     */
    Set<AuthorityReference> lookup(EntityName subject);
}