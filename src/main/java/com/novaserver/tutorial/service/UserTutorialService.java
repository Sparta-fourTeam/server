package com.novaserver.tutorial.service;

import com.novaserver.global.error.ErrorCode;
import com.novaserver.playerlevel.dto.PlayerLevelConfigResponse;
import com.novaserver.playerlevel.service.PlayerLevelService;
import com.novaserver.stage.service.RewardRulesService;
import com.novaserver.tutorial.dto.TutorialReceiptResponse;
import com.novaserver.tutorial.dto.TutorialStateResponse;
import com.novaserver.tutorial.entity.Tutorials;
import com.novaserver.tutorial.repository.TutorialRepository;
import com.novaserver.user.entity.UserData;
import com.novaserver.user.entity.UserTutorial;
import com.novaserver.user.repository.UserDataRepository;
import com.novaserver.user.repository.UserRepository;
import com.novaserver.user.repository.UserStageProgressRepository;
import com.novaserver.user.repository.UserTutorialRepository;
import com.novaserver.user.repository.UserUpgradeRepository;
import com.novaserver.user.service.UserRewardService;
import java.time.Clock;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

/** 서버에 저장된 조건으로 튜토리얼을 검증하고 계정별 최초 영수증을 보존한다. */
@Service
@RequiredArgsConstructor
public class UserTutorialService {
    private final TutorialRepository tutorials;
    private final UserTutorialRepository states;
    private final UserRepository users;
    private final UserDataRepository data;
    private final UserStageProgressRepository stages;
    private final UserUpgradeRepository upgrades;
    private final PlayerLevelService levels;
    private final UserRewardService rewards;
    private final Clock clock;
    private final RewardRulesService referenceLocks;

    @Transactional(readOnly = true)
    public List<TutorialStateResponse> list(Long userId) {
        return states.findByUserId(userId).stream().map(TutorialStateResponse::from).toList();
    }

    /** 계정+튜토리얼 ID가 멱등 키다. 이미 확정했으면 현재 설정을 읽기 전에 영수증을 반환한다. */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public TutorialReceiptResponse complete(Long userId, String recordId) {
        if (recordId == null || !recordId.matches("[A-Za-z0-9_-]{1,64}(\\.dialogue)?")) {
            throw ErrorCode.TUTORIAL_INVALID.exception();
        }
        var user =
                users.findByIdForUpdate(userId).orElseThrow(ErrorCode.ACCOUNT_DELETED::exception);
        boolean dialogue = recordId.endsWith(".dialogue");
        String id = dialogue ? recordId.substring(0, recordId.length() - 9) : recordId;
        var previous = states.findByUserIdAndTutorialsId(userId, id);
        if (previous.isPresent()
                && (dialogue
                        ? previous.get().getDialogueSeenAt() != null
                        : previous.get().getCompletedAt() != null)) {
            return TutorialReceiptResponse.from(previous.get(), dialogue);
        }
        // 기존 상태 행은 FK로 정의 삭제를 막는다. 첫 생성은 정의 삭제와 같은 잠금으로 정렬한다.
        if (previous.isEmpty()) {
            referenceLocks.lockItemReferences();
        }
        // 조건과 실제 보상 항목을 동일 SELECT에서 읽어 공유 편집의 중간값을 섞지 않는다.
        Tutorials definition =
                tutorials
                        .findById(id)
                        .orElseThrow(() -> ErrorCode.TUTORIAL_NOT_FOUND.exception(id));
        var row = previous.orElseGet(() -> new UserTutorial(user, definition));
        if (dialogue) {
            row.recordDialogue(clock.instant());
        } else {
            UserData account =
                    data.findByUserIdForUpdate(userId)
                            .orElseThrow(() -> ErrorCode.USER_DATA_NOT_FOUND.exception(userId));
            int before = account.getExp();
            var config =
                    definition.getMinLevel() > 1
                                    || definition.getPlayerLevel() > 1
                                    || definition.getSetLevel() > 1
                            ? levels.getConfig()
                            : null;
            checkReady(userId, definition, before, config);
            int after =
                    definition.getSetLevel() == 0
                            ? before
                            : threshold(config, definition.getSetLevel());
            var reward = definition.getRewardDefinition();
            var items = reward.itemAmounts();
            // setLevel은 Local과 동일하게 정확한 레벨 시작점으로 맞춘다. 일반 보상 EXP와 분리한다.
            account.addExp(after - before);
            rewards.grant(userId, 0, reward.getGold(), items);
            row.complete(reward.getGold(), items, before, after, clock.instant());
        }
        states.saveAndFlush(row);
        return TutorialReceiptResponse.from(row, dialogue);
    }

    private void checkReady(Long userId, Tutorials t, int exp, PlayerLevelConfigResponse config) {
        if ((t.getPrerequisite() != null
                        && states.findByUserIdAndTutorialsId(userId, t.getPrerequisite().getId())
                                .filter(s -> s.getCompletedAt() != null)
                                .isEmpty())
                || exp < threshold(config, t.getMinLevel())
                || (t.getStartStage() != null && !cleared(userId, t.getStartStage().getStageId()))
                || (t.getClearStage() != null && !cleared(userId, t.getClearStage().getStageId()))
                || (t.getRequiredUpgrade() != null
                        && upgrades.findByUserIdAndUpgradeId(
                                        userId, t.getRequiredUpgrade().getUpgradeId())
                                .map(u -> u.getLevel() < t.getUpgradeLevel())
                                .orElse(true))
                || (t.getPlayerLevel() > 0 && exp < threshold(config, t.getPlayerLevel()))) {
            throw ErrorCode.TUTORIAL_NOT_READY.exception();
        }
    }

    private boolean cleared(Long userId, Integer stageId) {
        return stages.findByUserIdAndStageId(userId, stageId)
                .map(s -> s.getClearRating() > 0)
                .orElse(false);
    }

    private int threshold(PlayerLevelConfigResponse config, int level) {
        if (level <= 1) {
            return 0;
        }
        if (config == null || level > config.maxLevel()) {
            throw ErrorCode.TUTORIAL_UNAVAILABLE.exception();
        }
        long count = level - 1L;
        return Math.toIntExact(
                count * config.baseExp() + count * (count - 1) / 2 * config.stepExp());
    }
}
