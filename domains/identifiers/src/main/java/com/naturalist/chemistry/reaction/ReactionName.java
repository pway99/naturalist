package com.naturalist.chemistry.reaction;

import com.naturalist.ddd.CatalogName;

public final class ReactionName extends CatalogName {

    private ReactionName(String value) {
        super(value);
    }

    public static ReactionName of(String value) {
        return new ReactionName(value);
    }

    @Override
    protected int maxLength() {
        return 128;
    }
}