package com.naturalist.soil;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.naturalist.ddd.EntityName;

/**
 * The name of the crop a soil analysis was interpreted for — the reference that ties an analysis to
 * its optimum-range context (e.g. {@code "tomato"}). A soft key, not an anemic value: the rich
 * crop-planting concept (a season's planting across soil profiles, its amendments, and outcomes)
 * is a domain of its own; this is the identifier those efforts resolve.
 */
public final class CropName extends EntityName {

    private CropName(String value) {
        super(value);
    }

    @JsonCreator
    public static CropName of(String value) {
        return new CropName(value);
    }

    @Override
    protected int maxLength() {
        return 48;
    }
}
