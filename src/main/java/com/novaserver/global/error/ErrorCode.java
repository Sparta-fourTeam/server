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
    SKILL_DUPLICATED(HttpStatus.CONFLICT, "이미 존재하는 스킬명입니다: %s"),
    SKILL_BASE_STAT_VALUE_INVALID(HttpStatus.BAD_REQUEST, "baseStats 값은 유한한 숫자여야 합니다: %s"),

    // 스킬 형태
    SKILL_FORM_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 스킬 형태입니다: %s"),
    SKILL_FORM_DUPLICATED(HttpStatus.CONFLICT, "이미 있는 스킬 형태 이름입니다: %s"),
    SKILL_FORM_IN_USE(HttpStatus.CONFLICT, "사용 중인 효과가 있어 삭제할 수 없습니다: %s"),

    // 효과 발동 시점
    EFFECT_TRIGGER_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 발동 시점입니다: %s"),
    EFFECT_TRIGGER_DUPLICATED(HttpStatus.CONFLICT, "이미 있는 발동 시점 이름입니다: %s"),
    EFFECT_TRIGGER_IN_USE(HttpStatus.CONFLICT, "사용 중인 효과가 있어 삭제할 수 없습니다: %s"),

    // 효과 종류
    EFFECT_KIND_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 효과 종류입니다: %s"),
    EFFECT_KIND_DUPLICATED(HttpStatus.CONFLICT, "이미 있는 효과 종류 이름입니다: %s"),
    EFFECT_KIND_IN_USE(HttpStatus.CONFLICT, "사용 중인 효과가 있어 삭제할 수 없습니다: %s"),

    // 강화 카드
    UPGRADE_CARD_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 강화 카드입니다: %s"),
    CARD_MIN_BATTLE_LEVEL_INVALID(HttpStatus.BAD_REQUEST, "minBattleLevel은 1~%s 사이여야 합니다: %s"),

    // 카드 변형
    CARD_VARIANT_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 카드 변형입니다: %s"),
    CARD_VARIANT_DUPLICATED(HttpStatus.CONFLICT, "이미 있는 변형 조건입니다: %s"),

    // 카드 효과
    CARD_EFFECT_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 카드 효과입니다: %s"),
    CARD_EFFECT_INHERIT_VALUE_INVALID(HttpStatus.BAD_REQUEST, "inherit 값은 유한한 숫자여야 합니다: %s"),

    // 선행 카드 조건
    REQUIRED_CARD_COUNT_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 선행 카드 조건입니다: %s"),
    REQUIRED_CARD_COUNT_DUPLICATED(HttpStatus.CONFLICT, "이미 등록된 선행 카드 조건입니다: %s"),
    REQUIRED_CARD_COUNT_SELF_REFERENCE(HttpStatus.BAD_REQUEST, "자기 자신을 선행 카드로 지정할 수 없습니다: %s"),

    // 카드 배제 조건
    CARD_EXCLUSION_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 배제 조건입니다: %s"),
    CARD_EXCLUSION_DUPLICATED(HttpStatus.CONFLICT, "이미 등록된 배제 조건입니다: %s"),
    CARD_EXCLUSION_SELF_REFERENCE(HttpStatus.BAD_REQUEST, "자기 자신을 배제 카드로 지정할 수 없습니다: %s"),
    CARD_EXCLUSION_SELF_REFERENCE(HttpStatus.BAD_REQUEST, "자기 자신을 배제 카드로 지정할 수 없습니다: %s"),

    // 필요 스킬 조건
    REQUIRED_SKILL_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 필요 스킬 조건입니다: %s"),
    REQUIRED_SKILL_DUPLICATED(HttpStatus.CONFLICT, "이미 등록된 필요 스킬 조건입니다: %s"),

    // 카드 효과 적용 조건
    MIRRORED_CARD_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 카드 효과 적용 조건입니다: %s"),
    MIRRORED_CARD_DUPLICATED(HttpStatus.CONFLICT, "이미 등록된 카드 효과 적용 조건입니다: %s"),

    // 스킬 속성
    SKILL_ELEMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 속성입니다: %s"),
    SKILL_ELEMENT_DUPLICATED(HttpStatus.CONFLICT, "이미 있는 속성 이름입니다: %s"),
    SKILL_ELEMENT_IN_USE(HttpStatus.CONFLICT, "사용 중인 스킬이 있어 삭제할 수 없습니다: %s"),

    // 유저
    USER_DATA_NOT_FOUND(HttpStatus.NOT_FOUND, "유저 데이터가 없습니다: %s"),
    WALLET_NOT_FOUND(HttpStatus.NOT_FOUND, "유저 지갑이 없습니다: %s"),
    ENERGY_NOT_FOUND(HttpStatus.NOT_FOUND, "유저 에너지가 없습니다: %s");

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
