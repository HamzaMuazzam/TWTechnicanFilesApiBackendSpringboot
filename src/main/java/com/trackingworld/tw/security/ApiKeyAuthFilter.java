package com.trackingworld.tw.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.trackingworld.tw.alerts.domain.IngestSource;
import com.trackingworld.tw.alerts.repo.IngestSourceRepository;
import com.trackingworld.tw.common.ErrorCodes;
import com.trackingworld.tw.common.web.ClientIp;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/** X-Api-Key authentication for /api/v1/ingest/**: SHA-256 compare against ingest_source, touches last_used_at. */
@Slf4j
@RequiredArgsConstructor
public class ApiKeyAuthFilter extends OncePerRequestFilter {

    public static final String HEADER = "X-Api-Key";
    private static final Duration TOUCH_INTERVAL = Duration.ofSeconds(30);

    private final IngestSourceRepository sourceRepository;
    private final TransactionTemplate transactionTemplate;
    private final ObjectMapper objectMapper;
    private final Map<Long, Instant> lastTouched = new ConcurrentHashMap<>();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String apiKey = request.getHeader(HEADER);
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("[INGEST][AUTH] missing {} from {}", HEADER, ClientIp.of(request));
            SecurityResponses.write(response, objectMapper, HttpServletResponse.SC_UNAUTHORIZED, ErrorCodes.INVALID_API_KEY);
            return;
        }
        Optional<IngestSource> source = sourceRepository.findByApiKeyHashAndActiveTrue(ApiKeys.hash(apiKey));
        if (source.isEmpty()) {
            log.warn("[INGEST][AUTH] invalid API key (prefix {}) from {}", ApiKeys.prefix(apiKey), ClientIp.of(request));
            SecurityResponses.write(response, objectMapper, HttpServletResponse.SC_UNAUTHORIZED, ErrorCodes.INVALID_API_KEY);
            return;
        }
        IngestSource s = source.get();
        touch(s.getId());
        UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                new IngestSourcePrincipal(s.getId(), s.getName()), null, List.of(new SimpleGrantedAuthority("ROLE_INGEST")));
        SecurityContextHolder.getContext().setAuthentication(authentication);
        chain.doFilter(request, response);
    }

    private void touch(Long sourceId) {
        Instant now = Instant.now();
        Instant previous = lastTouched.get(sourceId);
        if (previous != null && previous.plus(TOUCH_INTERVAL).isAfter(now)) {
            return;
        }
        lastTouched.put(sourceId, now);
        transactionTemplate.executeWithoutResult(status -> sourceRepository.touch(sourceId, now));
    }
}
