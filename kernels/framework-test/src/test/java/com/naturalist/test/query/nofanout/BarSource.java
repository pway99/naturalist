package com.naturalist.test.query.nofanout;

import com.naturalist.data.ForeignKeyConstraint;
import com.naturalist.data.NaturalistDatabase;
import com.naturalist.data.TestEntitySource;

import java.util.List;

public class BarSource extends TestEntitySource<String, Bar> {
    public BarSource(NaturalistDatabase database) {
        super(database);
    }

    @Override
    protected List<ForeignKeyConstraint<Bar, ?>> foreignKeyConstraints() {
        return List.of(ForeignKeyConstraint.of("foo", Bar::foo, FooSource.class));
    }
}
