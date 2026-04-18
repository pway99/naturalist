package com.naturalist.chemistry.element;

import com.naturalist.ddd.CatalogName;

public final class ElementName extends CatalogName {

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