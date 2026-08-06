/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.fineract.core.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Moves a Flyway 2.x history table to the layout Flyway 10 expects.
 *
 * The old app ran Flyway 2.x, which kept its history in a table called
 * "schema_version". Flyway 10 uses "flyway_schema_history" and a different
 * layout: "installed_rank" is the primary key, the old "version_rank" column is
 * gone, "description" is NOT NULL and "version" is nullable. Flyway 10 can read
 * the old table but cannot insert into it, so the first pending migration on an
 * existing database fails. Flyway used to do this conversion by itself (up to
 * Flyway 4), that code was removed, so we do it here before calling migrate().
 *
 * The old table is kept under the name "schema_version_flyway2_backup" instead
 * of being dropped, so a failed upgrade can be inspected.
 *
 * Does nothing on a fresh database, and nothing if it has already run.
 *
 * How a failure here surfaces depends on which schema it happens in, and the two
 * are not the same. TenantDatabaseUpgradeService.flywayDefaultSchema() lets the
 * exception escape, so a problem in the core schema stops startup. Its
 * flywayTenants() loop catches Exception per tenant and only logs it, so on a
 * tenant schema the "interrupted conversion, restore from the backup" guard below
 * degrades to one ERROR line: the context starts, the readiness probe goes green
 * and that tenant serves traffic against a schema whose migration state is
 * unknown. The catch predates this class and widening it is a startup-behaviour
 * decision for a multi-tenant deployment, not something to change inside a
 * migration - but anyone reading the guard should know it can be swallowed.
 */
final class FlywayHistoryTableUpgrade {

    private static final Logger logger = LoggerFactory.getLogger(FlywayHistoryTableUpgrade.class);

    private static final String LEGACY_TABLE = "schema_version";
    private static final String CURRENT_TABLE = "flyway_schema_history";
    private static final String BACKUP_TABLE = "schema_version_flyway2_backup";

    private FlywayHistoryTableUpgrade() {
    }

    /**
     * @return true if this call moved a history table written by an older Flyway,
     *         which is the only situation where the caller needs Flyway.repair():
     *         those rows carry checksums the current version will not validate.
     *         False when there was nothing to do, so repair() can be skipped and
     *         does not get a chance to mark rows DELETED on every restart.
     */
    static boolean upgradeIfNeeded(DataSource dataSource) {
        try (Connection connection = dataSource.getConnection()) {
            String schema = connection.getCatalog();
            if (tableExists(connection, schema, CURRENT_TABLE)) {
                return false; // already on the Flyway 10 layout
            }
            if (!tableExists(connection, schema, LEGACY_TABLE)) {
                if (tableExists(connection, schema, BACKUP_TABLE)) {
                    // the backup exists but neither history table does, so a previous
                    // conversion was interrupted after the rename. Returning here would
                    // look like a fresh database to Flyway, which would then baseline and
                    // re-apply every migration on a schema that already has the objects.
                    // Fail loudly instead: the history is recoverable from the backup.
                    throw new IllegalStateException("Schema " + schema + " has " + BACKUP_TABLE + " but no "
                            + CURRENT_TABLE + " and no " + LEGACY_TABLE + ": a previous history conversion was "
                            + "interrupted. Restore the history from " + BACKUP_TABLE + " before starting again.");
                }
                return false; // fresh database, Flyway will create its own table
            }
            if (columnExists(connection, schema, LEGACY_TABLE, "version_rank")) {
                // the root log level is ERROR, so application.yml raises this class to INFO:
                // a one-off rewrite of the history table has to be visible in the logs
                logger.info("Found a Flyway 2.x history table in schema {}, converting it to {}", schema, CURRENT_TABLE);
                convertLegacyTable(connection);
            } else {
                // "schema_version" written by a recent Flyway (a run of this app
                // that still passed .table("schema_version")): layout is already
                // right, only the name is old.
                logger.info("Renaming {} to {} in schema {}", LEGACY_TABLE, CURRENT_TABLE, schema);
                try (Statement statement = connection.createStatement()) {
                    statement.execute("RENAME TABLE " + LEGACY_TABLE + " TO " + CURRENT_TABLE);
                }
                commitIfNeeded(connection);
            }
            return true;
        } catch (SQLException e) {
            throw new IllegalStateException("Cannot upgrade the Flyway history table", e);
        }
    }

