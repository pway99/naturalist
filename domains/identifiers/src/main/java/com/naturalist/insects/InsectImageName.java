package com.naturalist.insects;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.naturalist.ddd.FactName;

import java.util.UUID;

public final class InsectImageName extends FactName {
    private InsectImageName(UUID value) { super(value); }

    @JsonCreator
    public static InsectImageName of(UUID value) { return new InsectImageName(value); }
}
