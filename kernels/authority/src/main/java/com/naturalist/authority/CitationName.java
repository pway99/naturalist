package com.naturalist.authority;

import com.naturalist.ddd.EntityName;

public final class CitationName extends EntityName {

    public static CitationName of(String value) {
        return new CitationName(value);
    }

    private CitationName(String value) {
        super(value);
    }

    @Override
    protected int maxLength() {
        return 200;
    }
}
