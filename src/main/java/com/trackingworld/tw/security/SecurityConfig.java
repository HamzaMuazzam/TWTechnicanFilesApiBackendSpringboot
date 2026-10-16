package com.trackingworld.tw.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trackingworld.tw.alerts.domain.SubjectType;
import com.trackingworld.tw.alerts.repo.AdminUserRepository;
import com.trackingworld.tw.alerts.repo.AppUserRepository;
import com.trackingworld.tw.alerts.repo.IngestSourceRepository;
import com.trackingworld.tw.common.ApiPaths;
import com.trackingworld.tw.common.ErrorCodes;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.cors.CorsConfigurationSource;

import java.util.Set;

/**
 * Three stateless chains (ingest API key, app JWT, admin JWT) plus a permit-all chain for the legacy
 * technician API, the dashboard SPA, swagger and actuator health.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
@Slf4j
public class SecurityConfig {

    private final JwtService jwtService;
    private final AppUserRepository appUserRepository;
    private final AdminUserRepository adminUserRepository;
    private final IngestSourceRepository ingestSourceRepository;
    private final TransactionTemplate transactionTemplate;
    private final ObjectMapper objectMapper;
    private final CorsConfigurationSource corsConfigurationSource;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    @Order(1)
    @ConditionalOnProperty(name = "app.security.enabled", havingValue = "true")
    public SecurityFilterChain ingestChain(HttpSecurity http) throws Exception {
        common(http.securityMatcher(ApiPaths.INGEST + "/**"))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .anyRequest().authenticated())
                .addFilterBefore(new ApiKeyAuthFilter(ingestSourceRepository, transactionTemplate, objectMapper),
                        UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    @Order(2)
    @ConditionalOnProperty(name = "app.security.enabled", havingValue = "true")
    public SecurityFilterChain appChain(HttpSecurity http) throws Exception {
        common(http.securityMatcher(ApiPaths.APP + "/**"))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(ApiPaths.APP_AUTH + "/firebase", ApiPaths.APP_AUTH + "/login",
                                ApiPaths.APP_AUTH + "/dev-login", ApiPaths.APP_AUTH + "/refresh").permitAll()
                        .anyRequest().hasRole("USER"))
                .addFilterBefore(jwtFilter(SubjectType.USER), UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    @Order(3)
    @ConditionalOnProperty(name = "app.security.enabled", havingValue = "true")
    public SecurityFilterChain adminChain(HttpSecurity http) throws Exception {
        common(http.securityMatcher(ApiPaths.ADMIN + "/**"))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers(ApiPaths.ADMIN_AUTH + "/login", ApiPaths.ADMIN_AUTH + "/refresh").permitAll()
                        .anyRequest().hasAnyRole("ADMIN", "VIEWER"))
                .addFilterBefore(jwtFilter(SubjectType.ADMIN), UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    /**
     * app.security.enabled=false (the default for now): one open chain. {@link OpenAccessFilter} still sets a
     * principal so controllers work unchanged — the ingest API needs no key, the app identifies itself with
     * X-User-Phone, and the dashboard needs no login.
     */
    @Bean
    @Order(0)
    @ConditionalOnProperty(name = "app.security.enabled", havingValue = "false", matchIfMissing = true)
    public SecurityFilterChain openChain(HttpSecurity http) throws Exception {
        log.warn("[SECURITY] app.security.enabled=false -> ingest, app and admin APIs are OPEN (no API key, no JWT)");
        common(http).authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                .addFilterBefore(new OpenAccessFilter(ingestSourceRepository, appUserRepository, adminUserRepository,
                        transactionTemplate), UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    @Order(4)
    @ConditionalOnProperty(name = "app.security.enabled", havingValue = "true")
    public SecurityFilterChain publicChain(HttpSecurity http) throws Exception {
        // /technician/** (legacy files API + swagger), /admin/** (SPA), /swagger-ui/**, /v3/api-docs/**,
        // /actuator/health: all public. Anything unknown falls through to Spring MVC's 404.
        common(http).authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        return http.build();
    }

    private HttpSecurity common(HttpSecurity http) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())
                .logout(logout -> logout.disable())
                .requestCache(cache -> cache.disable())
                .anonymous(Customizer.withDefaults())
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(authenticationEntryPoint())
                        .accessDeniedHandler(accessDeniedHandler()));
    }

    private JwtAuthFilter jwtFilter(SubjectType type) {
        Set<String> publicPaths = type == SubjectType.USER
                ? Set.of(ApiPaths.APP_AUTH + "/firebase", ApiPaths.APP_AUTH + "/login", ApiPaths.APP_AUTH + "/dev-login",
                        ApiPaths.APP_AUTH + "/refresh")
                : Set.of(ApiPaths.ADMIN_AUTH + "/login", ApiPaths.ADMIN_AUTH + "/refresh");
        return new JwtAuthFilter(jwtService, type, appUserRepository, adminUserRepository, objectMapper, publicPaths);
    }

    private AuthenticationEntryPoint authenticationEntryPoint() {
        return (request, response, ex) ->
                SecurityResponses.write(response, objectMapper, HttpServletResponse.SC_UNAUTHORIZED, ErrorCodes.UNAUTHORIZED);
    }

    private AccessDeniedHandler accessDeniedHandler() {
        return (request, response, ex) ->
                SecurityResponses.write(response, objectMapper, HttpServletResponse.SC_FORBIDDEN, ErrorCodes.FORBIDDEN);
    }
}
