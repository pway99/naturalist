package com.naturalist.garden;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.naturalist.ddd.EntityName;

/**
 * The name of a cultivated category (e.g. {@code "tomato"}, {@code "lettuce"}) — what is being
 * grown, as distinct from the botanical species it belongs to. Owned by the garden domain, whose
 * {@code CropInfo} is keyed by it.
 * <p>
 * Soil holds this as a soft reference on {@code LabAnalysisInfo.crop}: the crop a lab analysis was
 * interpreted for, and therefore whose optimum ranges the report's targets belong to. That edge
 * runs soil → garden by typed name only; neither module imports the other.
 * <p>
 * It lived under {@code com.naturalist.soil} until the garden domain existed — an artifact of soil
 * having been built first, not a claim of ownership.
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
