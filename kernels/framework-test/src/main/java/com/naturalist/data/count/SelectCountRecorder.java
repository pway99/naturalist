package com.naturalist.data.count;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Thread-local recorder for the N+1 select gate. The AspectJ {@code SelectCountAspect}
 * feeds it; {@code NaturalistTestExtension} arms it before each test and reads its
 * {@link #snapshot()} after. State is per-thread and bounded to a single test: the head
 * stack self-clears at outermost exit, and {@link #arm()}/{@link #disarm()} reset it, so
 * a pooled thread reused across tests always starts clean.
 */
public final class SelectCountRecorder {

    private static final class Scope {
        boolean armed;
        final Deque<String> heads = new ArrayDeque<>();
        final Map<String, Map<String, Integer>> tally = new LinkedHashMap<>();
    }

    private static final ThreadLocal<Scope> SCOPE = ThreadLocal.withInitial(Scope::new);

    private SelectCountRecorder() {
    }

    public static void arm() {
        Scope s = SCOPE.get();
        s.armed = true;
        s.heads.clear();
        s.tally.clear();
    }

    public static void disarm() {
        SCOPE.remove();
    }

    /** Push a head-of-DAG query. Always maintained so nesting is correct even before a select. */
    public static void enterQuery(String headFqn) {
        SCOPE.get().heads.push(headFqn);
    }

    public static void exitQuery() {
        Deque<String> heads = SCOPE.get().heads;
        if (!heads.isEmpty()) {
            heads.pop();
        }
    }

    /** Record a repository select against the outermost enclosing head, when armed. */
    public static void recordSelect(String selectFqn) {
        Scope s = SCOPE.get();
        if (!s.armed || s.heads.isEmpty()) {
            return;
        }
        String head = s.heads.peekLast(); // outermost = bottom of the stack
        s.tally.computeIfAbsent(head, k -> new LinkedHashMap<>())
                .merge(selectFqn, 1, Integer::sum);
    }

    /** A defensive copy of the current tally: head FQN -> (select FQN -> count). */
    public static Map<String, Map<String, Integer>> snapshot() {
        Map<String, Map<String, Integer>> copy = new LinkedHashMap<>();
        SCOPE.get().tally.forEach((head, selects) -> copy.put(head, new LinkedHashMap<>(selects)));
        return copy;
    }
}
