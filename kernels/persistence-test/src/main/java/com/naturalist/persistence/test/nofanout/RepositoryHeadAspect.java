package com.naturalist.persistence.test.nofanout;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;

/**
 * Load-time-woven head marker for the RDBMS mapper-select fan-out gate — the sibling of
 * {@code com.naturalist.test.query.nofanout.SelectCountAspect}, but keyed on repository methods
 * (the RDBMS {@code *IT} suites exercise repositories directly, with no {@code *QueryImpl} on the
 * stack). It wraps every public method on an {@link com.naturalist.data.AbstractEntityRepository}
 * subtype, pushing/popping the head so {@link MapperSelectInterceptor}'s selects attribute to the
 * right invocation. Never evaluates, never throws.
 *
 * <p>The pointcut targets {@code AbstractEntityRepository+} because the standard operations
 * ({@code getByName}, {@code getByEntityNameSet}, {@code getPage}, {@code insert}, {@code update},
 * {@code save}) are {@code final} on the base class — where a batched-vs-looped fan-out actually
 * hides — while domain-specific selects are declared on the concrete {@code *Rdbms} subclass.
 * Applied only under test (see {@code META-INF/aop.xml}); never on a production classpath.
 */
@Aspect
public class RepositoryHeadAspect {

    @Around("execution(public * com.naturalist.data.AbstractEntityRepository+.*(..))")
    public Object aroundRepository(ProceedingJoinPoint pjp) throws Throwable {
        MapperSelectRecorder.enterRepository(fqn(pjp));
        try {
            return pjp.proceed();
        } finally {
            MapperSelectRecorder.exitRepository();
        }
    }

    private static String fqn(ProceedingJoinPoint pjp) {
        return pjp.getTarget().getClass().getName() + "." + pjp.getSignature().getName();
    }
}
