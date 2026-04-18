package com.naturalist.exception;

public class NullEntityException extends Exception {
    final Class atSourceClass;

    private NullEntityException(Class<?> atSourceClass) {
        this.atSourceClass = atSourceClass;
    }

    public static NullEntityException atSource(Class<?> clazz) {
        return new NullEntityException(clazz);
    }

    @Override
    public String getMessage() {
        return "Null Entity at: %S".formatted(atSourceClass.getSimpleName());
    }


}
