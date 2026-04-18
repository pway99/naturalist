package com.naturalist.ddd;

public interface EntityName<N> {
    N value();

    boolean isValid();

    default boolean isNotValid() {
        return !isValid();
    }
}
