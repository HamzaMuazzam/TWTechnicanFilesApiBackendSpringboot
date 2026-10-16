package com.trackingworld.tw.security;

import com.trackingworld.tw.alerts.domain.AdminRole;
import com.trackingworld.tw.alerts.domain.AppUser;
import com.trackingworld.tw.alerts.domain.IngestSource;
import com.trackingworld.tw.alerts.domain.UserStatus;
import com.trackingworld.tw.alerts.repo.AdminUserRepository;
import com.trackingworld.tw.alerts.repo.AppUserRepository;
import com.trackingworld.tw.alerts.repo.IngestSourceRepository;
import com.trackingworld.tw.common.ApiPaths;
import com.trackingworld.tw.common.util.PhoneNormalizer;
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
import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Active only when {@code app.security.enabled=false}: no API key, no JWT.
 * <ul>
 *   <li>/api/v1/ingest/** — any caller acts as the first active ingest source (created as "default" if none).</li>
 *   <li>/api/v1/app/** — the caller says who it is with the {@code X-User-Phone} header (any PK format);
 *       the user is created on first sight. {@code X-Device-Id} is optional.</li>
 *   <li>/api/v1/admin/** — every caller is a full ADMIN.</li>
 * </ul>
 */
@Slf4j
@RequiredArgsConstructor
public class OpenAccessFilter extends OncePerRequestFilter {

    public static final String HEADER_USER_PHONE = "X-User-Phone";
    public static final String HEADER_DEVICE_ID = "X-Device-Id";

    private final IngestSourceRepository sourceRepository;
    private final AppUserRepository userRepository;
    private final AdminUserRepository adminRepository;
    private final TransactionTemplate tx;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String path = request.getRequestURI();
        if (path.startsWith(ApiPaths.INGEST + "/")) {
            IngestSource source = tx.execute(s -> sourceRepository.findAll().stream()
                    .filter(IngestSource::isActive).findFirst()
                    .orElseGet(() -> sourceRepository.save(IngestSource.builder()
                            .name("default").apiKeyHash("open").apiKeyPrefix("open").active(true)
                            .createdAt(Instant.now()).build())));
            authenticate(new IngestSourcePrincipal(source.getId(), source.getName()), "ROLE_INGEST");
        } else if (path.startsWith(ApiPaths.APP + "/")) {
            Optional<String> phone = PhoneNormalizer.normalize(request.getHeader(HEADER_USER_PHONE));
            if (phone.isPresent()) {
                AppUser user = tx.execute(s -> userRepository.findByPhoneE164(phone.get())
                        .orElseGet(() -> {
                            log.info("[OPEN-ACCESS] creating user {} on first request", phone.get());
                            return userRepository.save(AppUser.builder().phoneE164(phone.get())
                                    .status(UserStatus.ACTIVE).createdAt(Instant.now()).build());
                        }));
                String deviceId = Optional.ofNullable(request.getHeader(HEADER_DEVICE_ID)).orElse("unknown");
                authenticate(new AppUserPrincipal(user.getId(), deviceId), "ROLE_USER");
            }
        } else if (path.startsWith(ApiPaths.ADMIN + "/")) {
            Long id = tx.execute(s -> adminRepository.findAll().stream()
                    .filter(a -> a.getRole() == AdminRole.ADMIN && a.isActive()).findFirst().map(a -> a.getId()).orElse(0L));
            authenticate(new AdminPrincipal(id, "open-access", AdminRole.ADMIN), "ROLE_ADMIN");
        }
        chain.doFilter(request, response);
    }

    private static void authenticate(Object principal, String role) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, List.of(new SimpleGrantedAuthority(role))));
    }
}
