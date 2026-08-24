package com.naturalist.data.count;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Thread-local recorder for the N+1 select gate. The AspectJ {@code SelectCountAspect}
 * feeds it; {@code NaturalistTestExtension} arms it before each test and reads its
 * {@link #snapshot()} after.
 *
 * <p><b>Gating is per head-of-DAG INVOCATION, not per-FQN-across-the-test.</b> Selects
 * are counted into a fresh {@code current} map each time an <i>outermost</i> query is
 * entered (stack empty → depth 1); when that outermost query exits (depth 1 → 0) the
 * per-invocation counts fold into the persistent {@code tally} via {@link Math#max}. So a
 * caller that invokes the same query method N times — each doing one select — reads as
 * count 1 (max of N ones), never a phantom N; only a <i>single</i> invocation that loops a
 * select {@code >1×} is a real N+1. Nested sub-queries roll up to the outermost head, so
 * the whole subtree of one invocation shares one budget.
 *
 * <p>State is per-thread and bounded to a single test: the head stack self-clears at
 * outermost exit, and {@link #arm()}/{@link #disarm()} reset it, so a pooled thread reused
 * across tests always starts clean. The chain is synchronous on the test thread (surefire
 * is single-threaded here; no cross-thread fan-out in test paths). If a query ever fans out
 * across threads (parallelStream / executor), the head stack would not propagate to worker
 * threads and that select would be <b>under-counted, never miscounted</b> — the gate can
 * miss such an N+1 but can never false-fail on it.
 */
public final class SelectCountRecorder {

    private static final class Scope {
        boolean armed;
        final Deque<String> heads = new ArrayDeque<>();
        /** Per-outermost-invocation counts; reset when a new outermost query is entered. */
        final Map<String, Map<String, Integer>> current = new LinkedHashMap<>();
        /** Persistent per-test tally; folds {@code current} via MAX at each outermost exit. */
        final Map<String, Map<String, Integer>> tally = new LinkedHashMap<>();
    }

    private static final ThreadLocal<Scope> SCOPE = ThreadLocal.withInitial(Scope::new);

    private SelectCountRecorder() {
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

    /** Push a head-of-DAG query. A new outermost head starts a fresh per-invocation count. */
    public static void enterQuery(String headFqn) {
        Scope s = SCOPE.get();
        if (s.heads.isEmpty()) {
            s.current.clear();
        }
        s.heads.push(headFqn);
    }

    public static void exitQuery() {
        Scope s = SCOPE.get();
        if (s.heads.isEmpty()) {
            return;
        }
        s.heads.pop();
        if (s.heads.isEmpty()) {
            // Outermost invocation complete: fold its per-invocation counts into the
            // persistent tally via MAX, so repeated invocations of one head never sum.
            s.current.forEach((head, selects) -> {
                Map<String, Integer> agg = s.tally.computeIfAbsent(head, k -> new LinkedHashMap<>());
                selects.forEach((sel, cnt) -> agg.merge(sel, cnt, Math::max));
            });
            s.current.clear();
        }
    }

    /** Record a repository select against the outermost enclosing head, when armed. */
    public static void recordSelect(String selectFqn) {
        Scope s = SCOPE.get();
        if (!s.armed || s.heads.isEmpty()) {
            return;
        }
        String head = s.heads.peekLast(); // outermost = bottom of the stack
        s.current.computeIfAbsent(head, k -> new LinkedHashMap<>())
                .merge(selectFqn, 1, Integer::sum);
    }

    /** A defensive copy of the current tally: head FQN -> (select FQN -> max-per-invocation count). */
    public static Map<String, Map<String, Integer>> snapshot() {
        Map<String, Map<String, Integer>> copy = new LinkedHashMap<>();
        SCOPE.get().tally.forEach((head, selects) -> copy.put(head, new LinkedHashMap<>(selects)));
        return copy;
    }
}
