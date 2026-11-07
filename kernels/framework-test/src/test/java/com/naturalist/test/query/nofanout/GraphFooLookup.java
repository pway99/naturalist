package com.naturalist.test.query.nofanout;

import com.naturalist.data.MockDomainService;

/** A concrete collaborator the test graph builds as a {@code SelectTrace.Traced} subclass. */
@MockDomainService
class GraphFooLookup {
    private final FooRepository foos;

    GraphFooLookup(FooRepository foos) {
        this.foos = foos;
    }

    void one(String key) {
        foos.byKey(key);
    }
}
