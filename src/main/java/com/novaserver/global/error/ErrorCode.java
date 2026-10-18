package com.novaserver.global.error;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/** 서버 공통 에러 코드. 상태 코드와 메시지를 함께 관리한다. */
@Getter
@RequiredArgsConstructor
public enum ErrorCode {
    TUTORIAL_NOT_FOUND(HttpStatus.NOT_FOUND, "튜토리얼이 없습니다: %s"),
    TUTORIAL_INVALID(HttpStatus.BAD_REQUEST, "튜토리얼 조건·보상 참조가 올바르지 않습니다"),
    TUTORIAL_NOT_READY(HttpStatus.CONFLICT, "튜토리얼 완료 조건을 충족하지 않았습니다"),
    TUTORIAL_IN_USE(HttpStatus.CONFLICT, "계정 상태 또는 튜토리얼 조건에서 참조 중입니다"),
    TUTORIAL_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "튜토리얼 레벨 기준을 적용할 수 없습니다"),

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
    ALREADY_LINKED_PROVIDER(HttpStatus.CONFLICT, "이미 소셜 계정과 연동되어 있습니다"),
    SOCIAL_ACCOUNT_ALREADY_LINKED(HttpStatus.CONFLICT, "이미 다른 계정에 연동된 소셜 계정입니다"),
    ACCOUNT_DELETED(HttpStatus.UNAUTHORIZED, "삭제된 계정입니다"),

    // 데이터 버전
    DATA_TABLE_NOT_FOUND(HttpStatus.BAD_REQUEST, "존재하지 않는 테이블입니다: %s"),
    DATA_VERSION_ROW_MISSING(HttpStatus.INTERNAL_SERVER_ERROR, "데이터 버전 행이 없습니다: %s"),

    // 배틀
    BATTLE_NOT_FOUND(HttpStatus.NOT_FOUND, "진행 중인 배틀이 없습니다"),
    BATTLE_REWARD_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "전투 보상 기준 데이터가 올바르지 않습니다"),
    BATTLE_RESULT_CONFLICT(HttpStatus.CONFLICT, "이미 확정한 전투 결과와 요청 내용이 다릅니다"),
    ITEM_LINKED_TO_REWARD_RULES(HttpStatus.CONFLICT, "보상 기준에 연결된 아이템입니다: %s"),
    REWARD_RULES_NOT_CONFIGURED(HttpStatus.SERVICE_UNAVAILABLE, "보상 기준 데이터가 설정되지 않았거나 잘못되었습니다"),
    REWARD_RULES_INVALID(HttpStatus.BAD_REQUEST, "보상 기준 데이터가 올바르지 않습니다"),
    BATTLE_RESULT_INVALID(HttpStatus.BAD_REQUEST, "완료 웨이브와 별 등급이 일치하지 않습니다"),
    STAGE_REWARD_ITEM_UNSUPPORTED(HttpStatus.BAD_REQUEST, "지급 규칙이 준비되지 않은 보상 아이템입니다: %s"),
    STAGE_REWARD_BALANCE_INVALID(HttpStatus.BAD_REQUEST, "스테이지 보상 총량이 올바르지 않습니다"),
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

    // 플레이어 레벨 기준
    PLAYER_LEVELS_NOT_CONFIGURED(HttpStatus.SERVICE_UNAVAILABLE, "플레이어 레벨 기준이 등록되지 않았습니다"),
    PLAYER_LEVELS_DATA_INVALID(HttpStatus.BAD_REQUEST, "플레이어 레벨 기준이 올바르지 않습니다"),

    // 강화
    UPGRADE_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 강화입니다: %s"),
    UPGRADE_DUPLICATED(HttpStatus.CONFLICT, "이미 존재하는 강화 id입니다: %s"),
    UPGRADE_IN_USE(HttpStatus.CONFLICT, "사용 중인 유저 강화가 있어 삭제할 수 없습니다: %s"),
    UPGRADE_DATA_INVALID(HttpStatus.BAD_REQUEST, "강화 데이터가 올바르지 않습니다: %s"),
    UPGRADE_LOCKED(HttpStatus.CONFLICT, "아직 해금되지 않은 강화입니다: %s"),
    UPGRADE_MAX_LEVEL(HttpStatus.CONFLICT, "이미 최대 레벨입니다: %s"),
    UPGRADE_REQUEST_DUPLICATED(HttpStatus.CONFLICT, "이미 처리 중인 요청입니다: %s"),

    // 아이템
    ITEM_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 아이템입니다: %s"),
    ITEM_DUPLICATED(HttpStatus.CONFLICT, "이미 존재하는 아이템 id입니다: %s"),
    ITEM_IN_USE(HttpStatus.CONFLICT, "사용 중인 유저 아이템이 있어 삭제할 수 없습니다: %s"),
    ITEM_DATA_INVALID(HttpStatus.BAD_REQUEST, "아이템 데이터가 올바르지 않습니다: %s"),
    REWARD_QUANTITY_OVERFLOW(HttpStatus.CONFLICT, "보상 아이템 수량이 저장 범위를 초과합니다"),
    ITEM_QUANTITY_NOT_ENOUGH(HttpStatus.CONFLICT, "재료가 부족합니다: %s"),
    REWARD_DEFINITION_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 보상 묶음입니다: %s"),
    REWARD_DEFINITION_INVALID(HttpStatus.BAD_REQUEST, "고정 보상 묶음 설정이 올바르지 않습니다"),
    REWARD_DEFINITION_VERSION_CONFLICT(HttpStatus.CONFLICT, "보상 묶음 버전이 변경됐습니다. 다시 조회하세요"),
    REWARD_DEFINITION_IN_USE(HttpStatus.CONFLICT, "레이팅이 참조 중인 보상 묶음입니다: %s"),
    ITEM_LINKED_TO_REWARD_DEFINITION(HttpStatus.CONFLICT, "고정 보상 묶음에서 참조 중인 아이템입니다: %s"),
    ITEM_LINKED_TO_UPGRADE(HttpStatus.CONFLICT, "강화에서 재료로 사용 중이어서 삭제할 수 없습니다: %s"),

    // 스테이지
    STAGE_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 스테이지입니다: %s"),
    STAGE_DUPLICATED(HttpStatus.CONFLICT, "이미 존재하는 스테이지 id입니다: %s"),
    STAGE_REWARD_EXISTS(HttpStatus.CONFLICT, "등록된 보상이 있어 삭제할 수 없습니다: %s"),

    // 스테이지 보상
    STAGE_REWARD_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 스테이지 보상입니다: %s"),
    STAGE_REWARD_DUPLICATED(HttpStatus.CONFLICT, "이미 등록된 스테이지 보상입니다: %s"),
    STAGE_REWARD_ITEM_DUPLICATED(HttpStatus.BAD_REQUEST, "같은 아이템을 중복해서 넣을 수 없습니다: %s"),
    STAGE_REWARD_ITEM_AMOUNT_INVALID(
            HttpStatus.BAD_REQUEST, "아이템 보상 총수량이 지급 규칙의 최소 누적량보다 작습니다: %s"),

    // 스테이지 별등급 보상
    STAGE_RATING_REWARD_INVALID(HttpStatus.BAD_REQUEST, "별등급 보상 묶음 ID가 올바르지 않습니다"),
    STAGE_RATING_REWARD_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "별등급 보상 기준이 없거나 올바르지 않습니다"),
    STAGE_RATING_CLAIM_TARGET_INVALID(HttpStatus.CONFLICT, "수령 목표 별 등급이 달성한 등급을 벗어납니다"),
    STAGE_RATING_CLAIM_CONFLICT(HttpStatus.CONFLICT, "같은 수령 요청 키의 스테이지·목표 등급을 변경할 수 없습니다"),
    REWARD_GOLD_OVERFLOW(HttpStatus.CONFLICT, "보상 골드가 저장 범위를 초과합니다"),
    STAGE_RATING_REWARD_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 별등급 보상입니다: stageId=%s, rating=%s"),
    STAGE_RATING_REWARD_DUPLICATED(HttpStatus.CONFLICT, "이미 등록된 별등급 보상입니다: stageId=%s, rating=%s"),

    // 유저 스테이지 진행도
    STAGE_PROGRESS_NOT_FOUND(HttpStatus.NOT_FOUND, "스테이지 진행도가 없습니다: %s"),
    STAGE_RATING_REWARD_NOTHING_TO_CLAIM(HttpStatus.CONFLICT, "받을 수 있는 보상이 없습니다: %s"),

    // 유저
    USER_DATA_NOT_FOUND(HttpStatus.NOT_FOUND, "유저 데이터가 없습니다: %s"),
    WALLET_NOT_FOUND(HttpStatus.NOT_FOUND, "유저 지갑이 없습니다: %s"),
    WALLET_GOLD_NOT_ENOUGH(HttpStatus.CONFLICT, "골드가 부족합니다: %s"),
    ENERGY_NOT_FOUND(HttpStatus.NOT_FOUND, "유저 에너지가 없습니다: %s"),

    // 에너지
    INSUFFICIENT_ENERGY(HttpStatus.CONFLICT, "해당 유저의 에너지가 부족합니다."),
    ENERGY_LOCK_CONFLICT(HttpStatus.CONFLICT, "잠시 후 다시 시도해주세요."),
    WALLET_GEM_NOT_ENOUGH(HttpStatus.CONFLICT, "젬이 부족합니다."),
    ENERGY_PURCHASE_LIMIT(HttpStatus.CONFLICT, "일일 에너지 구매 한도에 도달했습니다."),
    ENERGY_STORAGE_LIMIT(HttpStatus.CONFLICT, "에너지 저장 범위를 초과합니다."),
    ENERGY_REQUEST_CONFLICT(HttpStatus.CONFLICT, "같은 회복 요청 키의 경로를 변경할 수 없습니다."),
    AD_REWARD_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "광고 보상 검증이 준비되지 않았습니다.");

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
