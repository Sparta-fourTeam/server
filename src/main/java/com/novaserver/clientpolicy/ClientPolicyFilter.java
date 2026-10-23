package com.novaserver.clientpolicy;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.UrlPathHelper;

/** JWT 뒤에서 GAME 요청만 검사한다. WEB 권한은 헤더 문자열이 아니라 검증된 인증에서 판정한다. */
public class ClientPolicyFilter extends OncePerRequestFilter {
    private static final Set<String> NEW_ENTRY =
            Set.of(
                    "/api/battle/start",
                    "/api/auth/guest/signup",
                    "/api/auth/guest/login",
                    "/api/auth/social/login");
    private static final Set<String> COMPLETION =
            Set.of(
                    "/api/battle/end",
                    "/api/battle/cancel",
                    "/api/battle/progress",
                    "/api/battle/progress/open",
                    "/api/battle/progress/abandon",
                    "/api/auth/refresh");
    private final Supplier<ClientPolicy> source;
    private final ObjectMapper mapper = new ObjectMapper();

    public ClientPolicyFilter(Supplier<ClientPolicy> source) {
        this.source = source;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        String path = UrlPathHelper.defaultInstance.getPathWithinApplication(request);
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean web = hasAuthority(auth, "CLIENT_WEB");
        boolean excluded =
                path.equals("/api/client/status")
                        || (web && path.startsWith("/api/admin/"))
                        || path.startsWith("/api/auth/web/")
                        || !path.startsWith("/api/")
                        || path.startsWith("/api/v3/api-docs");
        boolean game =
                hasAuthority(auth, "CLIENT_GAME")
                        || (web && !excluded)
                        || (path.startsWith("/api/auth/") && !path.startsWith("/api/auth/web/"));
        if (excluded || !game) {
            chain.doFilter(request, response);
            return;
        }
        ClientPolicy policy = source.get();
        response.setHeader("X-Client-Policy-Revision", policy.revision());
        String code =
                rejection(
                        policy,
                        path,
                        request.getHeader("X-Game-Platform"),
                        request.getHeader("X-Game-Build"));
        if (code == null) {
            chain.doFilter(request, response);
            return;
        }
        int status = code.equals("CLIENT_UPDATE_REQUIRED") ? 426 : 503;
        response.setStatus(status);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Cache-Control", "no-store");
        response.setHeader("Retry-After", Integer.toString(policy.retryAfterSeconds()));
        response.setHeader("X-Client-Policy-Revision", policy.revision());
        mapper.writeValue(
                response.getOutputStream(),
                Map.of(
                        "status",
                        status,
                        "error",
                        HttpStatus.valueOf(status).getReasonPhrase(),
                        "code",
                        code,
                        "message",
                        policy.message(),
                        "path",
                        path,
                        "policyRevision",
                        policy.revision()));
    }

    /** 완료·재시도 경로는 최소 버전 차단보다 우선한다. MAINTENANCE는 전체 GAME 요청을 막는다. */
    public static String rejection(
            ClientPolicy policy, String path, String platform, String build) {
        if (!policy.enforcementEnabled()) {
            return null;
        }
        if (policy.isMaintenance()) {
            return "MAINTENANCE";
        }
        if ("DRAINING".equals(policy.mode()) && NEW_ENTRY.contains(path)) {
            return "DRAINING";
        }
        if (COMPLETION.contains(path) || path.startsWith("/api/battle/progress/")) {
            return null;
        }
        if ((platform == null || platform.isEmpty()) && (build == null || build.isEmpty())) {
            return policy.allowLegacy() ? null : "CLIENT_UPDATE_REQUIRED";
        }
        ClientPolicy.PlatformPolicy rule =
                platform == null ? null : policy.platforms().get(platform);
        return rule == null
                        || !BuildNumber.valid(build)
                        || BuildNumber.compare(build, rule.minimumBuild()) < 0
                ? "CLIENT_UPDATE_REQUIRED"
                : null;
    }

    private boolean hasAuthority(Authentication auth, String authority) {
        return auth != null
                && auth.isAuthenticated()
                && auth.getAuthorities().stream().anyMatch(a -> authority.equals(a.getAuthority()));
    }
}
