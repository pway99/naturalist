package com.naturalist.authority;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.naturalist.ddd.EntityName;

public final class CitationName extends EntityName {

    @JsonCreator
    public static CitationName of(@JsonProperty("value") String value) {
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
