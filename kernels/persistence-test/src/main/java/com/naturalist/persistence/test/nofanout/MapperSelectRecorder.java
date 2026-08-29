package com.naturalist.persistence.test.nofanout;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Thread-local recorder for the RDBMS mapper-select fan-out gate — the sibling of
 * {@code com.naturalist.test.query.nofanout.SelectCountRecorder}. {@link RepositoryHeadAspect}
 * pushes/pops repository-method heads; {@link MapperSelectInterceptor} records one select per
 * MyBatis {@code Executor.query} against the current head; {@code RdbmsTestExtension} arms it
 * before each test and evaluates its {@link #snapshot()} after.
 *
 * <p><b>Gating is per repository-method INVOCATION, not per-statement-across-the-test.</b>
 * Selects fold into the persistent {@code tally} via {@link Math#max} only when the outermost
 * head exits, so a test that calls one repository method N times (each doing one select) reads
 * as count 1 — only a <i>single</i> invocation looping a select {@code >1×} is an N+1.
 *
 * <p>Deliberately a <b>separate</b> ThreadLocal from the in-memory gate's recorder: the
 * in-memory {@code SelectCountAspect} also weaves the {@code *Rdbms} classes but is never armed
 * during an RDBMS {@code *IT}, so keeping the recorders apart prevents any cross-talk.
 */
public final class MapperSelectRecorder {

    private static final class Scope {
        boolean armed;
        final Deque<String> heads = new ArrayDeque<>();
        /** Per-outermost-invocation counts; reset when a new outermost head is entered. */
        final Map<String, Map<String, Integer>> current = new LinkedHashMap<>();
        /** Persistent per-test tally; folds {@code current} via MAX at each outermost exit. */
        final Map<String, Map<String, Integer>> tally = new LinkedHashMap<>();
    }

    private static final ThreadLocal<Scope> SCOPE = ThreadLocal.withInitial(Scope::new);

    private MapperSelectRecorder() {
    }

    public static void arm() {
        Scope s = SCOPE.get();
        s.armed = true;
        s.heads.clear();
        s.current.clear();
        s.tally.clear();
    }

    public static void disarm() {
        SCOPE.remove();
    }

    /** Push a repository-method head. A new outermost head starts a fresh per-invocation count. */
    public static void enterRepository(String headFqn) {
        Scope s = SCOPE.get();
        if (s.heads.isEmpty()) {
            s.current.clear();
        }
        s.heads.push(headFqn);
    }

    public static void exitRepository() {
        Scope s = SCOPE.get();
        if (s.heads.isEmpty()) {
            return;
        }
        s.heads.pop();
        if (s.heads.isEmpty()) {
            s.current.forEach((head, selects) -> {
                Map<String, Integer> agg = s.tally.computeIfAbsent(head, k -> new LinkedHashMap<>());
                selects.forEach((sel, cnt) -> agg.merge(sel, cnt, Math::max));
            });
            s.current.clear();
        }
    }

    /** Record one mapper select against the outermost enclosing head, when armed. */
    public static void recordSelect(String statementId) {
        Scope s = SCOPE.get();
        if (!s.armed || s.heads.isEmpty()) {
            return;
        }
        String head = s.heads.peekLast(); // outermost = bottom of the stack
        s.current.computeIfAbsent(head, k -> new LinkedHashMap<>())
                .merge(statementId, 1, Integer::sum);
    }

    /** Defensive copy of the tally: head FQN -> (mapped-statement id -> max-per-invocation count). */
    public static Map<String, Map<String, Integer>> snapshot() {
        Map<String, Map<String, Integer>> copy = new LinkedHashMap<>();
        SCOPE.get().tally.forEach((head, selects) -> copy.put(head, new LinkedHashMap<>(selects)));
        return copy;
    }
}
