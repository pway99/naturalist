package com.naturalist.test.query.nofanout;

import com.naturalist.data.AbstractTestEntityRepository;
import com.naturalist.data.SuppressRepeatedSelect;
import com.naturalist.data.NaturalistDatabase;
import com.naturalist.ddd.Named;
import com.naturalist.observability.Constraints;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

/* The unit under test for SelectTraceTest. Top level, because the tracer tells the unit from the test by outer class. */

record Foo(String key) implements Named<String> {
    @Override
    public Consumer<? extends Constraints> invariants() {
        return c -> { };
    }
}

record Bar(String key, String foo) implements Named<String> {
    @Override
    public Consumer<? extends Constraints> invariants() {
        return c -> { };
    }
}

class FooRepository extends AbstractTestEntityRepository<String, Foo, FooSource> {
    FooRepository(NaturalistDatabase database) {
        super(database);
    }

    Optional<Foo> byKey(String key) {
        return testEntitySource().getByName(key);
    }

    List<Foo> byKeys(Set<String> keys) {
        return testEntitySource().getByEntityNameSet(keys);
    }

    List<Foo> filtered(String key) {
        return testEntitySource().entityStream().filter(foo -> foo.key().equals(key)).toList();
    }
}

class BarRepository extends AbstractTestEntityRepository<String, Bar, BarSource> {
    BarRepository(NaturalistDatabase database) {
        super(database);
    }

    void add(Bar bar) {
        testEntitySource().insert(bar);
    }

    void addIfNew(Bar bar) {
        if (testEntitySource().getByName(bar.key()).isEmpty()) {
            testEntitySource().insert(bar);
        }
    }

    Optional<Bar> byKey(String key) {
        return testEntitySource().getByName(key);
    }
}

class FooLookup {
    private final FooRepository foos;

    FooLookup(FooRepository foos) {
        this.foos = foos;
    }

    void one(String key) {
        foos.byKey(key);
    }
}

interface FooPortQuery {
    void one(String key);

    void loop(List<String> keys);

    FooPortQuery self();

    void fail();
}

class FooCaller {
    private final FooPortQuery query;

    FooCaller(FooPortQuery query) {
        this.query = query;
    }

    void loop(List<String> keys) {
        keys.forEach(query::one);
    }

    void each(List<String> keys, Consumer<String> callback) {
        keys.forEach(callback);
    }
}

class FooQuery implements FooPortQuery {
    private final FooRepository foos;
    private final FooLookup lookup;

    FooQuery(FooRepository foos) {
        this.foos = foos;
        this.lookup = new FooLookup(foos);
    }

    @Override
    public void loop(List<String> keys) {
        for (String key : keys) {
            foos.byKey(key);
        }
    }

    @SuppressRepeatedSelect(reason = "fixture")
    void suppressedLoop(List<String> keys) {
        keys.forEach(foos::byKey);
    }

    @SuppressRepeatedSelect(reason = "fixture")
    void suppressedThroughLoop(List<String> keys) {
        loop(keys);
    }

    void suppressedThenLoop(List<String> keys) {
        suppressedLoop(keys);
        loop(keys);
    }

    void streamLoop(List<String> keys) {
        keys.stream().map(foos::filtered).toList();
    }

    void composed(List<String> keys) {
        keys.forEach(lookup::one);
    }

    void twoLines(String first, String second) {
        foos.byKey(first);
        foos.byKey(second);
    }

    void batched(Set<String> keys) {
        foos.byKeys(keys);
    }

    @Override
    public void one(String key) {
        foos.byKey(key);
    }

    @Override
    public FooPortQuery self() {
        return this;
    }

    @Override
    public void fail() {
        throw new UnsupportedOperationException();
    }

    void other(String key) {
        foos.filtered(key);
    }
}

class BarCommand {
    private final BarRepository bars;
    private final FooRepository foos;

    BarCommand(BarRepository bars, FooRepository foos) {
        this.bars = bars;
        this.foos = foos;
    }

    void addAll(List<Bar> toAdd) {
        toAdd.forEach(bars::add);
    }

    void addAllIfNew(List<Bar> toAdd) {
        toAdd.forEach(bars::addIfNew);
    }

    void addIfNewAndReadInLoop(List<Bar> toAdd) {
        for (Bar bar : toAdd) {
            bars.addIfNew(bar);
            bars.byKey(bar.key());
        }
    }

    void readInLoop(List<Bar> toAdd) {
        for (Bar bar : toAdd) {
            foos.byKey(bar.foo());
            bars.add(bar);
        }
    }
}
