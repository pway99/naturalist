package com.naturalist.insects;

import com.naturalist.data.NaturalistDatabase;
import com.naturalist.data.TestEntitySource;
import com.naturalist.data.UniqueConstraint;

import java.util.List;
import java.util.function.Function;

public class InsectFeatureTestEntitySource extends TestEntitySource<InsectFeatureId, InsectFeature> {

    public InsectFeatureTestEntitySource(NaturalistDatabase database) {
        super(database);
        loadFile("insects/insect-features.json");
    }

    @Override
    protected List<UniqueConstraint<InsectFeature>> uniqueConstraints() {
        return List.of(
                new UniqueConstraint<>() {
                    @Override
                    public String name() {
                        return "value";
                    }

                    @Override
                    public Function<InsectFeature, ?> valueFunction() {
                        return InsectFeature::value;
                    }
                });
    }
}
