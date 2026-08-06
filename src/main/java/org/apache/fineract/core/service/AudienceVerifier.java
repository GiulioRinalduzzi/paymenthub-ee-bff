package org.apache.fineract.core.service;

import org.apache.fineract.organisation.tenant.TenantServerConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Same check as before, new interface: the token's "aud" list must contain the
 * schema name of the tenant the request is for. The old JwtClaimsSetVerifier
 * interface belonged to the discontinued Spring Security OAuth2 stack; in
 * Spring Security 6 the same hook is an OAuth2TokenValidator plugged into the
 * JwtDecoder (see ResourceServerConfig).
 */
@Component
public class AudienceVerifier implements OAuth2TokenValidator<Jwt> {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Override
    public OAuth2TokenValidatorResult validate(Jwt jwt) {
        TenantServerConnection tenant = ThreadLocalContextUtil.getTenant();
        List<String> audiences = jwt.getAudience();
        if (tenant == null) {
            // No tenant means TenantAwareHeaderFilter let the request through without
            // resolving one, so there is nothing to compare the audience against and the
            // token cannot be accepted for this request. Before this the line below
            // dereferenced null and the caller got a 500 instead of a 401. The paths
            // that legitimately run without a tenant do not reach this validator at all -
            // they are on the chain with no resource server, see ResourceServerConfig -
            // so this is the safety net for anything else that ever skips the filter.
            String message = "No tenant in context, cannot verify the token audience";
            logger.error(message);
            return OAuth2TokenValidatorResult.failure(new OAuth2Error(OAuth2ErrorCodes.INVALID_TOKEN, message, null));
        }
        boolean matches = audiences != null && audiences.stream().anyMatch(a -> tenant.getSchemaName().equals(a));
        if (matches) {
            return OAuth2TokenValidatorResult.success();
        }
        String message = "Token audiences " + audiences + " are not matching with request " + tenant.getSchemaName();
        logger.error(message);
        return OAuth2TokenValidatorResult.failure(new OAuth2Error(OAuth2ErrorCodes.INVALID_TOKEN, message, null));
    }
}
