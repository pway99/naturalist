package com.naturalist.authority;

import com.naturalist.ddd.ValueObject;
import com.naturalist.observability.Constraints;

import java.util.function.Consumer;

/**
 * The external authority a reference came from — e.g. EOL, iNaturalist.
 * Open provider metadata: the kernel names no concrete source; each
 * provider defines its own {@code AuthoritySource} constant in its api.
 */
public record AuthoritySource(String id, String displayName) implements ValueObject {

    @Override
    public Consumer<? extends Constraints> invariants() {
        return i -> i
                .notBlank(id, "id")
                .notBlank(displayName, "displayName");
    }
}