package com.naturalist.library;

import com.naturalist.catalog.DomainId;
import com.naturalist.catalog.EntityRef;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Mock-stack {@link EntityRefResolver} that dispatches to a per-domain delegate on the subject's
 * domain slug. The in-memory counterpart of {@code CompositeEntityRefResolver} in the app, which
 * does the same over Spring-discovered beans.
 *
 * <h2>Keyed, not walked</h2>
 * Dispatch is a map lookup on {@link EntityRefResolver#domain()}, not a walk that tries each
 * delegate until one stops throwing. That matters for two reasons. Delegates reject an unowned ref
 * with {@link IllegalArgumentException}, so a walk would be using exceptions as control flow. And a
 * walk's answer depends on delegate order, which is exactly the latent hazard
 * {@code CompositeEntityRefLinker}'s javadoc flags about its own first-non-null strategy — a
 * linker can afford it because {@code linkFor} returns null for "not mine"; this port has no such
 * return.
 *
 * <p>Two delegates claiming one slug is rejected at construction rather than silently resolved by
 * order. That mirrors {@code CatalogAssembly}, which fails fast when two {@code DomainId}s share a
 * {@link DomainId#value()}.
 *
 * <p>{@link #domain()} is unanswerable for a composite — it speaks for several domains — so it
 * throws. Nothing composes a composite; if that changes, the port needs a multi-domain shape
 * rather than a lie here.
 */
final class CompositeEntityRefResolverMock implements EntityRefResolver {

    private final Map<String, EntityRefResolver> byDomainSlug;

    CompositeEntityRefResolverMock(List<EntityRefResolver> delegates) {
        if (delegates == null || delegates.isEmpty()) {
            throw new IllegalArgumentException("A composite needs at least one delegate resolver");
        }
        Map<String, EntityRefResolver> index = new LinkedHashMap<>();
        for (EntityRefResolver delegate : delegates) {
            String slug = delegate.domain().value();
            EntityRefResolver previous = index.put(slug, delegate);
            if (previous != null) {
                throw new IllegalArgumentException(
                        "Two resolvers claim the citation subject domain '" + slug + "': "
                                + previous.getClass().getName() + " and "
                                + delegate.getClass().getName());
            }
        }
        this.byDomainSlug = Map.copyOf(index);
    }

    @Override
    public DomainId domain() {
        throw new UnsupportedOperationException(
                "A composite resolver speaks for several domains: "
                        + byDomainSlug.keySet());
    }

    @Override
    public String rankOf(EntityRef subject) {
        if (subject == null) {
            throw new IllegalArgumentException("Cannot read the rank of a null subject");
        }
        return delegateFor(subject.domain().value()).rankOf(subject);
    }

    @Override
    public EntityRef resolve(String domain, String rank, String name) {
        return delegateFor(domain).resolve(domain, rank, name);
    }

    private EntityRefResolver delegateFor(String slug) {
        EntityRefResolver delegate = byDomainSlug.get(slug);
        if (delegate == null) {
            throw new IllegalArgumentException(
                    "Unknown citation subject domain: " + slug
                            + " (known: " + byDomainSlug.keySet() + ")");
        }
        return delegate;
    }
}
