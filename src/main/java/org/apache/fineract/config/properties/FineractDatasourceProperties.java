package org.apache.fineract.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Where the tenant databases live. Read in two places before this record existed:
 * DataSourcePerTenantService, which builds a Hikari pool per tenant, and TenantDatabaseUpgradeService,
 * which runs Flyway over them.
 */
@ConfigurationProperties(prefix = "fineract.datasource")
public record FineractDatasourceProperties(@DefaultValue Core core, @DefaultValue Common common) {

    public record Core(@DefaultValue("operations-mysql") String host, @DefaultValue("3306") int port,
            @DefaultValue("tenants") String schema, @DefaultValue("root") String username,
            @DefaultValue("mysql") String password) {}

    /**
     * driverclass_name keeps its underscore: it is the name in application.yml and relaxed binding
     * matches it to driverclassName. FineractDatasourcePropertiesTest pins that down.
     */
    public record Common(@DefaultValue("jdbc") String protocol, @DefaultValue("mysql") String subprotocol,
            @DefaultValue("com.mysql.cj.jdbc.Driver") String driverclassName) {}
}
