package com.naturalist.persistence.test;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import javax.sql.DataSource;

/** Process-wide test DataSource, configured from env with localhost defaults. */
public final class RdbmsDataSource {

    private RdbmsDataSource() {}

    private static volatile DataSource instance;

    public static DataSource shared() {
        DataSource local = instance;
        if (local == null) {
            synchronized (RdbmsDataSource.class) {
                local = instance;
                if (local == null) {
                    instance = local = build();
                }
            }
        }
        return local;
    }

    private static DataSource build() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(env("NATURALIST_TEST_JDBC_URL", "jdbc:postgresql://localhost:5432/naturalist_test"));
        config.setUsername(env("NATURALIST_TEST_DB_USER", "postgres"));
        config.setPassword(env("NATURALIST_TEST_DB_PASSWORD", "postgres"));
        config.setMaximumPoolSize(4);
        config.setPoolName("naturalist-test");
        return new HikariDataSource(config);
    }

    private static String env(String key, String dflt) {
        String v = System.getenv(key);
        return (v == null || v.isBlank()) ? dflt : v;
    }
}
