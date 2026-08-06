package org.apache.fineract.config;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import org.apache.fineract.core.service.AudienceVerifier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.expression.WebExpressionAuthorizationManager;

import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.List;

/**
 * Resource-server half of the old Spring Security OAuth2 setup, rebuilt on
 * Spring Security 6. The old stack (@EnableResourceServer +
 * ResourceServerConfigurerAdapter, spring-security-oauth2 2.4.1) was
 * discontinued and does not exist for Spring Boot 3.
 *
 * Behavior kept from the old ResourceServerConfig:
 * - stateless sessions, everything (csrf/cors/form/basic/rememberMe/x509/jee) disabled
 * - rest.authorization.enabled = false -> /api/v1/** is open
 * - rest.authorization.enabled = true  -> per-endpoint SpEL rules from
 *   rest.authorization.settings, everything else fully authenticated
 * - JWTs are verified with the same RSA public key (jwt_pub.pem), the same
 *   "identity-provider" resource id check and the same per-tenant audience
 *   check (AudienceVerifier)
 * - the "authorities" claim of the token becomes the granted authorities,
 *   with no prefix (same as the old JwtAccessTokenConverter contract)
 */
@Configuration
@EnableWebSecurity
public class ResourceServerConfig {

    public static final String IDENTITY_PROVIDER_RESOURCE_ID = "identity-provider";

    @Autowired
    private AuthProperties authProperties;

    @Autowired
    private InvalidAuthEntryPoint invalidAuthEntryPoint;

    @Value("${rest.authorization.enabled}")
    private boolean isRestAuthEnabled;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, JwtDecoder jwtDecoder) throws Exception {
        http.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .csrf(AbstractHttpConfigurer::disable)
                .cors(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .rememberMe(AbstractHttpConfigurer::disable)
                .x509(AbstractHttpConfigurer::disable)
                .jee(AbstractHttpConfigurer::disable);

        if (!isRestAuthEnabled) {
            // .anonymous() stays enabled here, exactly like the old config:
            // without it every request fails with 401 regardless of permissions
            //
            // anyRequest().permitAll() is not a relaxation, it keeps the old
            // behaviour: the old config listed only /api/v1/** and had no
            // anyRequest() rule at all, and a request matching no rule was let
            // through. Anything stricter here breaks the kubernetes probes on
            // /actuator/health/liveness and /actuator/health/readiness, which
            // never carry a token.
            http.authorizeHttpRequests(auth -> auth
                    .requestMatchers("/oauth/token", "/oauth/token_key").permitAll()
                    // check_token was isAuthenticated() in the old AuthorizationServerConfig,
                    // and that chain was separate from this one, so it did not follow
                    // rest.authorization.enabled. Keep it closed here too.
                    .requestMatchers("/oauth/check_token").authenticated()
                    .requestMatchers("/api/v1/**").permitAll()
                    .anyRequest().permitAll());
        } else {
            http.anonymous(AbstractHttpConfigurer::disable);
            http.authorizeHttpRequests(auth -> {
                // token_key was permitAll in the old AuthorizationServerConfig; check_token was
                // isAuthenticated(), which the anyRequest().fullyAuthenticated() below covers
                auth.requestMatchers("/oauth/token", "/oauth/token_key").permitAll();
                List<EndpointSetting> settings = authProperties.getSettings();
                if (settings.isEmpty()) {
                    throw new RuntimeException("Configuration property rest.authorization.settings can not be empty!");
                }
                for (EndpointSetting setting : settings) {
                    // the old .access(String) SpEL contract is kept via WebExpressionAuthorizationManager
                    auth.requestMatchers(setting.getEndpoint()).access(new WebExpressionAuthorizationManager(setting.getAuthority()));
                }
                auth.anyRequest().fullyAuthenticated();
            });
        }

        http.oauth2ResourceServer(rs -> rs
                .authenticationEntryPoint(invalidAuthEntryPoint)
                .jwt(jwt -> jwt.decoder(jwtDecoder).jwtAuthenticationConverter(jwtAuthenticationConverter())));

        return http.build();
    }

    private JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter authoritiesConverter = new JwtGrantedAuthoritiesConverter();
        authoritiesConverter.setAuthoritiesClaimName("authorities");
        authoritiesConverter.setAuthorityPrefix("");
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(authoritiesConverter);
        return converter;
    }

    @Bean
    public JwtDecoder jwtDecoder(AudienceVerifier audienceVerifier) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withPublicKey(PemUtils.readPublicKey("jwt_pub.pem")).build();
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefault(), resourceIdValidator(), audienceVerifier));
        return decoder;
    }

    /**
     * Replaces the resourceId(IDENTITY_PROVIDER_RESOURCE_ID) call of the old
     * ResourceServerSecurityConfigurer. The old stack checked this in
     * OAuth2AuthenticationManager, with the same rule kept here: a token is
     * rejected only if it carries an audience list that does not contain
     * "identity-provider". A token with no audience at all was accepted before
     * and still is.
     */
    private static OAuth2TokenValidator<Jwt> resourceIdValidator() {
        return jwt -> {
            List<String> audiences = jwt.getAudience();
            if (audiences == null || audiences.isEmpty() || audiences.contains(IDENTITY_PROVIDER_RESOURCE_ID)) {
                return OAuth2TokenValidatorResult.success();
            }
            String message = "Token audiences " + audiences + " do not contain the resource id " + IDENTITY_PROVIDER_RESOURCE_ID;
            return OAuth2TokenValidatorResult.failure(new OAuth2Error(OAuth2ErrorCodes.INVALID_TOKEN, message, null));
        };
    }

    @Bean
    public JwtEncoder jwtEncoder() {
        RSAPublicKey publicKey = PemUtils.readPublicKey("jwt_pub.pem");
        RSAPrivateKey privateKey = PemUtils.readPrivateKey("jwt.pem");
        RSAKey key = new RSAKey.Builder(publicKey).privateKey(privateKey).build();
        return new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(key)));
    }
}
