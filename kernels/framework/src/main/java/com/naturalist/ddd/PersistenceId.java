package com.naturalist.ddd;

import java.util.Objects;

public abstract class PersistenceId<T> {
    final T value;

    protected PersistenceId(T value) {
        this.value = value;
    }

    public T value() {
        return value;
    }

    public boolean isValid() {
        return value != null;
    }

    public boolean isNotValid() {
        return !isValid();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PersistenceId<?> that = (PersistenceId<?>) o;
        return Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(value);
    }

    @Override
    public String toString() {
        return String.valueOf(value);
    }
}
