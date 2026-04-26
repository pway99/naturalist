package com.naturalist.chemistry.compound;

import com.naturalist.data.TestEntitySource;
import com.naturalist.data.UniqueConstraint;

import java.util.List;
import java.util.function.Function;

public class CompoundDepictionTestEntitySource extends TestEntitySource<DepictionId, CompoundDepiction> {

    public CompoundDepictionTestEntitySource() {
        loadFile("chemistry/compound/depictions.json");
    }

    @Override
    protected List<UniqueConstraint<CompoundDepiction>> uniqueConstraints() {
        return List.of(
                new UniqueConstraint<>() {
                    @Override
                    public String name() {
                        return "compoundName";
                    }

                    @Override
                    public Function<CompoundDepiction, ?> valueFunction() {
                        return CompoundDepiction::compoundName;
                    }
                }
        );
    }
}
