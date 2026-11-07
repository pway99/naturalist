package com.naturalist.test.query.nofanout;

import java.util.List;

/** A unit under test that loops over a graph-built {@link GraphFooLookup}. */
class GraphFooCaller {
    private final GraphFooLookup lookup;

    GraphFooCaller(GraphFooLookup lookup) {
        this.lookup = lookup;
    }

    void loop(List<String> keys) {
        for (String key : keys) {
            lookup.one(key);
        }
    }
}
