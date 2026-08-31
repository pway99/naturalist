package com.naturalist.naturalist;

import com.naturalist.data.NaturalistDatabase;
import com.naturalist.data.TestEntitySource;
import com.naturalist.data.UniqueConstraint;

import java.util.List;
import java.util.function.Function;

public class NaturalistTestEntitySource extends TestEntitySource<NaturalistName, Naturalist> {

    public NaturalistTestEntitySource(NaturalistDatabase database) {
        super(database);
        loadFile("naturalists/naturalists.json");
    }

    @Override
    protected List<UniqueConstraint<Naturalist>> uniqueConstraints() {
        return List.of(
                new UniqueConstraint<>() {
                    @Override
                    public String name() {
                        return "publicHandle";
                    }

                    @Override
                    public Function<Naturalist, ?> valueFunction() {
                        return Naturalist::publicHandle;
                    }
                });
    }
}
