package com.novaserver.global.error;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/** 서버 공통 에러 코드. 상태 코드와 메시지를 함께 관리한다. */
@Getter
@RequiredArgsConstructor
public enum ErrorCode {
    // 공통
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "요청 형식이 올바르지 않습니다"),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 오류가 발생했습니다"),

    // 인증
    TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "토큰이 만료되었습니다"),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "유효하지 않은 토큰입니다"),

    // 데이터 버전
    DATA_TABLE_NOT_FOUND(HttpStatus.BAD_REQUEST, "존재하지 않는 테이블입니다: %s"),
    DATA_VERSION_ROW_MISSING(HttpStatus.INTERNAL_SERVER_ERROR, "데이터 버전 행이 없습니다: %s"),

    // 배틀
    BATTLE_NOT_FOUND(HttpStatus.NOT_FOUND, "진행 중인 배틀이 없습니다"),
    BATTLE_ID_MISMATCH(HttpStatus.BAD_REQUEST, "배틀 ID가 일치하지 않습니다");

    private final HttpStatus status;
    private final String message;

    /**
     * 이 에러 코드로 예외를 만든다. 메시지의 %s 자리에 args가 순서대로 들어간다.
     *
     * @param args 메시지에 넣을 값
     * @return 던질 예외
     */
    public ResponseStatusException exception(Object... args) {
        return new ResponseStatusException(status, message.formatted(args));
    }
}
