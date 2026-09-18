package org.apache.fineract.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * The bucket batch result files are written to. The deployment sets it as APPLICATION_BUCKET-NAME,
 * an environment variable with a dash in the middle, which is why ApplicationPropertiesTest binds it
 * from that exact name rather than from a dotted property.
 */
@ConfigurationProperties(prefix = "application")
public record ApplicationProperties(@DefaultValue("paymenthub-ee-dev") String bucketName) {}
