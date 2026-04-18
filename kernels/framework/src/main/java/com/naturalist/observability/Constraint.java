package com.naturalist.observability;

public interface Constraint<V> {

    String name();

    V value();

    boolean isValid();

    Constraint<V> withName(String name);

    /**
     * The class declaring the observation point — first segment of the
     * fully-qualified dotted name (e.g. {@code "CompoundQuery"} from
     * {@code "CompoundQuery.getByName.c.compoundInfo.formula"}).
     */
    default String source() {
        String n = name();
        int dot = n.indexOf('.');
        return dot > 0 ? n.substring(0, dot) : n;
    }

    /**
     * The method at the observation point — second segment of the
     * fully-qualified dotted name (e.g. {@code "getByName"} from
     * {@code "CompoundQuery.getByName.c.compoundInfo.formula"}).
     */
    default String methodName() {
        String n = name();
        int first = n.indexOf('.');
        if (first < 0) return n;
        int second = n.indexOf('.', first + 1);
        return second > 0 ? n.substring(first + 1, second) : n.substring(first + 1);
    }
}