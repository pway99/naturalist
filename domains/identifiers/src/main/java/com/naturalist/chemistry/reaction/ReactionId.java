package com.naturalist.chemistry.reaction;

import com.naturalist.ddd.PersistenceId;

/**
 * Strongly typed identifier for Reaction catalog entries.
 * Example: ReactionId.of("gypsum-dissolution")
 */
public final class ReactionId extends PersistenceId<Long> {

    private ReactionId(Long value) {
        super(value);
    }

    public static ReactionId of(Long value) {
        return new ReactionId(value);
    }
}
