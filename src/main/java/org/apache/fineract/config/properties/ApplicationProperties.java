package org.apache.fineract.config.properties;

import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * The bucket batch result files are written to. The deployment sets it as APPLICATION_BUCKET-NAME,
 * an environment variable with a dash in the middle, which is why DeploymentEnvironmentBindingTest binds it
 * from that exact name rather than from a dotted property.
 */
@Validated
@ConfigurationProperties(prefix = "application")
public record ApplicationProperties(@NotNull String bucketName) {}
