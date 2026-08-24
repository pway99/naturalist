package com.naturalist.test.query.nofanout;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;

/**
 * Load-time-woven aspect for the N+1 (no-fan-out) select gate — the dumb feeder that
 * {@link SelectCountRecorder} tallies. It never evaluates the rule and never throws; all
 * judgement lives in {@link SelectGate}, invoked from {@code NaturalistTestExtension}'s
 * {@code afterEach}.
 *
 * <p>Two pointcuts, one recorder:
 * <ul>
 *   <li><b>Head</b> — {@link #aroundQuery} wraps every {@code *QueryImpl} method (and the
 *       inherited {@code AbstractEntityQuery} methods), pushing/popping the head-of-DAG stack
 *       so the recorder knows which query invocation a select belongs to. The outermost
 *       query on the stack owns the whole subtree's selects (nested sub-queries roll up).</li>
 *   <li><b>Select</b> — {@link #beforeSelect} fires on every {@code *Repository+} method
 *       <em>except</em> the writes {@code insert}/{@code update}/{@code save} (a bulk write
 *       loop is legitimate, not a read fan-out), recording one select against the current
 *       outermost head. {@code public} in the pointcut excludes protected {@code do*} template
 *       hooks that would otherwise double-count.</li>
 * </ul>
 *
 * <p>FQNs are keyed off the <em>runtime</em> class ({@code getTarget().getClass().getName()})
 * so {@code getByName} on different repositories never collides under a shared base class.
 *
 * <p>Applied only under test (see {@code META-INF/aop.xml}); {@code aspectjweaver} is a
 * test/runtime-scope dependency and the aspect is never on a production classpath.
 */
@Aspect
public class SelectCountAspect {

    @Around("execution(public * com.naturalist..*QueryImpl.*(..)) "
            + "|| execution(public * com.naturalist.data.AbstractEntityQuery.*(..))")
    public Object aroundQuery(ProceedingJoinPoint pjp) throws Throwable {
        SelectCountRecorder.enterQuery(fqn(pjp));
        try {
            return pjp.proceed();
        } finally {
            SelectCountRecorder.exitQuery();
        }
    }

    @Before("execution(public * com.naturalist..*Repository+.*(..)) "
            + "&& !execution(* com.naturalist..*.insert(..)) "
            + "&& !execution(* com.naturalist..*.update(..)) "
            + "&& !execution(* com.naturalist..*.save(..))")
    public void beforeSelect(JoinPoint jp) {
        SelectCountRecorder.recordSelect(fqn(jp));
    }

    private static String fqn(JoinPoint jp) {
        return jp.getTarget().getClass().getName() + "." + jp.getSignature().getName();
    }
}
