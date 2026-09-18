/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.fineract;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.fineract.core.service.TenantAwareHeaderFilter;
import org.apache.fineract.organisation.tenant.TenantServerConnectionRepository;
import org.mifos.connector.common.interceptor.annotation.EnableJsonWebSignature;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.error.ErrorMvcAutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.ArrayList;
import java.util.List;

// org.mifos.g2pconnector comes from the former ph-ee-operations-g2p-service:
// its controllers and services live outside org.apache.fineract, so component
// scanning has to be told about them
@SpringBootApplication(scanBasePackages = { "org.apache.fineract", "org.mifos.g2pconnector" })
@EnableConfigurationProperties
@ConfigurationPropertiesScan("org.apache.fineract.config.properties")
@EnableAutoConfiguration(exclude = {DataSourceAutoConfiguration.class,
        DataSourceTransactionManagerAutoConfiguration.class,
        FlywayAutoConfiguration.class,
        ErrorMvcAutoConfiguration.class})
@EnableJsonWebSignature
public class ServerApplication {

    /**
     * Spring security filter chain ordering, the tenant header filter
     * must run before this to set current tenant so it's order has to be lower to gain priority
     */
    @Value("${security.filter-order}")
    private int securityFilterOrder;

    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder.build();
    }

    /**
     * The API used to be served by a plain new ObjectMapper(). Declaring any ObjectMapper bean makes
     * Spring Boot's Jackson auto-configuration back off, so that bean also decided how every request
     * body is parsed and every response written, and every spring.jackson.* property was inert.
     *
     * The one difference that is visible on the wire is FAIL_ON_UNKNOWN_PROPERTIES: a plain mapper
     * has it on, Boot's has it off, so a request body carrying an unknown field is a 400 today. That
     * is kept, explicitly, rather than relaxed as a side effect of this change - whether the API
     * should accept unknown fields is a decision for the team, not a refactor.
     */
    @Bean
    public ObjectMapper mapper(Jackson2ObjectMapperBuilder builder) {
        return builder.build().enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public DaoAuthenticationProvider customAuthenticationProvider(PasswordEncoder passwordEncoder,
                                                                  UserDetailsService userDetailsService) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
        provider.setUserDetailsService(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return provider;
    }

    @Bean
    public FilterRegistrationBean tenantFilter(TenantServerConnectionRepository repository) {
        FilterRegistrationBean registration = new FilterRegistrationBean();
        registration.setFilter(new TenantAwareHeaderFilter(repository));
        registration.addUrlPatterns("/*");
        registration.setName("tenantFilter");
        registration.setOrder(Integer.MIN_VALUE+1);
        return registration;
    }

    @Bean
    public FilterRegistrationBean corsFilter() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowCredentials(true);
        // addAllowedOriginPattern, not addAllowedOrigin: since Spring 5.3 a literal
        // "*" origin together with allowCredentials=true makes CorsConfiguration
        // throw, so with Spring 6 every request carrying an Origin header answered
        // 500 - which is every request from the operations web console, login
        // included. Spring 5.1 (Spring Boot 2.1.9) still accepted it, so the same
        // code worked before the migration. allowedOriginPattern echoes the request
        // Origin back and keeps exactly the old behaviour.
        config.addAllowedOriginPattern("*");
        config.addAllowedHeader("*");
        config.addAllowedMethod("*");
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        FilterRegistrationBean bean = new FilterRegistrationBean(new CorsFilter(source));
        bean.setOrder(securityFilterOrder - 5);
        return bean;
    }

    // The old TokenStore / DefaultTokenServices / JwtAccessTokenConverter beans
    // belonged to the discontinued Spring Security OAuth2 stack. Their jobs
    // moved to ResourceServerConfig (JwtDecoder/JwtEncoder on the same PEM key
    // pair, AudienceVerifier as token validator) and TokenController
    // (/oauth/token endpoint).

    @Bean
    public AuthenticationManager authenticationManager(DaoAuthenticationProvider customAuthenticationProvider) {
        List<AuthenticationProvider> providers = new ArrayList<>();
        providers.add(customAuthenticationProvider);
        return new ProviderManager(providers);
    }

    public static void main(String[] args) throws Exception {
        SpringApplication.run(ServerApplication.class, args);
    }
}
