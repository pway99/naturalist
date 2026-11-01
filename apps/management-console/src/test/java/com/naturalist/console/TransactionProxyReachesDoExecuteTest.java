package com.naturalist.console;

import com.naturalist.data.Transaction;
import com.naturalist.ddd.Aggregate;
import com.naturalist.observability.Constraints;
import org.junit.jupiter.api.Test;
import org.springframework.aop.support.AopUtils;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Regression for the production NPE in {@link Transaction#execute}: the deployed app has a
 * {@code DataSource}, so Spring Boot registers a transaction manager and every
 * {@code @DomainService} {@code Transaction} bean is CGLIB-proxied for its
 * {@code @Transactional execute}. While {@code execute} was {@code final}, CGLIB could not
 * override it, so the call ran on the Objenesis-built proxy instance, whose {@code observer}
 * field is null — a NullPointerException before {@code doExecute} ran, logged only as
 * {@code CglibAopProxy: Public final method [...] cannot get proxied via CGLIB}.
 *
 * <p>No {@code @SpringBootTest} here sees it: app test contexts run without a
 * {@code DataSource} ({@code MockPersistenceWiringTest#contextHasNoDataSource}), so no
 * transaction proxy is ever made. This test builds its own small context — not a Boot
 * context — with Spring's real transaction advice ({@code @EnableTransactionManagement},
 * class-based proxies as Boot defaults to) and a recording stub
 * {@code PlatformTransactionManager}, and proves the proxied {@code execute} reaches
 * {@code doExecute} inside a begun-and-committed transaction.
 */
class TransactionProxyReachesDoExecuteTest {

    @Test
    void proxiedExecuteRunsDoExecuteInsideABegunAndCommittedTransaction() {
        try (var context = new AnnotationConfigApplicationContext(ProxiedTransactionConfiguration.class)) {
            RecordingTransaction transaction = context.getBean(RecordingTransaction.class);
            TransactionEventLog log = context.getBean(TransactionEventLog.class);

            assertThat(AopUtils.isCglibProxy(transaction))
                    .as("the Transaction bean must be CGLIB-proxied, as it is in the deployed app")
                    .isTrue();

            transaction.execute(new Batch("moth"));

            assertThat(log.events())
                    .as("execute must reach doExecute, between the manager's begin and commit")
                    .containsExactly("begin", "doExecute:moth", "commit");
        }
    }

    record Batch(String label) implements Aggregate {
        @Override
        public Consumer<? extends Constraints> invariants() {
            return c -> c.notBlank(label, "label");
        }
    }

    /** Shared by the manager and the transaction, so the order of their events is observable. */
    static class TransactionEventLog {
        private final List<String> events = new ArrayList<>();

        void record(String event) {
            events.add(event);
        }

        List<String> events() {
            return List.copyOf(events);
        }
    }

    /** A minimal Transaction subclass; non-final so CGLIB can subclass it, as it does the real beans. */
    static class RecordingTransaction extends Transaction<Batch> {
        private final TransactionEventLog log;

        RecordingTransaction(TransactionEventLog log) {
            this.log = log;
        }

        @Override
        protected void doExecute(Batch batch) {
            log.record("doExecute:" + batch.label());
        }
    }

    /** Stub PlatformTransactionManager that records begin / commit / rollback. */
    static class RecordingTransactionManager extends AbstractPlatformTransactionManager {
        private final TransactionEventLog log;

        RecordingTransactionManager(TransactionEventLog log) {
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
        TransactionEventLog eventLog() {
            return new TransactionEventLog();
        }

        @Bean
        RecordingTransactionManager transactionManager(TransactionEventLog log) {
            return new RecordingTransactionManager(log);
        }

        @Bean
        RecordingTransaction recordingTransaction(TransactionEventLog log) {
            return new RecordingTransaction(log);
        }
    }
}
