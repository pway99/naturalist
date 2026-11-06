package com.naturalist.test.query.nofanout;

import com.naturalist.data.NaturalistDatabase;
import com.naturalist.data.TestEntitySource;

public class FooSource extends TestEntitySource<String, Foo> {
    public FooSource(NaturalistDatabase database) {
        super(database);
    }
}
