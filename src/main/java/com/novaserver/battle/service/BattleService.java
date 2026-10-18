package com.novaserver.battle.service;

import com.novaserver.battle.dto.BattleEndRequest;
import com.novaserver.battle.dto.BattleResponse;
import com.novaserver.battle.dto.BattleResultResponse;
import com.novaserver.battle.entity.BattleResult;
import com.novaserver.battle.entity.BattleSession;
import com.novaserver.battle.repository.BattleRepository;
import com.novaserver.battle.repository.BattleResultRepository;
import com.novaserver.global.error.ErrorCode;
import com.novaserver.quest.service.UserQuestService;
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
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
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
    private final BattleResultRepository results;
    private final Clock clock;
    private final UserQuestService quests;

    /**
     * 유저에게 새 배틀 ID를 발급한다. 같은 멱등성 키로 이미 발급받은 배틀이 있으면 기존 ID를 그대로 돌려주고, 그렇지 않으면 에너지를 소모하고 새 ID로 덮어쓴다.
     *
     * @param userId 유저 ID
     * @param createId 클라이언트가 발급 요청마다 보내는 멱등성 키
     * @return 발급된(또는 기존) 배틀 ID
     */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public BattleResponse createBattle(Long userId, String createId) {
        lockUser(userId);
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
        quests.recordBattleEnergy(userId, BATTLE_ENERGY_COST);

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
        lockUser(userId);
        int deleted = battleRepository.deleteByUserIdAndBattleId(userId, battleId);
        if (deleted == 0) {
            if (!battleRepository.existsById(userId)) {
                throw ErrorCode.BATTLE_NOT_FOUND.exception();
            }
            throw ErrorCode.BATTLE_ID_MISMATCH.exception();
        }
    }

    /** 보상·진척·최초 확정 영수증을 함께 저장하고 동일 결과 재요청은 재지급 없이 복원한다. */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public BattleResultResponse endBattle(Long userId, BattleEndRequest request) {
        // 시작·취소·종료를 같은 계정 잠금으로 직렬화한다. 잠금 획득 후 최신 완료 기록을 읽는다.
        lockUser(userId);
        var prior = results.findByUserIdAndBattleId(userId, request.getBattleId());
        if (prior.isPresent()) {
            if (!prior.get().matches(request)) {
                throw ErrorCode.BATTLE_RESULT_CONFLICT.exception();
            }
            return BattleResultResponse.from(prior.get());
        }
        BattleSession session =
                battleRepository
                        .findById(userId)
                        .orElseThrow(ErrorCode.BATTLE_NOT_FOUND::exception);
        if (!session.getBattleId().equals(request.getBattleId())) {
            throw ErrorCode.BATTLE_ID_MISMATCH.exception();
        }
        int deleted = battleRepository.deleteByUserIdAndBattleId(userId, request.getBattleId());
        if (deleted == 0) {
            throw ErrorCode.BATTLE_NOT_FOUND.exception();
        }
        ClearRewardResult reward =
                stageRewardService.calculateClearReward(
                        userId,
                        request.getStageId(),
                        request.getClearedWave(),
                        request.getRating());
        if (reward.exp() < 0
                || reward.threeStarBonusExp() < 0
                || reward.threeStarBonusExp() > reward.exp()
                || (request.getRating() != 3 && reward.threeStarBonusExp() != 0)
                || reward.gold() < 0
                || reward.items() == null
                || reward.items().entrySet().stream()
                        .anyMatch(
                                item ->
                                        item.getKey() == null
                                                || item.getValue() == null
                                                || item.getValue() <= 0)) {
            throw ErrorCode.BATTLE_REWARD_UNAVAILABLE.exception();
        }
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
        BattleResult result =
                results.saveAndFlush(
                        new BattleResult(
                                userId,
                                request,
                                reward,
                                progress.getClearRating(),
                                clock.instant().truncatedTo(ChronoUnit.MICROS)));
        return BattleResultResponse.from(result);
    }

    private void lockUser(Long userId) {
        userRepository
                .findByIdForUpdate(userId)
                .orElseThrow(() -> ErrorCode.ACCOUNT_DELETED.exception());
    }
}
