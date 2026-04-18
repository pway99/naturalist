package com.naturalist.exception;

public class PersistenceIdException extends RuntimeException {
    final Class atSourceClass;

    private PersistenceIdException(Class<?> atSourceClass) {
        this.atSourceClass = atSourceClass;
    }

    public static PersistenceIdException atSource(Class<?> clazz) {
        return new PersistenceIdException(clazz);
    }

    @Override
    public String getMessage() {
        return "Null PersistenceID at: %S".formatted(atSourceClass.getSimpleName());
    }


}
