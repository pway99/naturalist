package com.naturalist.insects;

import com.naturalist.infrastructure.DomainService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.aop.support.AopUtils;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Every {@code *Transaction} bean in this module runs each of its writes inside one transaction
 * once Spring proxies it, as it does in the deployed app. The {@code *IT}s construct these beans
 * by hand on a rolled-back {@code SqlSession}, so no proxy exists there; this builds a small
 * context — not a Boot one — with Spring's real transaction advice and a recording stub
 * {@code PlatformTransactionManager}, and calls {@code insert}, {@code update} and {@code save}
 * through the proxy with a {@code null} entity, which fails inside each method. A begin then a
 * rollback proves the write is wrapped; a {@code final} method or a missing annotation records
 * neither. {@link InsectSpeciesTransactionProxyTest} drives one bean through a real commit and a
 * mid-write rollback.
 *
 * <p>The beans are discovered, not listed, so an adapter that gains one is covered without
 * touching this test; {@link #everyMultiTableAdapterHasItsTransaction} keeps the discovery from
 * passing on an empty scan.
 */
class InsectsRdbmsTransactionProxyTest {

    private static final List<String> WRITES = List.of("insert", "update", "save");

    static Set<String> transactions() {
        var scanner = new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(DomainService.class));
        Set<String> found = new TreeSet<>();
        scanner.findCandidateComponents("com.naturalist.insects").stream()
                .map(candidate -> candidate.getBeanClassName())
                .filter(name -> name.endsWith("Transaction"))
                .forEach(found::add);
        return found;
    }

    @Test
    void everyMultiTableAdapterHasItsTransaction() {
        assertThat(transactions()).contains(
                "com.naturalist.insects.InsectFamilyTransaction",
                "com.naturalist.insects.InsectFunctionalRoleTransaction",
                "com.naturalist.insects.InsectGenusTransaction",
                "com.naturalist.insects.InsectObservationTransaction",
                "com.naturalist.insects.InsectOrderTransaction",
                "com.naturalist.insects.InsectSpeciesTransaction",
                "com.naturalist.insects.lifestage.InsectLifeStageTransaction");
    }

    @ParameterizedTest
    @MethodSource("transactions")
    void everyWriteRunsInsideATransactionThatRollsBackOnFailure(String className) throws Exception {
        Class<?> type = Class.forName(className);
        EventLog log = new EventLog();
        Object target = construct(type, log);

        try (var context = new AnnotationConfigApplicationContext()) {
            context.register(ProxiedTransactionConfiguration.class);
            context.registerBean(RecordingTransactionManager.class, () -> new RecordingTransactionManager(log));
            registerTarget(context, type, target);
            context.refresh();
            Object bean = context.getBean(type);

            assertThat(AopUtils.isCglibProxy(bean))
                    .as("%s must be CGLIB-proxied, as it is in the deployed app", type.getSimpleName())
                    .isTrue();
            for (String write : WRITES) {
                Method method = write(type, write);
                log.clear();

                assertThatThrownBy(() -> invoke(method, bean))
                        .as("%s.%s(null)", type.getSimpleName(), write)
                        .isInstanceOf(RuntimeException.class);
                assertThat(log.events())
                        .as("%s.%s must begin a transaction and roll it back", type.getSimpleName(), write)
                        .startsWith("begin")
                        .endsWith("rollback")
                        .doesNotContain("commit");
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static <T> void registerTarget(AnnotationConfigApplicationContext context, Class<T> type, Object target) {
        context.registerBean(type, () -> (T) target);
    }

    /** The bean over a mapper whose every statement records its name and affects one row. */
    private static Object construct(Class<?> type, EventLog log) throws Exception {
        Constructor<?> constructor = type.getDeclaredConstructors()[0];
        constructor.setAccessible(true);
        Class<?> mapperType = constructor.getParameterTypes()[0];
        Object mapper = Proxy.newProxyInstance(mapperType.getClassLoader(), new Class<?>[] {mapperType},
                (proxy, method, args) -> {
                    if (method.getDeclaringClass() == Object.class) return method.invoke(log, args);
                    log.record(method.getName());
                    return method.getReturnType() == int.class ? 1 : null;
                });
        return constructor.newInstance(mapper);
    }

    private static Method write(Class<?> type, String name) {
        for (Method method : type.getDeclaredMethods()) {
            if (method.getName().equals(name) && method.getParameterCount() == 1) {
                method.setAccessible(true);
                return method;
            }
        }
        throw new AssertionError(type.getSimpleName() + " declares no one-argument " + name);
    }

    /** Calls through the proxy, unwrapping reflection so the write's own exception surfaces. */
    private static void invoke(Method method, Object bean) throws Throwable {
        try {
            method.invoke(bean, (Object) null);
        } catch (InvocationTargetException e) {
            throw e.getCause();
        }
    }

    /** Shared by the manager and the mapper, so the order of their events is observable. */
    static class EventLog {
        private final List<String> events = new ArrayList<>();

        void record(String event) {
            events.add(event);
        }

        void clear() {
            events.clear();
        }

        List<String> events() {
            return List.copyOf(events);
        }
    }

    /** Stub PlatformTransactionManager that records begin / commit / rollback. */
    static class RecordingTransactionManager extends AbstractPlatformTransactionManager {
        private final EventLog log;

        RecordingTransactionManager(EventLog log) {
            this.log = log;
        }

        @Override
        protected Object doGetTransaction() {
            return new Object();
        }

        @Override
        protected void doBegin(Object transaction, TransactionDefinition definition) {
            log.record("begin");
        }

        @Override
        protected void doCommit(DefaultTransactionStatus status) {
            log.record("commit");
        }

        @Override
        protected void doRollback(DefaultTransactionStatus status) {
            log.record("rollback");
        }
    }

    @Configuration(proxyBeanMethods = false)
    @EnableTransactionManagement(proxyTargetClass = true)
    static class ProxiedTransactionConfiguration {
    }
}
