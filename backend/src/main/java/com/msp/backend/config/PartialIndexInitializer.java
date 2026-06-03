package com.msp.backend.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Drops global unique constraints on users(email) and users(display_name)
 * and replaces them with partial unique indexes that only apply to non-deleted rows.
 * This runs after Hibernate ddl-auto=update so the table/columns already exist.
 */
@Component
@Order(1)
public class PartialIndexInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(PartialIndexInitializer.class);

    private final JdbcTemplate jdbc;

    public PartialIndexInitializer(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public void run(ApplicationArguments args) {
        // Drop old global unique constraints (name may vary; try both common names)
        dropConstraintIfExists("users", "uk6dotkott2kjsp8vw4d0m25fb7"); // email constraint
        dropConstraintIfExists("users", "ukmwwhlk3getlkf1a3655oyfh34"); // display_name constraint
        // Also try Hibernate-generated names based on column name
        dropConstraintIfExists("users", "users_email_key");
        dropConstraintIfExists("users", "users_display_name_key");

        // Create partial unique index on email (only for non-deleted rows)
        jdbc.execute("""
            CREATE UNIQUE INDEX IF NOT EXISTS uidx_users_email_active
            ON users (email)
            WHERE deleted_at IS NULL
        """);

        // Create partial unique index on display_name (only for non-deleted rows, ignoring NULLs)
        jdbc.execute("""
            CREATE UNIQUE INDEX IF NOT EXISTS uidx_users_display_name_active
            ON users (LOWER(display_name))
            WHERE deleted_at IS NULL AND display_name IS NOT NULL
        """);

        log.info("Partial unique indexes on users(email, display_name) ensured.");
    }

    private void dropConstraintIfExists(String table, String constraint) {
        try {
            Integer count = jdbc.queryForObject(
                "SELECT COUNT(*) FROM information_schema.table_constraints " +
                "WHERE table_name = ? AND constraint_name = ?",
                Integer.class, table, constraint
            );
            if (count != null && count > 0) {
                jdbc.execute("ALTER TABLE " + table + " DROP CONSTRAINT " + constraint);
                log.info("Dropped constraint: {}", constraint);
            }
        } catch (Exception e) {
            log.debug("Could not drop constraint {} (may not exist): {}", constraint, e.getMessage());
        }
    }
}