    private static void convertLegacyTable(Connection connection) throws SQLException {
        // the cleanup below must only remove a table this call created, and only
        // before the rename. If two instances start together they both get here, the
        // CREATE TABLE of the loser fails, and dropping on the way out would delete
        // the history the winner has just converted. After the rename there is
        // nothing safe to undo either: MySQL commits implicitly on DDL, so by then
        // the copy is already durable.
        boolean tableCreated = false;
        boolean legacyRenamed = false;
        try (Statement statement = connection.createStatement()) {
            try {
                // same DDL Flyway 10 itself uses for MySQL
                statement.execute("CREATE TABLE " + CURRENT_TABLE + " ("
                        + "installed_rank INT NOT NULL,"
                        + "version VARCHAR(50),"
                        + "description VARCHAR(200) NOT NULL,"
                        + "type VARCHAR(20) NOT NULL,"
                        + "script VARCHAR(1000) NOT NULL,"
                        + "checksum INT,"
                        + "installed_by VARCHAR(100) NOT NULL,"
                        + "installed_on TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,"
                        + "execution_time INT NOT NULL,"
                        + "success BOOL NOT NULL,"
                        + "CONSTRAINT " + CURRENT_TABLE + "_pk PRIMARY KEY (installed_rank)"
                        + ") ENGINE=InnoDB");
                tableCreated = true;
                statement.execute("CREATE INDEX " + CURRENT_TABLE + "_s_idx ON " + CURRENT_TABLE + " (success)");
                // description was nullable before and is NOT NULL now; "INIT" was
                // renamed to "BASELINE" when Flyway 5 came out
                statement.execute("INSERT INTO " + CURRENT_TABLE
                        + " (installed_rank, version, description, type, script, checksum,"
                        + " installed_by, installed_on, execution_time, success)"
                        + " SELECT installed_rank, version, COALESCE(description, ''),"
                        + " CASE WHEN type = 'INIT' THEN 'BASELINE' ELSE type END,"
                        + " script, checksum, installed_by, installed_on, execution_time, success"
                        + " FROM " + LEGACY_TABLE);
                statement.execute("RENAME TABLE " + LEGACY_TABLE + " TO " + BACKUP_TABLE);
                legacyRenamed = true;
                commitIfNeeded(connection);
            } catch (SQLException e) {
                // leave no half-built table behind: the next start must be able to retry
                if (tableCreated && !legacyRenamed) {
                    try {
                        if (!connection.getAutoCommit()) {
                            connection.rollback();
                        }
                        statement.execute("DROP TABLE IF EXISTS " + CURRENT_TABLE);
                        commitIfNeeded(connection);
                    } catch (SQLException cleanupFailure) {
                        e.addSuppressed(cleanupFailure);
                    }
                }
                throw e;
            }
        }
    }

    /**
     * The tenant datasources are built with autoCommit off (see
     * DataSourcePerTenantService), so the row copy has to be committed by hand.
     * MySQL commits on its own after each DDL statement, but not after the
     * INSERT, and the pool rolls back whatever is left open.
     */
    private static void commitIfNeeded(Connection connection) throws SQLException {
        if (!connection.getAutoCommit()) {
            connection.commit();
        }
    }

    // information_schema rather than DatabaseMetaData: getTables/getColumns take
    // LIKE patterns, and every name here contains an underscore, which is a
    // single-character wildcard in a pattern.
    private static boolean tableExists(Connection connection, String schema, String table) throws SQLException {
        return exists(connection, "SELECT 1 FROM information_schema.tables"
                + " WHERE table_schema = ? AND table_name = ?", schema, table);
    }

    private static boolean columnExists(Connection connection, String schema, String table, String column) throws SQLException {
        return exists(connection, "SELECT 1 FROM information_schema.columns"
                + " WHERE table_schema = ? AND table_name = ? AND column_name = ?", schema, table, column);
    }

    private static boolean exists(Connection connection, String sql, String... parameters) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            for (int i = 0; i < parameters.length; i++) {
                statement.setString(i + 1, parameters[i]);
            }
            try (ResultSet rows = statement.executeQuery()) {
                return rows.next();
            }
        }
    }
}
