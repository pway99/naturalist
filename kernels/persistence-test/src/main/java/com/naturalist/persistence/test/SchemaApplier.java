package com.naturalist.persistence.test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/** Runs a `.sql` classpath resource. The seam a DDL generator or Flyway swaps in later. */
public final class SchemaApplier {

    private SchemaApplier() {}

    public static void applyResource(Connection connection, String classpathResource) {
        String sql = read(classpathResource);
        try (Statement statement = connection.createStatement()) {
            for (String stmt : sql.split(";")) {
                if (!stmt.isBlank()) {
                    statement.execute(stmt);
                }
            }
            connection.commit();
        } catch (SQLException e) {
            throw new IllegalStateException("Failed applying schema " + classpathResource, e);
        }
    }

    private static String read(String classpathResource) {
        try (InputStream in = SchemaApplier.class.getClassLoader().getResourceAsStream(classpathResource)) {
            if (in == null) throw new IllegalStateException("Schema resource not found: " + classpathResource);
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Failed reading schema " + classpathResource, e);
        }
    }
}
