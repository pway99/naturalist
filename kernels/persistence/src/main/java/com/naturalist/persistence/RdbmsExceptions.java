package com.naturalist.persistence;

import java.sql.SQLException;

/** Translates driver/SQLState signals into decisions the adapter maps to domain exceptions. */
public final class RdbmsExceptions {

    private RdbmsExceptions() {}

    private static final String UNIQUE_VIOLATION = "23505"; // Postgres

    /** True if {@code t}'s cause chain carries a SQLState {@code 23505} (unique/PK violation). */
    public static boolean isUniqueViolation(Throwable t) {
        for (Throwable c = t; c != null; c = c.getCause()) {
            if (c instanceof SQLException sql && UNIQUE_VIOLATION.equals(sql.getSQLState())) {
                return true;
            }
        }
        return false;
    }
}
