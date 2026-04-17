package com.logicaldevs.twlivebackendspringboot.config;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorsFilter implements Filter {

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws IOException, ServletException {
        HttpServletResponse response = (HttpServletResponse) res;
        HttpServletRequest request = (HttpServletRequest) req;

        // Allow all origins
        response.setHeader("Access-Control-Allow-Origin", "*");
        
        // Allow all methods
        response.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS, PATCH, HEAD");
        
        // Allow all headers
        response.setHeader("Access-Control-Allow-Headers", "*");
        
        // Allow credentials
        response.setHeader("Access-Control-Allow-Credentials", "true");
        
        // Expose headers
        response.setHeader("Access-Control-Expose-Headers", "Authorization, Content-Type, X-Requested-With, Accept, Origin, Access-Control-Request-Method, Access-Control-Request-Headers");
        
        // Cache preflight requests for 1 hour
        response.setHeader("Access-Control-Max-Age", "3600");

        // Handle preflight OPTIONS requests
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            response.setStatus(HttpServletResponse.SC_OK);
        } else {
            chain.doFilter(req, res);
        }
    }

    @Override
    public void init(FilterConfig filterConfig) {
        // No initialization needed
    }

    @Override
    public void destroy() {
        // No cleanup needed
    }
}

