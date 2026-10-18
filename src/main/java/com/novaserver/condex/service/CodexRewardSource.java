package com.novaserver.condex.service;

import com.novaserver.reward.entity.RewardDefinition;
import java.util.Optional;

/**
 * #50의 서버 몬스터 등급과 CodexRewards를 공통 고정 보상으로 연결하는 경계. 구현은 같은 트랜잭션에서 등급과 보상 항목의 일관된 스냅샷을 읽어야 한다. 설정이
 * 없는 몬스터·등급은 빈 값으로 반환해 신규 지급을 차단한다.
 */
public interface CodexRewardSource {
    /** 서버 몬스터 존재·등급을 검증한 골드 전용 보상. 미설정은 빈 값이다. */
    Optional<RewardDefinition> findForMonster(Long monsterId);
}
