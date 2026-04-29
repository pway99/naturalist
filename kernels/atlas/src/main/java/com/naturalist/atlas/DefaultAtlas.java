package com.naturalist.atlas;

import com.naturalist.ddd.EntityName;
import com.naturalist.observability.Observer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * In-memory {@link Atlas} assembled from a list of {@link AtlasContribution}s
 * (forward direction) and a list of {@link EntityReferences} providers
 * (inverse direction). The default implementation used by every app in the
 * codebase; bespoke deployments may substitute their own {@link Atlas}.
 * <p>
 * Surface forms are eagerly indexed at construction; resolution is a single
 * hash lookup. The map is built case-sensitively — see
 * {@link Atlas#resolveAlias(String)} for the matching contract.
 * <p>
 * Inverse providers are also indexed eagerly, keyed by their
 * {@code referenceType()}. The actual fan-out to a provider's
 * {@code referencesTo(target)} runs live on each
 * {@link Atlas#findReferencesTo(EntityName)} call — per the plan's
 * "Registry shape: routing, not graph" decision, we do not pre-materialise
 * the inbound reference set.
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
 * <p>
 * Multiple providers registered for the same {@code referenceType()} are not
 * a conflict — their results are concatenated by
 * {@link Atlas#findReferencesTo(EntityName)} in registration order. A domain
 * may register more than one provider for the same type when sub-contexts
 * each own a slice of the answer.
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
    private final Map<Class<? extends EntityName>, List<EntityReferences<?>>> providersByType;

    DefaultAtlas(List<AtlasContribution> contributions, List<EntityReferences<?>> providers) {
        observer.arguments("constructor", i -> i
                        .notNull(contributions, "contributions")
                        .notNull(providers, "providers"))
                .throwWhenInvalid();
        this.bySurfaceForm = indexAliases(contributions);
        this.providersByType = indexProviders(providers);
    }

    private static Map<String, EntityRef> indexAliases(List<AtlasContribution> contributions) {
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
        return Map.copyOf(map);
    }

    private static Map<Class<? extends EntityName>, List<EntityReferences<?>>> indexProviders(
            List<EntityReferences<?>> providers) {
        Map<Class<? extends EntityName>, List<EntityReferences<?>>> indexed = new LinkedHashMap<>();
        for (EntityReferences<?> provider : providers) {
            observer.arguments("constructor", i -> i
                            .notNull(provider, "provider"))
                    .throwWhenInvalid();
            indexed.computeIfAbsent(provider.referenceType(), k -> new ArrayList<>())
                    .add(provider);
        }
        Map<Class<? extends EntityName>, List<EntityReferences<?>>> immutable = new LinkedHashMap<>();
        indexed.forEach((k, v) -> immutable.put(k, List.copyOf(v)));
        return Collections.unmodifiableMap(immutable);
    }

    @Override
    public Optional<EntityRef> resolveAlias(String text) {
        if (text == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(bySurfaceForm.get(text));
    }

    @Override
    public Set<DomainId> domainsReferencing(Class<? extends EntityName> referenceType) {
        if (referenceType == null) {
            return Set.of();
        }
        return providersByType.getOrDefault(referenceType, List.of()).stream()
                .map(EntityReferences::domain)
                .collect(Collectors.toUnmodifiableSet());
    }

    @Override
    public Map<DomainId, List<EntityRef>> findReferencesTo(EntityName target) {
        if (target == null) {
            return Map.of();
        }
        List<EntityReferences<?>> handlers = providersByType.getOrDefault(target.getClass(), List.of());
        Map<DomainId, List<EntityRef>> grouped = new LinkedHashMap<>();
        for (EntityReferences<?> handler : handlers) {
            List<EntityRef> refs = invokeQuietly(handler, target);
            if (!refs.isEmpty()) {
                grouped.computeIfAbsent(handler.domain(), k -> new ArrayList<>()).addAll(refs);
            }
        }
        Map<DomainId, List<EntityRef>> immutable = new LinkedHashMap<>();
        grouped.forEach((k, v) -> immutable.put(k, List.copyOf(v)));
        return Collections.unmodifiableMap(immutable);
    }

    private static List<EntityRef> invokeQuietly(EntityReferences<?> handler, EntityName target) {
        try {
            return invoke(handler, target).toList();
        } catch (RuntimeException ignored) {
            // Per the plan, a throwing provider is observed in M9 and dropped
            // from the fan-out; peer providers' results survive.
            return List.of();
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Stream<EntityRef> invoke(EntityReferences<?> handler, EntityName target) {
        // Provider was indexed by referenceType; target.getClass() == referenceType
        // is how we found it, so the cast is safe at runtime. The raw invocation
        // is the standard idiom for calling a generic method whose type witness
        // is held only as a Class<T>.
        return ((EntityReferences) handler).referencesTo(handler.referenceType().cast(target));
    }
}
