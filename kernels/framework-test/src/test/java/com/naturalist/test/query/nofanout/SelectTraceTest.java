package com.naturalist.test.query.nofanout;

import com.naturalist.data.NaturalistTestExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** The extension evaluates after each test: a test that expects a repeat evaluates it here and re-arms. */
class SelectTraceTest {

    private static final List<String> KEYS = List.of("a", "b", "c");

    @RegisterExtension
    final NaturalistTestExtension nte = NaturalistTestExtension.create();

    private FooRepository foos;
    private FooQuery query;
    private BarCommand command;

    @BeforeEach
    void setUp() {
        foos = new FooRepository(nte);
        query = new FooQuery(foos);
        command = new BarCommand(new BarRepository(nte), foos);
        nte.getNamed(FooSource.class).insert(new Foo("p"));
    }

    private void assertRepeated(String expected) {
        assertThatThrownBy(() -> SelectTrace.evaluate("test"))
                .isInstanceOf(RepeatedSelectException.class)
                .hasMessageContaining(expected);
        SelectTrace.arm();
    }

    @Test
    void flagsALoopInAQuery() {
        query.loop(KEYS);
        assertRepeated("FooSource.byKey read 3 times by one call in test");
    }

    @Test
    void flagsAStreamLoopOverAnEntityStream() {
        query.streamLoop(KEYS);
        assertRepeated("FooSource.filtered read 3 times");
    }

    @Test
    void flagsALoopComposedAcrossClasses() {
        query.composed(KEYS);
        assertRepeated("FooSource.byKey read 3 times");
    }

    @Test
    void flagsTheSameSelectFromTwoLinesOfOneCall() {
        query.twoLines("a", "b");
        assertRepeated("FooSource.byKey read 2 times");
    }

    @Test
    void flagsALoopReachedThroughAnAssertionLambda() {
        assertThatThrownBy(() -> {
            query.loop(KEYS);
            throw new IllegalStateException();
        }).isInstanceOf(IllegalStateException.class);
        assertRepeated("FooSource.byKey read 3 times");
    }

    @Test
    void flagsAReadInACommandLoop() {
        command.readInLoop(List.of(new Bar("1", "p"), new Bar("2", "p")));
        assertRepeated("FooSource.byKey read 2 times");
    }

    @Test
    void namesTheLineThatMadeTheRead() {
        query.loop(KEYS);
        assertRepeated("\tat com.naturalist.test.query.nofanout.FooQuery.loop(SelectTraceFixtures.java:");
    }

    /** Not wrapped, one reading call in a test loop has the same stack as a loop in the unit, so it fails. */
    @Test
    void flagsATestLoopOverOneCallOnAnUnwrappedObject() {
        for (String key : KEYS) {
            query.one(key);
        }
        assertRepeated("FooSource.byKey read 3 times");
    }

    @Test
    void aTracedPortSeparatesPassesOfATestLoop() {
        FooPortQuery traced = SelectTrace.traced(FooPortQuery.class, query);
        for (String key : KEYS) {
            traced.one(key);
        }
        KEYS.forEach(traced::one);
    }

    @Test
    void aTracedPortStillFlagsALoopInsideOneCall() {
        SelectTrace.traced(FooPortQuery.class, query).loop(KEYS);
        assertRepeated("FooSource.byKey read 3 times");
    }

    @Test
    void aCallFromTheUnitToATracedPortDoesNotStartANewCount() {
        new FooCaller(SelectTrace.traced(FooPortQuery.class, query)).loop(KEYS);
        assertRepeated("FooSource.byKey read 3 times");
    }

    @Test
    void aTestCallbackInsideAUnitLoopDoesNotStartANewCount() {
        FooPortQuery traced = SelectTrace.traced(FooPortQuery.class, query);
        new FooCaller(traced).each(KEYS, key -> traced.one(key));
        assertRepeated("FooSource.byKey read 3 times");
    }

    @Test
    void aPortReturnedByATracedPortIsTraced() {
        FooPortQuery nested = SelectTrace.traced(FooPortQuery.class, query).self();
        for (String key : KEYS) {
            nested.one(key);
        }
    }

    @Test
    void aTracedPortThrowsWhatTheTargetThrows() {
        FooPortQuery traced = SelectTrace.traced(FooPortQuery.class, query);
        assertThatThrownBy(traced::fail).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void allowsABatchedRead() {
        query.batched(Set.copyOf(KEYS));
    }

    @Test
    void allowsSeparateCallsFromATest() {
        query.one("a");
        query.one("b");
        query.one("c");
    }

    @Test
    void allowsATestLoopThatAlternatesCalls() {
        KEYS.forEach(key -> {
            query.one(key);
            foos.byKey(key);
            query.other(key);
        });
    }

    @Test
    void allowsAWriteLoopWithForeignKeyChecks() {
        command.addAll(List.of(new Bar("1", "p"), new Bar("2", "p"), new Bar("3", "p")));
    }

    @Test
    void doesNotCountATestCallingARepositoryDirectly() {
        for (String key : KEYS) {
            foos.byKey(key);
        }
    }

    @Test
    void doesNotCountReadsUnderASuppressedMethod() {
        query.suppressedLoop(KEYS);
    }

    @Test
    void aSuppressionReachesReadsInWhatTheMethodCalls() {
        query.suppressedThroughLoop(KEYS);
    }

    @Test
    void stillCountsAReadOutsideTheSuppressedMethod() {
        query.suppressedThenLoop(KEYS);
        assertRepeated("FooSource.byKey read 3 times");
    }

}
