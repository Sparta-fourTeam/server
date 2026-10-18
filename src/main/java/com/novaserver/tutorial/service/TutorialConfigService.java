package com.novaserver.tutorial.service;

import com.novaserver.global.error.ErrorCode;
import com.novaserver.playerlevel.service.PlayerLevelService;
import com.novaserver.reward.service.RewardDefinitionService;
import com.novaserver.stage.entity.Stage;
import com.novaserver.stage.repository.StageRepository;
import com.novaserver.stage.service.RewardRulesService;
import com.novaserver.tutorial.dto.TutorialConfigRequest;
import com.novaserver.tutorial.dto.TutorialConfigResponse;
import com.novaserver.tutorial.entity.Tutorials;
import com.novaserver.tutorial.repository.TutorialRepository;
import com.novaserver.upgrade.entity.Upgrade;
import com.novaserver.upgrade.repository.UpgradeRepository;
import com.novaserver.user.repository.UserTutorialRepository;
import java.util.HashSet;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 튜토리얼 서버 조건과 보상 참조를 관리한다. 출시 데이터는 자동으로 생성하지 않는다. */
@Service
@RequiredArgsConstructor
public class TutorialConfigService {
    private final TutorialRepository tutorials;
    private final StageRepository stages;
    private final UpgradeRepository upgrades;
    private final RewardDefinitionService rewards;
    private final RewardRulesService locks;
    private final PlayerLevelService levels;
    private final UserTutorialRepository states;

    @Transactional(readOnly = true)
    public List<TutorialConfigResponse> list() {
        return tutorials.findAll().stream().map(TutorialConfigResponse::from).toList();
    }

    /** 선행 순환·참조·레벨을 검증하고 전체 조건을 원자적으로 등록·교체한다. */
    @Transactional
    public TutorialConfigResponse save(String id, TutorialConfigRequest r) {
        locks.lockItemReferences();
        if (id == null
                || !id.matches("[A-Za-z0-9_-]{1,64}")
                || r == null
                || r.minLevel() < 1
                || r.playerLevel() < 0
                || r.setLevel() < 0
                || r.upgradeLevel() < 0
                || r.rewardDefinitionId() == null
                || r.rewardDefinitionId() < 1) {
            throw ErrorCode.TUTORIAL_INVALID.exception();
        }
        int conditions =
                (r.clearStageId() == null ? 0 : 1)
                        + (r.upgradeId() == null ? 0 : 1)
                        + (r.playerLevel() == 0 ? 0 : 1);
        if (conditions != 1 || (r.upgradeId() == null) != (r.upgradeLevel() == 0)) {
            throw ErrorCode.TUTORIAL_INVALID.exception();
        }
        int maxRequested = Math.max(r.minLevel(), Math.max(r.playerLevel(), r.setLevel()));
        if (maxRequested > 1 && maxRequested > levels.getConfig().maxLevel()) {
            throw ErrorCode.TUTORIAL_INVALID.exception();
        }
        Tutorials prerequisite = r.prerequisiteId() == null ? null : find(r.prerequisiteId());
        var visited = new HashSet<String>();
        for (var prior = prerequisite; prior != null; prior = prior.getPrerequisite()) {
            if (id.equals(prior.getId()) || !visited.add(prior.getId())) {
                throw ErrorCode.TUTORIAL_INVALID.exception();
            }
        }
        Stage start = stage(r.startStageId());
        Stage clear = stage(r.clearStageId());
        Upgrade upgrade =
                r.upgradeId() == null
                        ? null
                        : upgrades.findById(r.upgradeId())
                                .orElseThrow(
                                        () -> ErrorCode.UPGRADE_NOT_FOUND.exception(r.upgradeId()));
        if (upgrade != null && r.upgradeLevel() > upgrade.getMaxLevel()) {
            throw ErrorCode.TUTORIAL_INVALID.exception();
        }
        var reward = rewards.findDefinition(r.rewardDefinitionId());
        var definition = tutorials.findById(id).orElseGet(() -> new Tutorials(id));
        definition.configure(
                prerequisite,
                r.minLevel(),
                start,
                clear,
                upgrade,
                r.upgradeLevel(),
                r.playerLevel(),
                r.setLevel(),
                reward);
        return TutorialConfigResponse.from(tutorials.saveAndFlush(definition));
    }

    /** 계정 기록이나 후행 튜토리얼이 참조하면 정의 삭제를 거절한다. */
    @Transactional
    public void delete(String id) {
        locks.lockItemReferences();
        var definition = find(id);
        if (states.existsByTutorialsId(id) || tutorials.existsByPrerequisiteId(id)) {
            throw ErrorCode.TUTORIAL_IN_USE.exception();
        }
        tutorials.delete(definition);
        tutorials.flush();
    }

    private Tutorials find(String id) {
        return tutorials.findById(id).orElseThrow(() -> ErrorCode.TUTORIAL_NOT_FOUND.exception(id));
    }

    private Stage stage(Integer id) {
        return id == null
                ? null
                : stages.findById(id).orElseThrow(() -> ErrorCode.STAGE_NOT_FOUND.exception(id));
    }
}
