package com.naturalist.chemistry.compound;

import com.naturalist.data.TestEntitySource;
import com.naturalist.data.UniqueConstraint;

import java.util.List;
import java.util.function.Function;

public class CompoundTestEntitySource extends TestEntitySource<CompoundName, Compound> {
    public CompoundTestEntitySource() {
        loadFile("chemistry/compound/compounds.json");
    }

    @Override
    protected List<UniqueConstraint<Compound>> uniqueConstraints() {
        return List.of(
                new UniqueConstraint<>() {
                    @Override
                    public String name() {
                        return "commonName";
                    }

                    @Override
                    public Function<Compound, ?> valueFunction() {
                        return Compound::commonName;
                    }
                },
                new UniqueConstraint<>() {
                    @Override
                    public String name() {
                        return "formula";
                    }

                    @Override
                    public Function<Compound, ?> valueFunction() {
                        return c -> c.compoundInfo().formula();
                    }
                }
        );
    }
}
