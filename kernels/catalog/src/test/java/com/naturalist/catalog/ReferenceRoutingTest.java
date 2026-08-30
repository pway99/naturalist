package com.naturalist.catalog;

import com.naturalist.ddd.EntityName;
import com.naturalist.resilience.Resilience;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class ReferenceRoutingTest {

    private record Plants() implements DomainId { public String value() { return "plants"; } }
    private static final class Compound extends EntityName {
        private Compound(String v) { super(v); } static Compound of(String v){ return new Compound(v);}
        protected int maxLength(){ return 96; } }

    private static EntityReferences<Compound> provider(DomainId d, EntityRef... refs) {
        return new EntityReferences<>() {
            public DomainId domain() { return d; }
            public Class<Compound> referenceType() { return Compound.class; }
            public Stream<EntityRef> referencesTo(Compound t) { return Stream.of(refs); }
        };
    }

    @Test
    void groupsReferencesByDomain() {
        DomainId plants = new Plants();
        EntityRef ref = new EntityRef(plants, Compound.of("california-pipevine"));
        ReferenceRouting routing = new ReferenceRouting(List.of(provider(plants, ref)), Resilience.noOp());

        Map<DomainId, List<EntityRef>> found = routing.findReferencesTo(Compound.of("aristolochic-acid"));

        assertEquals(Set.of(plants), found.keySet());
        assertEquals(List.of(ref), found.get(plants));
        assertEquals(Set.of(plants), routing.domainsReferencing(Compound.class));
    }

    @Test
    void nullTargetYieldsEmptyMap() {
        ReferenceRouting routing = new ReferenceRouting(List.of(), Resilience.noOp());
        assertTrue(routing.findReferencesTo(null).isEmpty());
        assertTrue(routing.domainsReferencing(null).isEmpty());
    }
}
