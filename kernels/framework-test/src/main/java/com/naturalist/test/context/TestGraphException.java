package com.naturalist.test.context;

/** The test object graph cannot hand out what was asked for; the message names the context to fix. */
public final class TestGraphException extends RuntimeException {
    public TestGraphException(String message) {
        super(message);
    }

    public TestGraphException(String message, Throwable cause) {
        super(message, cause);
    }
}
