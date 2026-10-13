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
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "로그인이 필요합니다"),
    FORBIDDEN(HttpStatus.FORBIDDEN, "권한이 없습니다"),
    TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "토큰이 만료되었습니다"),
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "유효하지 않은 토큰입니다"),
    INVALID_GUEST_ID(HttpStatus.UNAUTHORIZED, "게스트 정보가 올바르지 않습니다"),
    INVALID_SOCIAL_TOKEN(HttpStatus.UNAUTHORIZED, "소셜 로그인 정보가 올바르지 않습니다"),
    UNSUPPORTED_PROVIDER(HttpStatus.BAD_REQUEST, "지원하지 않는 로그인 방식입니다"),
    SOCIAL_PROVIDER_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "소셜 로그인 서버에 연결할 수 없습니다"),

    // 데이터 버전
    DATA_TABLE_NOT_FOUND(HttpStatus.BAD_REQUEST, "존재하지 않는 테이블입니다: %s"),
    DATA_VERSION_ROW_MISSING(HttpStatus.INTERNAL_SERVER_ERROR, "데이터 버전 행이 없습니다: %s"),

    // 배틀
    BATTLE_NOT_FOUND(HttpStatus.NOT_FOUND, "진행 중인 배틀이 없습니다"),
    BATTLE_ID_MISMATCH(HttpStatus.BAD_REQUEST, "배틀 ID가 일치하지 않습니다"),

    // 스킬 시전 방식
    SKILL_CAST_TYPE_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 시전 방식입니다: %s"),
    SKILL_CAST_TYPE_DUPLICATED(HttpStatus.CONFLICT, "이미 있는 시전 방식 이름입니다: %s"),
    SKILL_CAST_TYPE_IN_USE(HttpStatus.CONFLICT, "사용 중인 스킬이 있어 삭제할 수 없습니다: %s"),

    // 스킬 투사체 경로
    SKILL_PROJECTILE_PATH_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 투사체 경로입니다: %s"),
    SKILL_PROJECTILE_PATH_DUPLICATED(HttpStatus.CONFLICT, "이미 있는 투사체 경로 이름입니다: %s"),
    SKILL_PROJECTILE_PATH_IN_USE(HttpStatus.CONFLICT, "사용 중인 스킬이 있어 삭제할 수 없습니다: %s"),

    // 스킬
    SKILL_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 스킬입니다: %s"),
    SKILL_DUPLICATED(HttpStatus.CONFLICT, "이미 존재하는 스킬명입니다: %s");

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
