package com.naturalist.persistence.test;

import com.naturalist.persistence.MyBatisSupport;
import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

/**
 * Binds one MyBatis {@link SqlSession} per test (autocommit off) and rolls it back after,
 * so the seeded standing DB stays pristine. Assumes the DB is already seeded by
 * {@code apps/test-db-seeder}; this extension performs NO schema or seed work.
 */
public final class RdbmsTestExtension implements BeforeEachCallback, AfterEachCallback {

    private static final SqlSessionFactory FACTORY =
            MyBatisSupport.sessionFactory(RdbmsDataSource.shared());

    private final ThreadLocal<SqlSession> current = new ThreadLocal<>();

    private RdbmsTestExtension() {}

    public static RdbmsTestExtension shared() {
        return new RdbmsTestExtension();
    }

    @Override
    public void beforeEach(ExtensionContext context) {
        current.set(FACTORY.openSession(false)); // autocommit off
    }

    @Override
    public void afterEach(ExtensionContext context) {
        SqlSession session = current.get();
        if (session != null) {
            try {
                session.rollback();
            } finally {
                session.close();
                current.remove();
            }
        }
    }

    /** Returns a mapper bound to the current test's rolled-back transaction. */
    public <M> M mapper(Class<M> mapperType) {
        synchronized (FACTORY) {
            if (!FACTORY.getConfiguration().hasMapper(mapperType)) {
                FACTORY.getConfiguration().addMapper(mapperType);
            }
        }
        SqlSession session = current.get();
        if (session == null) throw new IllegalStateException("No active test session; use @RegisterExtension");
        return session.getMapper(mapperType);
    }
}
