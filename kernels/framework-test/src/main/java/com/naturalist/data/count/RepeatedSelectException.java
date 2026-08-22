package com.naturalist.data.count;

/** Raised by the N+1 select gate when a head-of-DAG query repeats a repository select. */
public final class RepeatedSelectException extends AssertionError {
    public RepeatedSelectException(String message) {
        super(message);
    }
}
