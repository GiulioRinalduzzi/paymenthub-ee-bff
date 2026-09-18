package org.apache.fineract.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Token validities, and the secret of the machine client.
 *
 * <p>
 * All four values end up as Flyway placeholders in the tenant migrations, so they are written into
 * the oauth_client_details rows the token endpoint reads later.
 * </p>
 */
@ConfigurationProperties(prefix = "token")
public record TokenProperties(@DefaultValue User user, @DefaultValue Client client) {

    public record User(@DefaultValue("600") int accessValiditySeconds, @DefaultValue("43200") int refreshValiditySeconds) {}

    public record Client(@DefaultValue("3600") int accessValiditySeconds, @DefaultValue Channel channel) {}

    public record Channel(@DefaultValue("") String secret) {}
}
