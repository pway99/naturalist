package com.naturalist.data.count;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/** Applies the N+1 rule (any select called more than once within one head is a violation). */
public final class SelectGate {

    private SelectGate() {
    }

    public static void evaluate(Map<String, Map<String, Integer>> snapshot,
                                Collection<AllowRepeatedSelect> allowlist) {
        List<String> violations = new ArrayList<>();
        for (Map.Entry<String, Map<String, Integer>> head : snapshot.entrySet()) {
            for (Map.Entry<String, Integer> select : head.getValue().entrySet()) {
                if (select.getValue() > 1 && !allowed(head.getKey(), select.getKey(), allowlist)) {
                    violations.add("  head   %s%n  select %s  called %d×  (expected ≤ 1)"
                            .formatted(head.getKey(), select.getKey(), select.getValue()));
                }
            }
        }
        if (!violations.isEmpty()) {
            throw new RepeatedSelectException(
                    "N+1 select detected%n%s".formatted(String.join(System.lineSeparator(), violations)));
        }
    }

    private static boolean allowed(String headFqn, String selectFqn, Collection<AllowRepeatedSelect> allowlist) {
        return allowlist.stream()
                .anyMatch(a -> matches(headFqn, a.query()) && matches(selectFqn, a.select()));
    }

    /** Match by exact string, by {@code .}-suffix, or by simple name (segment after the last dot). */
    private static boolean matches(String fqn, String pattern) {
        if (fqn.equals(pattern) || fqn.endsWith("." + pattern)) {
            return true;
        }
        int dot = fqn.lastIndexOf('.');
        String simple = dot < 0 ? fqn : fqn.substring(dot + 1);
        return simple.equals(pattern);
    }
}
