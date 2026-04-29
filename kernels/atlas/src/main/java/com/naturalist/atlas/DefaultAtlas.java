package com.naturalist.atlas;

import com.naturalist.observability.Observer;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * In-memory {@link Atlas} assembled from a list of {@link AtlasContribution}s.
 * The default implementation used by every app in the codebase; bespoke
 * deployments may substitute their own {@link Atlas}.
 * <p>
 * Surface forms are eagerly indexed at construction; resolution is a single
 * hash lookup. The map is built case-sensitively — see
 * {@link Atlas#resolveAlias(String)} for the matching contract.
 *
 * <h2>Assembly conflicts</h2>
 * If two contributions register the same surface form pointing at <em>different</em>
 * targets, assembly fails fast with {@link IllegalStateException}. The plan's
 * deterministic resolution rule is "longest match wins; equal-length match is
 * an assembly error" — and identical surface forms are equal-length by
 * construction, so any direct collision is rejected. This is louder than the
 * runtime soft-validation philosophy because misregistered aliases would
 * silently route the renderer at the wrong page; better to refuse to boot.
 * <p>
 * The same surface form pointing at the same target from two contributions is
 * silently deduplicated. This keeps the contract forgiving when, for example,
 * a future shared common-name catalogue overlaps with a domain's own
 * contribution.
 *
 * <h2>Argument validation</h2>
 * Null checks at the assembly boundary go through the kernel's
 * {@link Observer#arguments(String, java.util.function.Consumer)} pipeline so
 * a misuse is reported through the same channel as every other invariant
 * violation in the codebase, with the standard {@code DefaultAtlas.constructor.<arg>}
 * scope on the resulting metric.
 *
 * <h2>Visibility</h2>
 * The constructor is package-private. Consumers go through
 * {@link AtlasAssembly} so the kernel retains the option to swap in a smarter
 * assembly (a builder, a parallel pre-pass, etc.) without breaking call
 * sites.
 */
final class DefaultAtlas implements Atlas {

    private static final Observer observer = Observer.forClass(DefaultAtlas.class);

    private final Map<String, EntityRef> bySurfaceForm;

    DefaultAtlas(List<AtlasContribution> contributions) {
        observer.arguments("constructor", i -> i
                        .notNull(contributions, "contributions"))
                .throwWhenInvalid();
        Map<String, EntityRef> map = new HashMap<>();
        for (AtlasContribution contribution : contributions) {
            observer.arguments("constructor", i -> i
                            .notNull(contribution, "contribution"))
                    .throwWhenInvalid();
            contribution.aliases().forEach(alias -> {
                observer.arguments("constructor", i -> i
                                .notNull(alias, "alias"))
                        .throwWhenInvalid();
                String surfaceForm = alias.surfaceForm();
                EntityRef target = alias.target();
                EntityRef previous = map.putIfAbsent(surfaceForm, target);
                if (previous != null && !previous.equals(target)) {
                    throw new IllegalStateException(
                            "Atlas assembly conflict: surface form '%s' is registered for both %s and %s"
                                    .formatted(surfaceForm, previous, target));
                }
            });
        }
        this.bySurfaceForm = Map.copyOf(map);
    }

    @Override
    public Optional<EntityRef> resolveAlias(String text) {
        if (text == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(bySurfaceForm.get(text));
    }
}
