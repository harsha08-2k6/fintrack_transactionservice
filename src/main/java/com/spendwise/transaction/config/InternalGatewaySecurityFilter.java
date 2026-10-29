package com.spendwise.transaction.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Validates that requests to this microservice arrive through the API Gateway
 * or an authorized internal peer service.
 */
@Component
public class InternalGatewaySecurityFilter extends OncePerRequestFilter {

    @Value("${fintrack.internal.gateway-key:${INTERNAL_GATEWAY_KEY:}}")
    private String internalGatewayKey;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String path = request.getRequestURI();

        if (path.startsWith("/actuator") || path.startsWith("/error")) {
            filterChain.doFilter(request, response);
            return;
        }

        if (internalGatewayKey != null && !internalGatewayKey.isBlank()) {
            String incomingKey = request.getHeader("X-Internal-Gateway-Key");
            if (incomingKey == null || !internalGatewayKey.equals(incomingKey.trim())) {
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                response.setContentType("application/json");
                response.getWriter().write("{\"error\":\"Forbidden\",\"message\":\"Direct microservice access denied. Requests must pass through API Gateway.\",\"status\":403}");
                return;
            }
        }

        filterChain.doFilter(request, response);
    }
}

