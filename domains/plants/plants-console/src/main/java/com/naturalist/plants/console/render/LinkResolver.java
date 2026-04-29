package com.naturalist.plants.console.render;

import com.naturalist.atlas.EntityRef;
import com.naturalist.ddd.EntityName;
import com.naturalist.observability.Observer;

import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

/**
 * Translates an {@link EntityRef} into the canonical console URL for the
 * referenced entity, using a small {@code Map<Class<? extends EntityName>,
 * Function<EntityName, String>>} populated at composition time.
 *
 * <p>Each domain's console module (or the app's composition root) is
 * responsible for contributing the URL builder for the {@link EntityName}
 * subclasses it owns — the resolver itself contains no domain knowledge and
 * imports nothing from the domain modules whose URLs it produces. The atlas
 * resolves a surface form to an {@link EntityRef}; the resolver is the
 * pluggable shim that turns that ref into the route the renderer wraps in an
 * anchor.
 *
 * <p>An unmapped {@link EntityName} subclass yields {@link Optional#empty()}.
 * The renderer treats that as "italicise without linking" — graceful
 * degradation when a domain's URL builder has not yet been wired (e.g. in
 * {@code plants-console} unit tests or before the insects console grows
 * routes for {@code InsectSpeciesName}).
 *
 * <p>Per the M7 milestone in {@code kernels/atlas/PLAN.md}, the binomial
 * italics pass becomes an italics-and-resolve pass: the {@code <em>} tag is
 * always emitted, and an {@code <a>} wrapper is added only when the surface
 * form resolves through the atlas <em>and</em> this resolver knows how to
 * route the resulting {@link EntityRef}.
 *
 * <h2>Thread-safety</h2>
 * Immutable post-construction; the underlying map is defensively copied.
 * A single instance is safe for concurrent use.
 */
public final class LinkResolver {

    private final Map<Class<? extends EntityName>, Function<EntityName, String>> urlBuilders;

    public LinkResolver(Map<Class<? extends EntityName>, Function<EntityName, String>> urlBuilders) {
        Observer.forClass(LinkResolver.class)
                .arguments("constructor", i -> i.notNull(urlBuilders, "urlBuilders"))
                .throwWhenInvalid();
        this.urlBuilders = Map.copyOf(urlBuilders);
    }

    /**
     * A resolver that knows no URL shapes — every call returns
     * {@link Optional#empty()}. Useful as the default collaborator in tests
     * and as a lightweight stub when atlas linking is not yet desired on a
     * given page.
     */
    public static LinkResolver empty() {
        return new LinkResolver(Map.of());
    }

    /**
     * Look up the canonical URL for the entity pointed at by {@code ref}.
     * Returns {@link Optional#empty()} when {@code ref} is null, when its
     * {@link EntityRef#name()} is null, or when no URL builder has been
     * registered for the runtime class of the name.
     */
    public Optional<String> urlFor(EntityRef ref) {
        if (ref == null || ref.name() == null) {
            return Optional.empty();
        }
        Function<EntityName, String> builder = urlBuilders.get(ref.name().getClass());
        if (builder == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(builder.apply(ref.name()));
    }
}
