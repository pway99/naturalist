package com.naturalist.authority.eol;

import com.naturalist.ddd.Named;
import com.naturalist.observability.Constraints;

import java.util.function.Consumer;

/**
 * A single fixture mapping from a subject slug to its EOL page id.
 * Used by {@link EolAuthorityTestEntitySource} to seed
 * {@link EolClientMock} from a JSON catalog file.
 */
public record EolAuthorityEntry(String name, EolPageId pageId) implements Named<String> {

    @Override
    public Consumer<? extends Constraints> invariants() {
        return i -> i
                .notBlank(name, "name")
                .namedValue(pageId, "pageId");
    }
}