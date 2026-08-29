package com.naturalist.persistence.test.nofanout;

import com.naturalist.ddd.Named;
import com.naturalist.observability.Constraints;

import java.util.function.Consumer;

/** Minimal fake entity for the weaving proof — never persisted. */
record Gadget(String key) implements Named<String> {
    @Override
    public Consumer<? extends Constraints> invariants() {
        return c -> {
        };
    }
}
