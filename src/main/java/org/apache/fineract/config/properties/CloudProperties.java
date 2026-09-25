package org.apache.fineract.config.properties;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.Name;
import org.springframework.validation.annotation.Validated;

/**
 * The object storage this service uploads batch result files to.
 *
 * <p>
 * {@code cloud.aws.enabled} and {@code cloud.azure.enabled} are not here: they are read by the
 * {@code @ConditionalOnProperty} on the storage beans, where a missing key means off, not an error.
 * </p>
 */
@Validated
@ConfigurationProperties(prefix = "cloud")
public record CloudProperties(@NotNull @Valid Aws aws, @NotNull @Valid Azure azure) {
    public record Aws(@NotNull @Valid Credentials credentials,
            @NotNull @Valid Region region, @NotNull String s3BaseUrl, @NotNull String minioPublicHost) {}
    public record Credentials(@NotNull String accessKey, @NotNull String secretKey) {}
    public record Region(@Name("static") @NotNull String staticRegion) {}
    public record Azure(@NotNull @Valid Blob blob) {}
    public record Blob(@NotNull String connectionString) {}
}
