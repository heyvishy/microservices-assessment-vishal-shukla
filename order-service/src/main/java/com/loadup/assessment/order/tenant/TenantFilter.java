package com.loadup.assessment.order.tenant;

import com.loadup.assessment.order.exception.MissingTenantHeaderException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerExceptionResolver;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class TenantFilter extends OncePerRequestFilter {
    public static final String HEADER = "X-Tenant-Id";
    private static final Logger log = LoggerFactory.getLogger(TenantFilter.class);
    private final HandlerExceptionResolver handlerExceptionResolver;

    public TenantFilter(@Qualifier("handlerExceptionResolver") final HandlerExceptionResolver handlerExceptionResolver) {
        this.handlerExceptionResolver = handlerExceptionResolver;
    }

    @Override
    protected boolean shouldNotFilter(final HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/actuator") || path.equals("/error");
    }

    @Override
    protected void doFilterInternal(final HttpServletRequest request, final HttpServletResponse response, final FilterChain filterChain)
            throws ServletException, IOException {
        try {
            String tenantId = request.getHeader(HEADER);
            if (tenantId == null || tenantId.isBlank()) {
                log.warn("Missing tenant header path={} method={} remoteAddress={}", request.getRequestURI(), request.getMethod(), request.getRemoteAddr());
                handlerExceptionResolver.resolveException(request, response, null,
                        new MissingTenantHeaderException("Missing required header X-Tenant-Id"));
                return;
            }
            String normalizedTenantId = tenantId.trim();
            TenantContext.setTenantId(normalizedTenantId);
            MDC.put("tenantId", normalizedTenantId);
            log.debug("Tenant resolved path={} method={} tenantId={}", request.getRequestURI(), request.getMethod(), normalizedTenantId);
            filterChain.doFilter(request, response);
        } finally {
            TenantContext.clear();
            MDC.remove("tenantId");
        }
    }
}
