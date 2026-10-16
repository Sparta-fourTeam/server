package com.novaserver.battle.service;

import com.novaserver.battle.dto.BattleResponse;
import com.novaserver.battle.repository.BattleRepository;
import com.novaserver.global.error.ErrorCode;
import com.novaserver.user.service.UserService;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 배틀ID 발급 및 확인 후 삭제 로직. */
@Service
@RequiredArgsConstructor
public class BattleService {
    public static final int BATTLE_ENERGY_COST = 5;

    private final UserService userService;

    private final BattleRepository battleRepository;

    /**
     * 유저에게 새 배틀 ID를 발급한다. 같은 멱등성 키로 이미 발급받은 배틀이 있으면 기존 ID를 그대로 돌려주고, 그렇지 않으면 에너지를 소모하고 새 ID로 덮어쓴다.
     *
     * @param userId 유저 ID
     * @param createId 클라이언트가 발급 요청마다 보내는 멱등성 키
     * @return 발급된(또는 기존) 배틀 ID
     */
    @Transactional
    public BattleResponse createBattle(Long userId, String createId) {
        return battleRepository
                .findById(userId)
                .filter(session -> session.getCreateId().equals(createId))
                .map(session -> new BattleResponse(session.getBattleId()))
                .orElseGet(() -> issueNewBattle(userId, createId));
    }

    private BattleResponse issueNewBattle(Long userId, String createId) {
        userService.useEnergy(userId, BATTLE_ENERGY_COST);

        String battleId = UUID.randomUUID().toString();
        battleRepository.upsert(userId, battleId, createId, LocalDateTime.now());

        return new BattleResponse(battleId);
    }

    /**
     * 배틀 ID가 일치하면 배틀 세션을 삭제한다.
     *
     * @param userId 유저 ID
     * @param battleId 포기할 배틀 ID
     */
    @Transactional
    public void cancelBattle(Long userId, String battleId) {
        int deleted = battleRepository.deleteByUserIdAndBattleId(userId, battleId);
        if (deleted == 0) {
            if (!battleRepository.existsById(userId)) {
                throw ErrorCode.BATTLE_NOT_FOUND.exception();
            }
            throw ErrorCode.BATTLE_ID_MISMATCH.exception();
        }
    }

    /**
     * Todo: 추후에 보상 획득 부분 추가해야 함. 배틀 ID가 일치하면 배틀 세션을 삭제한다.
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
