package com.novaserver.condex.service;

import com.novaserver.condex.repository.CodexRepository;
import com.novaserver.reward.entity.RewardDefinition;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** 서버에 저장한 몬스터 등급과 등급별 공통 보상만 읽는 실제 도감 지급 공급. */
@Component
@RequiredArgsConstructor
public class DatabaseCodexRewardSource implements CodexRewardSource {
    private final CodexRepository monsters;

    @Override
    @Transactional(readOnly = true)
    public Optional<RewardDefinition> findForMonster(Long monsterId) {
        return monsters.findRewardForMonster(monsterId);
    }
}
