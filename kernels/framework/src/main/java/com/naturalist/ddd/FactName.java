package com.naturalist.ddd;

import java.util.UUID;

public abstract class FactName implements EntityName<UUID> {
    final UUID value;

    protected FactName(UUID value) {
        this.value = value;
    }

    public UUID value() {
        return value;
    }

    public boolean isValid() {
        return value != null;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FactName that = (FactName) o;
        return java.util.Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hashCode(value);
    }

    @Override
    public String toString() {
        return String.valueOf(value);
    }
}