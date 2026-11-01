package com.naturalist.console.catalog;

import com.naturalist.catalog.DomainId;
import com.naturalist.catalog.EntityRef;
import com.naturalist.library.EntityRefResolver;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * App-shell composite that routes a {@code CitationAssociation} subject ref to the
 * per-domain {@link EntityRefResolver} that owns it, keyed on the domain slug. Sibling of
 * {@link CompositeEntityRefLinker}; the mock stack's equivalent is
 * {@code CompositeEntityRefResolverMock} in {@code library-repository-test}.
 *
 * <h2>Keyed, not walked — unlike the linker beside it</h2>
 * {@link CompositeEntityRefLinker} walks its delegates and takes the first non-null, which is
 * sound because {@code linkFor} returns null for "not mine". This port has no such return: a
 * delegate rejects an unowned ref by throwing. Walking would therefore mean catching
 * {@link IllegalArgumentException} to drive control flow, and would make the answer depend on
 * component-scan order — the hazard the linker's own javadoc calls out. {@link
 * EntityRefResolver#domain()} exists so this can be a map lookup instead, and so two delegates
 * claiming one slug are caught here rather than silently resolved by scan order.
 *
 * <h2>Why an empty delegate list is accepted</h2>
 * Under the {@code mock-data} profile the {@code @DomainService} scan is excluded, so no
 * per-domain resolver bean is registered and Spring injects an empty list. Throwing at
 * construction would fail context startup for every console {@code @SpringBootTest} — the
 * failure mode a factory-backed {@code @DomainService} has caused twice before. An empty
 * composite therefore constructs fine and fails only if something actually asks it to resolve,
 * which is the honest shape: the absence is a wiring fact, not a usage error, until used.
 */
@Component
@Primary
public class CompositeEntityRefResolver implements EntityRefResolver {

    private final Map<String, EntityRefResolver> byDomainSlug;

    CompositeEntityRefResolver(List<EntityRefResolver> delegates) {
        Map<String, EntityRefResolver> index = new LinkedHashMap<>();
        for (EntityRefResolver delegate : delegates) {
            if (delegate == this) {
                continue;
            }
            String slug = delegate.domain().value();
            EntityRefResolver previous = index.put(slug, delegate);
            if (previous != null) {
                throw new IllegalStateException(
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
                "A composite resolver speaks for several domains: " + byDomainSlug.keySet());
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
                    "No citation subject resolver is registered for domain: " + slug
                            + " (registered: " + byDomainSlug.keySet() + ")");
        }
        return delegate;
    }
}
