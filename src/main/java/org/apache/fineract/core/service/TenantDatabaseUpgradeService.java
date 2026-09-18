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

import org.flywaydb.core.Flyway;
import org.apache.fineract.organisation.tenant.TenantServerConnection;
import org.apache.fineract.organisation.tenant.TenantServerConnectionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.apache.fineract.config.properties.FineractDatasourceProperties;
import org.apache.fineract.config.properties.TokenProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import javax.sql.DataSource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.apache.fineract.config.ResourceServerConfig.IDENTITY_PROVIDER_RESOURCE_ID;

@Service
public class TenantDatabaseUpgradeService {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Autowired
    private TenantServerConnectionRepository repository;

    @Autowired
    private DataSourcePerTenantService dataSourcePerTenantService;

    @Autowired
    private FineractDatasourceProperties datasourceProperties;

    @Autowired
    private TokenProperties tokenProperties;

    @Value("#{'${tenants}'.split(',')}")
    private List<String> tenants;

    @PostConstruct
    public void setupEnvironment() {
        flywayDefaultSchema();
        insertTenants();
        flywayTenants();
    }

    private void flywayTenants() {
        for (TenantServerConnection tenant : repository.findAll()) {
            if (tenant.isAutoUpdateEnabled()) {
                try {
                    ThreadLocalContextUtil.setTenant(tenant);
                    Map<String, String> placeholders = new HashMap<>();
                    placeholders.put("tenantDatabase", tenant.getSchemaName()); // add tenant as aud claim
                    placeholders.put("userAccessTokenValidity", String.valueOf(tokenProperties.user().accessValiditySeconds()));
                    placeholders.put("userRefreshTokenValidity", String.valueOf(tokenProperties.user().refreshValiditySeconds()));
                    placeholders.put("clientAccessTokenValidity", String.valueOf(tokenProperties.client().accessValiditySeconds()));
                    placeholders.put("channelClientSecret", tokenProperties.client().channel().secret());
                    placeholders.put("identityProviderResourceId", IDENTITY_PROVIDER_RESOURCE_ID); // add identity provider as aud claim
                    // Flyway moved to a fluent configure() API; baselineOnMigrate is the new name of initOnMigrate.
                    // The Flyway 2.x history table (schema_version) is converted to the Flyway 10 one
                    // (flyway_schema_history) first: Flyway 10 can read the old table but cannot write to it.
                    DataSource tenantDataSource = dataSourcePerTenantService.retrieveDataSource();
                    boolean historyConverted = FlywayHistoryTableUpgrade.upgradeIfNeeded(tenantDataSource);
                    final Flyway fw = Flyway.configure()
                            .dataSource(tenantDataSource)
                            .locations("sql/migrations/tenant")
                            .baselineOnMigrate(true)
                            .outOfOrder(true)
                            .placeholders(placeholders)
                            .load();
                    // only after a conversion: the history written by Flyway 2.x holds checksums
                    // computed with the old algorithm and Flyway 10 would fail validation on them,
                    // which repair() re-computes. That is a one-time condition, so the call is
                    // gated: repair() also marks as DELETED any history row whose script is no
                    // longer on disk, and running it on every restart would keep rewriting the
                    // history of a schema carrying migrations this repo does not ship.
                    if (historyConverted) {
                        fw.repair();
                    }
                    fw.migrate();
                } catch (Exception e) {
                    logger.error("Error when running flyway on tenant: {}", tenant.getSchemaName(), e);
                } finally {
                    ThreadLocalContextUtil.clear();
                }
            }
        }
    }

    private void insertTenants() {
        for(String tenant : tenants) {
            TenantServerConnection existingTenant = repository.findOneBySchemaName(tenant);
            if(existingTenant == null) {
                TenantServerConnection tenantServerConnection = new TenantServerConnection();
                tenantServerConnection.setSchemaName(tenant);
                tenantServerConnection.setSchemaServer(datasourceProperties.core().host());
                tenantServerConnection.setSchemaServerPort(String.valueOf(datasourceProperties.core().port()));
                tenantServerConnection.setSchemaUsername(datasourceProperties.core().username());
                tenantServerConnection.setSchemaPassword(datasourceProperties.core().password());
                tenantServerConnection.setAutoUpdateEnabled(true);
                repository.saveAndFlush(tenantServerConnection);
            }
        }
    }

    private void flywayDefaultSchema() {
        DataSource coreDataSource = dataSourcePerTenantService.retrieveDataSource();
        // convert the Flyway 2.x history table if this database has one (see flywayTenants)
        boolean historyConverted = FlywayHistoryTableUpgrade.upgradeIfNeeded(coreDataSource);
        final Flyway fw = Flyway.configure()
                .dataSource(coreDataSource)
                .locations("sql/migrations/core")
                .baselineOnMigrate(true)
                .outOfOrder(true)
                .load();
        // gated for the same reason as in flywayTenants
        if (historyConverted) {
            fw.repair();
        }
        fw.migrate();
    }
}
