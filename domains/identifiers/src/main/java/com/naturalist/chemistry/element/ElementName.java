package com.naturalist.chemistry.element;

import com.naturalist.ddd.EntityName;

public final class ElementName extends EntityName {

    private ElementName(String value) {
        super(value);
    }

    public static ElementName of(String value) {
        return new ElementName(value);
    }

    @Override
    protected int maxLength() {
        return 64;
    }
}