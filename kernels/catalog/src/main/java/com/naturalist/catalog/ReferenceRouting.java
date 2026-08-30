package com.naturalist.catalog;

import com.naturalist.ddd.EntityName;
import com.naturalist.resilience.CircuitBreaker;
import com.naturalist.resilience.Resilience;
import com.naturalist.resilience.Timeout;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** Storage-independent inverse-direction routing shared by every {@link Catalog} adapter. */
public final class ReferenceRouting {

    public static final String CATALOG_FANOUT = "catalog.fanout";

    private final Map<Class<? extends EntityName>, List<EntityReferences<?>>> providersByType;
    private final Resilience resilience;

    public ReferenceRouting(List<EntityReferences<?>> providers, Resilience resilience) {
        Map<Class<? extends EntityName>, List<EntityReferences<?>>> indexed = new LinkedHashMap<>();
        for (EntityReferences<?> p : providers) {
            indexed.computeIfAbsent(p.referenceType(), k -> new ArrayList<>()).add(p);
        }
        Map<Class<? extends EntityName>, List<EntityReferences<?>>> immutable = new LinkedHashMap<>();
        indexed.forEach((k, v) -> immutable.put(k, List.copyOf(v)));
        this.providersByType = Collections.unmodifiableMap(immutable);
        this.resilience = resilience;
    }

    public Set<DomainId> domainsReferencing(Class<? extends EntityName> referenceType) {
        if (referenceType == null) return Set.of();
        return providersByType.getOrDefault(referenceType, List.of()).stream()
                .map(EntityReferences::domain).collect(Collectors.toUnmodifiableSet());
    }

    public Map<DomainId, List<EntityRef>> findReferencesTo(EntityName target) {
        if (target == null) return Map.of();
        List<EntityReferences<?>> handlers = providersByType.getOrDefault(target.getClass(), List.of());
        Timeout timeout = resilience.timeout(CATALOG_FANOUT);
        CircuitBreaker breaker = resilience.circuitBreaker(CATALOG_FANOUT);
        Map<DomainId, List<EntityRef>> grouped = new LinkedHashMap<>();
        for (EntityReferences<?> handler : handlers) {
            List<EntityRef> refs = invokeQuietly(handler, target, timeout, breaker);
            if (!refs.isEmpty()) grouped.computeIfAbsent(handler.domain(), k -> new ArrayList<>()).addAll(refs);
        }
        Map<DomainId, List<EntityRef>> immutable = new LinkedHashMap<>();
        grouped.forEach((k, v) -> immutable.put(k, List.copyOf(v)));
        return Collections.unmodifiableMap(immutable);
    }

    private static List<EntityRef> invokeQuietly(EntityReferences<?> h, EntityName t, Timeout timeout, CircuitBreaker breaker) {
        try {
            return breaker.execute(() -> timeout.execute(() -> invoke(h, t).toList()));
        } catch (RuntimeException ignored) {
            return List.of();
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Stream<EntityRef> invoke(EntityReferences<?> h, EntityName t) {
        return ((EntityReferences) h).referencesTo(h.referenceType().cast(t));
    }
}
