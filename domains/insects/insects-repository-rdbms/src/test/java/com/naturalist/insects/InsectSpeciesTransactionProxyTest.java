package com.naturalist.insects;

import com.naturalist.fieldnotes.Description;
import com.naturalist.taxonomy.TaxonomicSpecies;
import org.junit.jupiter.api.Test;
import org.springframework.aop.support.AopUtils;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Proves {@link InsectSpeciesTransaction}'s writes run inside one transaction once Spring proxies
 * the bean, as it does in the deployed app (Boot registers a transaction manager and class-based
 * proxies). The {@code *IT}s cannot show this: they construct the bean by hand on a rolled-back
 * {@code SqlSession}, so no proxy exists there. This builds a small context — not a Boot one —
 * with Spring's real transaction advice, a recording stub {@code PlatformTransactionManager} and a
 * recording mapper, and checks that every statement of a write lands between one begin and one
 * commit, or one rollback when a statement fails.
 */
class InsectSpeciesTransactionProxyTest {

    @Test
    void proxiedInsertRunsEveryStatementInsideOneCommittedTransaction() {
        try (var context = new AnnotationConfigApplicationContext(ProxiedTransactionConfiguration.class)) {
            InsectSpeciesTransaction transaction = context.getBean(InsectSpeciesTransaction.class);
            EventLog log = context.getBean(EventLog.class);

            assertThat(AopUtils.isCglibProxy(transaction))
                    .as("the bean must be CGLIB-proxied, as it is in the deployed app")
                    .isTrue();

            transaction.insert(species());

            assertThat(log.events()).containsExactly("begin", "insert", "commit");
        }
    }

    @Test
    void proxiedUpdateRollsBackEveryStatementWhenOneFails() {
        try (var context = new AnnotationConfigApplicationContext(ProxiedTransactionConfiguration.class)) {
            InsectSpeciesTransaction transaction = context.getBean(InsectSpeciesTransaction.class);
            EventLog log = context.getBean(EventLog.class);
            log.failOn("deleteCommonNames");

            assertThatThrownBy(() -> transaction.update(species())).isInstanceOf(IllegalStateException.class);

            assertThat(log.events())
                    .as("the committed-before-failure parent update must be rolled back with the rest")
                    .containsExactly("begin", "updateByName", "deleteCommonNames", "rollback");
        }
    }

    private static InsectSpeciesInfo species() {
        return new InsectSpeciesInfo(
                InsectSpeciesName.of("test-species-xx"),
                InsectGenusName.of("empoasca"),
                TaxonomicSpecies.of("nemoralis"),
                new Description("brief", "short", "long", "full"),
                Set.of(),
                null,
                null,
                null, null, null, null,
                null, null, null);
    }

    /** Shared by the manager and the mapper, so the order of their events is observable. */
    static class EventLog {
        private final List<String> events = new ArrayList<>();
        private String failingStatement;

        void record(String event) {
            events.add(event);
        }

        void failOn(String statement) {
            failingStatement = statement;
        }

        boolean fails(String statement) {
            return statement.equals(failingStatement);
        }

        List<String> events() {
            return List.copyOf(events);
        }
    }

    /** A mapper whose every statement records its name, affects one row, and can be made to fail. */
    private static InsectSpeciesMapper recordingMapper(EventLog log) {
        return (InsectSpeciesMapper) Proxy.newProxyInstance(
                InsectSpeciesMapper.class.getClassLoader(),
                new Class<?>[] {InsectSpeciesMapper.class},
                (proxy, method, args) -> {
                    if (method.getDeclaringClass() == Object.class) return method.invoke(log, args);
                    log.record(method.getName());
                    if (log.fails(method.getName())) throw new IllegalStateException(method.getName() + " failed");
                    return method.getReturnType() == int.class ? 1 : null;
                });
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
        @Bean
        EventLog eventLog() {
            return new EventLog();
        }

        @Bean
        RecordingTransactionManager transactionManager(EventLog log) {
            return new RecordingTransactionManager(log);
        }

        @Bean
        InsectSpeciesTransaction insectSpeciesTransaction(EventLog log) {
            return new InsectSpeciesTransaction(recordingMapper(log));
        }
    }
}
