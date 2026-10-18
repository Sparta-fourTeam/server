package com.novaserver.user.service;

import com.novaserver.playerlevel.dto.PlayerLevelConfigResponse;
import com.novaserver.playerlevel.service.PlayerLevelService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** DB 기준 데이터와 저장된 누적 경험치로 구매 해금을 검사한다. */
@Component
@RequiredArgsConstructor
public class PlayerLevelPolicy {
    private final PlayerLevelService playerLevels;

    /** 같은 기준 스냅샷으로 지급 전 플레이어 레벨을 구한다. */
    public int levelAt(int totalExp) {
        PlayerLevelConfigResponse config = playerLevels.getConfig();
        int level = 1;
        long requiredExp = config.baseExp();
        while (level < config.maxLevel() && Math.max(0, totalExp) >= requiredExp) {
            level++;
            requiredExp += config.baseExp() + (long) (level - 1) * config.stepExp();
        }
        return level;
    }

    /** 기준 데이터가 없으면 구매를 거절하고, 있으면 누적 경험치 경계를 검사한다. */
    public boolean hasReached(int totalExp, int requiredLevel) {
        PlayerLevelConfigResponse config = playerLevels.getConfig();
        if (requiredLevel > config.maxLevel()) {
            return false;
        }
        long count = Math.max(0, requiredLevel - 1L);
        long requiredExp = count * config.baseExp() + count * (count - 1) / 2 * config.stepExp();
        return Math.max(0, totalExp) >= requiredExp;
    }
}
