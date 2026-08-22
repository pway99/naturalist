package com.naturalist.data.count;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;

/**
 * Load-time-woven aspect for the N+1 select gate. Maintains the head-of-DAG query stack and
 * records repository selects into {@link SelectCountRecorder}. Applied only under test
 * (see {@code META-INF/aop.xml}); never on a production classpath.
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
