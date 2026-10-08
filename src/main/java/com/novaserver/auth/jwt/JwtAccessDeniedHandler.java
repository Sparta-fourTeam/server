package com.novaserver.auth.jwt;

import com.novaserver.global.error.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

/** 인증은 됐지만 권한이 없는 요청(예: GAME 토큰으로 관리자 API 호출)에 403을 응답한다. */
@Component
public class JwtAccessDeniedHandler implements AccessDeniedHandler {

    @Override
    public void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            AccessDeniedException accessDeniedException)
            throws IOException {
        ErrorCode error = ErrorCode.FORBIDDEN;
        response.sendError(error.getStatus().value(), error.getMessage());
    }
}
