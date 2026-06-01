package com.hackerprank.persistence;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.OffsetDateTime;

public final class JdbcInstant {
    private JdbcInstant() {
    }

    public static Instant from(ResultSet rs, String columnName) throws SQLException {
        Object value = rs.getObject(columnName);
        if (value instanceof OffsetDateTime offsetDateTime) {
            return offsetDateTime.toInstant();
        }
        if (value instanceof Timestamp timestamp) {
            return timestamp.toInstant();
        }
        if (value instanceof Instant instant) {
            return instant;
        }
        throw new SQLException("Unsupported timestamp value for " + columnName + ": " + value);
    }
}
