package org.apache.fineract.config.properties;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Where the tenant databases live. Read in two places before this record existed:
 * DataSourcePerTenantService, which builds a Hikari pool per tenant, and TenantDatabaseUpgradeService,
 * which runs Flyway over them.
 */
@Validated
@ConfigurationProperties(prefix = "fineract.datasource")
public record FineractDatasourceProperties(@NotNull @Valid Core core, @NotNull @Valid Common common) {

    public record Core(@NotNull String host, @NotNull Integer port, @NotNull String schema, @NotNull String username,
            @NotNull String password) {}

    /**
     * driverclass_name keeps its underscore: it is the name in application.yml and relaxed binding
     * matches it to driverclassName. DeploymentEnvironmentBindingTest pins that down.
     */
    public record Common(@NotNull String protocol, @NotNull String subprotocol, @NotNull String driverclassName) {}
}
