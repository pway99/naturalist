package com.naturalist.insects;

import com.naturalist.ddd.BehavioralCollection;

import java.util.Collection;
import java.util.List;

public final class InsectImageCollection extends BehavioralCollection<InsectImage> {

    InsectImageCollection(Collection<InsectImage> images) {
        super(images);
    }

    public static InsectImageCollection of(Collection<InsectImage> images) {
        return new InsectImageCollection(images);
    }

    public static InsectImageCollection empty() {
        return new InsectImageCollection(List.of());
    }
}
