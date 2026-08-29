package com.naturalist.library;

import com.naturalist.catalog.EntityRef;

/**
 * Composition-root seam for translating a {@link CitationAssociation}'s cross-domain
 * {@link EntityRef} subject to and from its flat stored form.
 *
 * <p>A {@code CitationAssociation.subject} pairs a {@link com.naturalist.catalog.DomainId} with a
 * <em>typed</em> {@link com.naturalist.ddd.EntityName} (an {@code InsectOrderName}, an
 * {@code InsectFamilyName}, …). The persistence adapter stores that pair as three slug columns
 * ({@code subject_domain} / {@code subject_rank} / {@code subject_name}) and must reconstruct the
 * typed value on read — but reconstructing (and even reading the rank discriminator off) a typed
 * name needs the owning domain's factory, which the {@code library-repository-rdbms} module may not
 * import (the DAG confines it to library's own api + persistence + framework). This port injects that
 * knowledge from the one place allowed to hold it: the composition root, which depends on every
 * subject domain.
 *
 * <p>Both directions are declared because both need domain knowledge: the rank discriminator is
 * encoded in the concrete {@code EntityName} subtype, so it cannot be read generically on the write
 * path any more than the subtype can be rebuilt generically on the read path. The domain slug and the
 * name slug are read straight off the {@link EntityRef} by the adapter; only the rank passes through
 * {@link #rankOf}.
 *
 * <p>Implementations live in a subject domain's {@code *-core} (e.g. an insects resolver) or in the
 * app, and are registered as a {@code @DomainService} bean. The in-memory mock resolves the same pair
 * directly (it may import the subject domains); this port is the production equivalent of that logic.
 */
public interface EntityRefResolver {

    /**
     * The stored rank discriminator (e.g. {@code "ORDER"}, {@code "FAMILY"}) for a subject ref,
     * read from the concrete {@link com.naturalist.ddd.EntityName} subtype of {@code subject.name()}.
     *
     * @throws IllegalArgumentException if no registered domain owns the subject's name type
     */
    String rankOf(EntityRef subject);

    /**
     * Reconstruct the typed subject {@link EntityRef} from its stored slugs.
     *
     * @throws IllegalArgumentException if {@code domain}/{@code rank} name no registered domain/rank
     */
    EntityRef resolve(String domain, String rank, String name);
}
