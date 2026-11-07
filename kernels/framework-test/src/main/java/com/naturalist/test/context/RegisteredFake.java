package com.naturalist.test.context;

/** One fake a test handed to a context: the context's simple name, the type it replaces, and the fake's class. */
public record RegisteredFake(String context, Class<?> type, Class<?> fakeClass) {
}
