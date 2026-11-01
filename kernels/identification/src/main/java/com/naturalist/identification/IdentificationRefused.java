package com.naturalist.identification;

import java.time.Instant;
import org.jspecify.annotations.Nullable;
import java.util.Objects;

/**
 * The port-level refusal of an identification attempt: the one type an adapter can catch
 * without depending on {@code usage-api} or the resilience kernel.
 *
 * <p>{@code detail} carries the budget's limit-kind name (for example {@code MONTHLY}) so the
 * adapter can word the message without importing {@code usage-api}. {@code resetAt} is when
 * the refusal is expected to lift, if known.
 *
 * <p>Shared across organism domains. The identification pipeline itself joins this kernel when
 * plants identification is built.
 */
public final class IdentificationRefused extends RuntimeException {

    /** Why the attempt was refused. */
    public enum Reason {
        RATE_LIMITED,
        BUDGET_EXHAUSTED
    }

    private final Reason reason;
    private final @Nullable String detail;
    private final @Nullable Instant resetAt;

    public IdentificationRefused(Reason reason, @Nullable String detail, @Nullable Instant resetAt) {
        super("Identification refused: " + reason + (detail == null ? "" : " (" + detail + ")"));
        this.reason = Objects.requireNonNull(reason, "reason");
        this.detail = detail;
        this.resetAt = resetAt;
    }

    public Reason reason() {
        return reason;
    }

    public @Nullable String detail() {
        return detail;
    }

    public @Nullable Instant resetAt() {
        return resetAt;
    }
}
