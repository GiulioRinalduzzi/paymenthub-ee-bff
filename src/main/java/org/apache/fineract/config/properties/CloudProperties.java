package org.apache.fineract.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.boot.context.properties.bind.Name;

/**
 * The object storage this service uploads batch result files to.
 */
@ConfigurationProperties(prefix = "cloud")
public record CloudProperties(@DefaultValue Aws aws, @DefaultValue Azure azure) {

    public record Aws(@DefaultValue("false") boolean enabled, @DefaultValue Credentials credentials,
            @DefaultValue Region region, @DefaultValue("") String s3BaseUrl, @DefaultValue("") String minioPublicHost) {}

    public record Credentials(@DefaultValue("") String accessKey, @DefaultValue("") String secretKey) {}

    public record Region(@Name("static") @DefaultValue("") String staticRegion) {}

    public record Azure(@DefaultValue("false") boolean enabled, @DefaultValue Blob blob) {}

    public record Blob(@DefaultValue("") String connectionString) {}
}
