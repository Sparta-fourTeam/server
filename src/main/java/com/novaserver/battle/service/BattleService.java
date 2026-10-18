package com.novaserver.battle.service;

import com.novaserver.battle.dto.BattleEndRequest;
import com.novaserver.battle.dto.BattleResponse;
import com.novaserver.battle.entity.BattleSession;
import com.novaserver.battle.repository.BattleRepository;
import com.novaserver.global.error.ErrorCode;
import com.novaserver.stage.dto.ClearRewardResult;
import com.novaserver.stage.entity.Stage;
import com.novaserver.stage.service.StageRewardService;
import com.novaserver.stage.service.StageService;
import com.novaserver.user.entity.User;
import com.novaserver.user.entity.UserStageProgress;
import com.novaserver.user.repository.UserRepository;
import com.novaserver.user.repository.UserStageProgressRepository;
import com.novaserver.user.service.UserRewardService;
import com.novaserver.user.service.UserService;
import java.time.Clock;
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
    private final UserRepository userRepository;
    private final UserRewardService userRewardService;
    private final UserStageProgressRepository userStageProgressRepository;
    private final StageRewardService stageRewardService;
    private final StageService stageService;

    private final BattleRepository battleRepository;
    private final Clock clock;

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
        battleRepository.upsert(userId, battleId, createId, clock.instant());

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
     * 배틀 ID가 일치하면 배틀 세션을 먼저 삭제해 선점한 뒤, 기본 보상(경험치·골드·아이템)을 지급하고 스테이지 클리어 등급을 갱신한다. 같은 배틀을 동시에 두 번 종료
     * 요청해도 삭제에 성공한 요청만 보상을 받는다. 배틀을 포기(cancel)하면 이 메서드를 거치지 않으므로 보상을 받지 못한다.
     *
     * @param userId 유저 ID
     * @param request 종료할 배틀 ID와 이번 배틀의 클리어 결과
     */
    @Transactional
    public void endBattle(Long userId, BattleEndRequest request) {
        BattleSession session =
                battleRepository
                        .findById(userId)
                        .orElseThrow(ErrorCode.BATTLE_NOT_FOUND::exception);
        if (!session.getBattleId().equals(request.getBattleId())) {
            throw ErrorCode.BATTLE_ID_MISMATCH.exception();
        }

        int deleted = battleRepository.deleteByUserIdAndBattleId(userId, request.getBattleId());
        if (deleted == 0) {
            // 동시에 들어온 다른 종료 요청이 이미 세션을 선점해 삭제했다.
            throw ErrorCode.BATTLE_NOT_FOUND.exception();
        }

        ClearRewardResult reward =
                stageRewardService.calculateClearReward(
                        request.getStageId(), request.getClearedWave(), request.getRating());
        userRewardService.grant(userId, reward.exp(), reward.gold(), reward.items());

        UserStageProgress progress =
                userStageProgressRepository
                        .findByUserIdAndStageId(userId, request.getStageId())
                        .orElseGet(
                                () -> {
                                    User user = userRepository.getReferenceById(userId);
                                    Stage stage = stageService.findStage(request.getStageId());
                                    return userStageProgressRepository.save(
                                            new UserStageProgress(user, stage));
                                });
        progress.updateClearRating(request.getRating());
    }
}
