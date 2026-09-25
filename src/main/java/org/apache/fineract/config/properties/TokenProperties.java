package org.apache.fineract.config.properties;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Token validities, and the secret of the machine client.
 *
 * <p>
 * All four values end up as Flyway placeholders in the tenant migrations, so they are written into
 * the oauth_client_details rows the token endpoint reads later.
 * </p>
 */
@Validated
@ConfigurationProperties(prefix = "token")
public record TokenProperties(@NotNull @Valid User user, @NotNull @Valid Client client) {
    public record User(@NotNull Integer accessValiditySeconds, @NotNull Integer refreshValiditySeconds) {}
    public record Client(@NotNull Integer accessValiditySeconds, @NotNull @Valid Channel channel) {}
    public record Channel(@NotNull String secret) {}
}
