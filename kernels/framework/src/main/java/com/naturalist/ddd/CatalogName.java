package com.naturalist.ddd;

import java.util.regex.Pattern;

public abstract class CatalogName implements EntityName<String> {

    private static final Pattern LOWER_KEBAB = Pattern.compile("^[a-z0-9]+(-[a-z0-9]+)*$");

    final String value;

    protected CatalogName(String value) {
        this.value = value;
    }

    protected abstract int maxLength();

    @Override
    public String value() {
        return value;
    }

    @Override
    public boolean isValid() {
        return value != null
                && value.length() <= maxLength()
                && LOWER_KEBAB.matcher(value).matches();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CatalogName that = (CatalogName) o;
        return java.util.Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hashCode(value);
    }

    @Override
    public String toString() {
        return value;
    }
}