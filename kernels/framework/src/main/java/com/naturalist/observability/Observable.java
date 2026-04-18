package com.naturalist.observability;

import java.util.function.Consumer;

public interface Observable {
    Consumer<? extends Constraints> invariants();
}