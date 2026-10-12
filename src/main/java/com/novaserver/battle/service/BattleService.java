package com.novaserver.battle.service;

import com.novaserver.battle.dto.BattleResponse;
import com.novaserver.battle.repository.BattleRepository;
import com.novaserver.global.error.ErrorCode;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 배틀ID 발급 및 확인 후 삭제 로직. */
@Service
@RequiredArgsConstructor
public class BattleService {
    private final BattleRepository battleRepository;

    /**
     * 유저에게 새 배틀 ID를 발급한다. 진행 중인 배틀이 있으면 새 ID로 덮어쓴다.
     *
     * @param userId 유저 ID
     * @return 발급된 배틀 ID
     */
    @Transactional
    public BattleResponse createBattle(Long userId) {
        String battleId = UUID.randomUUID().toString();
        battleRepository.upsert(userId, battleId, LocalDateTime.now());
        return new BattleResponse(battleId);
    }

    /**
     * 배틀 ID가 일치하면 배틀 세션을 삭제한다.
     *
     * @param userId 유저 ID
     * @param battleId 종료할 배틀 ID
     */
    @Transactional
    public void endBattle(Long userId, String battleId) {
        int deleted = battleRepository.deleteByUserIdAndBattleId(userId, battleId);
        if (deleted == 0) {
            if (!battleRepository.existsById(userId)) {
                throw ErrorCode.BATTLE_NOT_FOUND.exception();
            }
            throw ErrorCode.BATTLE_ID_MISMATCH.exception();
        }
    }
}
