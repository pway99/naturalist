package com.naturalist.data;

import com.naturalist.ddd.Aggregate;
import com.naturalist.observability.Observer;
import jakarta.transaction.Transactional;

/**
 * Abstract base for coordinated multi-entity writes that persist an
 * {@link Aggregate} atomically — the write-side analogue of an aggregate
 * factory on the read side.
 *
 * <p>Public {@link #execute(Aggregate)} is {@code final}: it validates the
 * aggregate's invariants via {@link Observer} before delegating to
 * {@link #doExecute(Aggregate)}, so a subclass cannot accidentally skip
 * validation. This mirrors the template in {@link AbstractEntityCommand}.
 *
 * <p>Subclasses are long-lived, constructor-injected beans — they receive
 * the entity commands and queries they need at construction time and reuse
 * them across invocations. When registered as Spring beans (via
 * {@link com.naturalist.infrastructure.DomainService @DomainService}), the
 * runtime adapter wraps {@code execute()} with transactional semantics so
 * the coordinated writes commit or roll back as a unit.
 *
 * @param <A> the aggregate type this transaction persists
 */
public abstract class Transaction<A extends Aggregate> {

    private final Observer observer = Observer.forClass(getClass());

    protected Observer observer() {
        return observer;
    }

    /**
     * Validates the aggregate's invariants and persists it. Void return
     * (CQS) — the caller already holds the inputs and can query for
     * post-write state if needed.
     *
     * @param aggregate the aggregate to persist; must pass its own
     *                  {@link com.naturalist.observability.Observable#invariants()}
     * @throws com.naturalist.observability.InvariantViolationException
     *         if any invariant on the aggregate fails
     */
    @Transactional
    public final void execute(A aggregate) {
        observer.arguments("execute", i -> i.aggregate(aggregate, "aggregate"))
                .throwWhenInvalid();
        doExecute(aggregate);
    }

    /**
     * Persistence hook — called after invariant validation succeeds.
     * Subclasses implement the coordinated writes (ensure parent entities,
     * insert/update child entities) without re-declaring validation.
     */
    protected abstract void doExecute(A aggregate);
}
