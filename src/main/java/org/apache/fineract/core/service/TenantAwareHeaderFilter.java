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
package org.apache.fineract.core.service;

import org.apache.commons.lang3.time.StopWatch;
import org.apache.fineract.organisation.tenant.TenantServerConnectionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.GenericFilterBean;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;


public class TenantAwareHeaderFilter extends GenericFilterBean {

    private static final String TENANT_IDENTIFIER_REQUEST_HEADER = "Platform-TenantId";
    private static final String TENANT_IDENTIFIER_REQUEST_PARAM = "tenantIdentifier";
    // public because ResourceServerConfig has to know which paths run without a
    // tenant: those get a filter chain with no resource server on it, otherwise a
    // bearer token on them reaches AudienceVerifier with no tenant to compare against
    public static final String EXCLUDED_URL = "/oauth/token_key";
    /**
     * G2P reference data is shared by all tenants, it lives in the core schema.
     * The operations web console calls these paths without a Platform-TenantId
     * header (its interceptor only adds it to /batches, /transactions and
     * /transfers), so asking for a tenant here would fail every call.
     * With no tenant set, DataSourcePerTenantService falls back to the core
     * connection, which is where these tables are.
     */
    public static final List<String> TENANT_LESS_PATHS =
            List.of("/governmentEntity", "/program", "/dfsp", "/g2pPaymentConfig");
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final TenantServerConnectionRepository repository;

    public TenantAwareHeaderFilter(TenantServerConnectionRepository repository) {
        this.repository = repository;
    }

    private static boolean isTenantLess(String servletPath) {
        for (String path : TENANT_LESS_PATHS) {
            if (servletPath.equals(path) || servletPath.startsWith(path + "/")) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void doFilter(final ServletRequest req, final ServletResponse res, final FilterChain chain) throws IOException {
        final HttpServletRequest request = (HttpServletRequest) req;
        final HttpServletResponse response = (HttpServletResponse) res;
        final StopWatch task = new StopWatch();
        task.start();

        try {
            if(!EXCLUDED_URL.equals(request.getServletPath()) && !isTenantLess(request.getServletPath()) &&
                    !request.getServletPath().contains("swagger") && !request.getServletPath().contains("api-docs") && !request.getServletPath().contains("actuator") ) {
                String tenantIdentifier = request.getHeader(TENANT_IDENTIFIER_REQUEST_HEADER);
                if (tenantIdentifier == null || tenantIdentifier.length() < 1) {
                    tenantIdentifier = request.getParameter(TENANT_IDENTIFIER_REQUEST_PARAM);
                }

                if (tenantIdentifier == null || tenantIdentifier.length() < 1) {
                    throw new RuntimeException(
                            String.format("No tenant identifier found! Add request header: %s or request param: %s", TENANT_IDENTIFIER_REQUEST_HEADER, TENANT_IDENTIFIER_REQUEST_PARAM));
                }

                ThreadLocalContextUtil.setTenant(this.repository.findOneBySchemaName(tenantIdentifier));
            }
            chain.doFilter(request, res);
        } catch (Exception e) {
            logger.error("Error when executing request!", e);
            SecurityContextHolder.getContext().setAuthentication(null);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        } finally {
            task.stop();
            ThreadLocalContextUtil.clear();
            logger.info(PlatformRequestLog.from(task, request).toString());
        }
    }
}
