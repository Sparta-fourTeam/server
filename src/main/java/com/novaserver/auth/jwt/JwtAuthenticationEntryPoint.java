package com.novaserver.auth.jwt;

import com.novaserver.global.error.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

/**
 * 인증이 필요한 경로에 인증 없이 들어온 요청에 401을 응답한다.
 *
 * <p>{@link JwtAuthenticationFilter}가 남긴 실패 원인이 있으면 그 메시지(만료·위조)를, 없으면 토큰을 보내지 않은 것으로 보고
 * UNAUTHORIZED 메시지를 쓴다. 응답 본문은 {@code /error}를 거쳐 CustomErrorAttributes가 만든다.
 */
@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException)
            throws IOException {
        ResponseStatusException error =
                request.getAttribute(JwtAuthenticationFilter.ERROR_ATTRIBUTE)
                                instanceof ResponseStatusException tokenError
                        ? tokenError
                        : ErrorCode.UNAUTHORIZED.exception();
        response.sendError(error.getStatusCode().value(), error.getReason());
    }
}
