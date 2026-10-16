package com.trackingworld.tw.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trackingworld.tw.alerts.domain.SubjectType;
import com.trackingworld.tw.alerts.domain.UserStatus;
import com.trackingworld.tw.alerts.repo.AdminUserRepository;
import com.trackingworld.tw.alerts.repo.AppUserRepository;
import com.trackingworld.tw.common.ErrorCodes;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Set;

/**
 * Bearer JWT filter bound to one subject type. A missing header lets the request through unauthenticated
 * (the chain's authorizeHttpRequests then rejects it); a bad token answers 401 immediately with its error code.
 */
@Slf4j
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    public static final String ERROR_ATTRIBUTE = JwtAuthFilter.class.getName() + ".error";

    private final JwtService jwtService;
    private final SubjectType expectedType;
    private final AppUserRepository appUserRepository;
    private final AdminUserRepository adminUserRepository;
    private final ObjectMapper objectMapper;
    /** Public paths (login / refresh): a stale Authorization header sent by the client must not break them. */
    private final Set<String> publicPaths;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return publicPaths.contains(request.getRequestURI()) || "OPTIONS".equalsIgnoreCase(request.getMethod());
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.regionMatches(true, 0, "Bearer ", 0, 7)) {
            chain.doFilter(request, response);
            return;
        }
        String token = header.substring(7).trim();
        JwtService.ParsedToken parsed;
        try {
            parsed = jwtService.parse(token);
        } catch (JwtService.TokenException e) {
            log.debug("[AUTH] rejected bearer token on {}: {}", request.getRequestURI(), e.getMessage());
            SecurityResponses.write(response, objectMapper, HttpServletResponse.SC_UNAUTHORIZED, e.getMessage());
            return;
        }
        if (parsed.type() != expectedType) {
            SecurityResponses.write(response, objectMapper, HttpServletResponse.SC_UNAUTHORIZED, ErrorCodes.INVALID_TOKEN);
            return;
        }
        Object principal;
        List<SimpleGrantedAuthority> authorities;
        if (expectedType == SubjectType.USER) {
            if (!appUserRepository.existsByIdAndStatus(parsed.subjectId(), UserStatus.ACTIVE)) {
                SecurityResponses.write(response, objectMapper, HttpServletResponse.SC_FORBIDDEN, ErrorCodes.USER_DISABLED);
                return;
            }
            principal = new AppUserPrincipal(parsed.subjectId(), parsed.deviceId());
            authorities = List.of(new SimpleGrantedAuthority("ROLE_USER"));
        } else {
            if (parsed.role() == null || !adminUserRepository.existsByIdAndActiveTrue(parsed.subjectId())) {
                SecurityResponses.write(response, objectMapper, HttpServletResponse.SC_UNAUTHORIZED, ErrorCodes.INVALID_TOKEN);
                return;
            }
            principal = new AdminPrincipal(parsed.subjectId(), parsed.username(), parsed.role());
            authorities = List.of(new SimpleGrantedAuthority("ROLE_" + parsed.role().name()));
        }
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(principal, token, authorities);
        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authentication);
        chain.doFilter(request, response);
    }
}
