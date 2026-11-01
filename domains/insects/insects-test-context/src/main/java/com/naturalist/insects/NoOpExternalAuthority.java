package com.naturalist.insects;

import com.naturalist.authority.AuthorityContent;
import com.naturalist.authority.AuthorityReference;
import com.naturalist.authority.AuthoritySource;
import com.naturalist.authority.ExternalAuthority;
import com.naturalist.ddd.EntityName;
import com.naturalist.resilience.ResilienceExempt;

import java.util.Set;

/**
 * The test contexts' default {@link com.naturalist.authority.ExternalAuthority}: it confirms
 * nothing (every lookup is empty) and so is never asked to fetch content. Identification
 * runs against it with vision as the sole source of truth.
 */
@ResilienceExempt(reason = "in-memory test double; performs no I/O")
final class NoOpExternalAuthority implements ExternalAuthority {
    private static final AuthoritySource SOURCE = new com.naturalist.authority.AuthoritySource("test", "Test Authority");

    @Override
    public AuthoritySource source() {
        return SOURCE;
    }

    @Override
    public Set<AuthorityReference> lookup(EntityName subject) {
        return Set.of();
    }

    @Override
    public AuthorityContent fetchContent(AuthorityReference ref) {
        throw new UnsupportedOperationException(
                "NoOpExternalAuthority confirms nothing, so no reference of its own reaches fetchContent: " + ref);
    }
}
