package com.naturalist.library;

import com.naturalist.ddd.ReadModel;
import com.naturalist.observability.Constraints;

import java.util.List;
import java.util.function.Consumer;

public record CladeView(
        CladeStep subject,
        List<CladeStep> ancestry,
        List<CladeStep> children
) implements ReadModel {

    @Override
    public Consumer<? extends Constraints> invariants() {
        return i -> i
                .valueObject(subject, "subject")
                .notNull(ancestry, "ancestry")
                .notNull(children, "children");
    }
}
