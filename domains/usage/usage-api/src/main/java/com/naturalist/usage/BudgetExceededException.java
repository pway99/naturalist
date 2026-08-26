package com.naturalist.usage;

import java.time.Instant;

public final class BudgetExceededException extends RuntimeException {

    private final LimitKind limitKind;
    private final Instant resetAt;

    public BudgetExceededException(LimitKind limitKind, Instant resetAt) {
        super("Identification budget exceeded: %s (resets at %s)".formatted(limitKind, resetAt));
        this.limitKind = limitKind;
        this.resetAt = resetAt;
    }

    public LimitKind limitKind() {
        return limitKind;
    }

    public Instant resetAt() {
        return resetAt;
    }
}
