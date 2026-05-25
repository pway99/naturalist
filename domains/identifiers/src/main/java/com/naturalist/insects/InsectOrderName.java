package com.naturalist.insects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.naturalist.ddd.EntityName;
import com.naturalist.taxonomy.LinealRank;

/**
 * Strongly typed natural key for {@code InsectOrder} entities.
 * <p>
 * The slug is the lowercased Linnaean order epithet —
 * {@code "diptera"}, {@code "hymenoptera"}, {@code "lepidoptera"}.
 * Single word, no hyphens — order is the topmost rank within
 * Class Insecta.
 */
public final class InsectOrderName extends EntityName implements InsectRankName {

    private InsectOrderName(String value) {
        super(value);
    }

    @JsonCreator
    public static InsectOrderName of(String value) {
        return new InsectOrderName(value);
    }

    @Override
    protected int maxLength() {
        return 64;
    }

    @Override
    public LinealRank rank() {
        return LinealRank.ORDER;
    }
}
