package com.novaserver.user.service;

import com.novaserver.global.error.ErrorCode;
import com.novaserver.stage.entity.StageRatingReward;
import com.novaserver.stage.entity.StageRewardItem;
import com.novaserver.stage.repository.StageRatingRewardRepository;
import com.novaserver.user.dto.UserStageProgressResponse;
import com.novaserver.user.entity.UserStageProgress;
import com.novaserver.user.repository.UserStageProgressRepository;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 유저 스테이지 진행도·별등급 보상 수령 관련 API. */
@Service
@RequiredArgsConstructor
public class UserStageProgressService {
    private final UserStageProgressRepository userStageProgressRepository;
    private final StageRatingRewardRepository stageRatingRewardRepository;
    private final UserRewardService userRewardService;

    /**
     * 아직 받지 못한 별등급 최초 보상을 수령한다. clearRating까지 중 claimedRating보다 높은 등급의 보상만 지급하고, claimedRating을
     * clearRating까지 올린다.
     *
     * @param userId 유저 id
     * @param stageId 스테이지 id
     */
    @Transactional
    public UserStageProgressResponse claimRatingReward(Long userId, Integer stageId) {
        UserStageProgress progress =
                userStageProgressRepository
                        .findByUserIdAndStageId(userId, stageId)
                        .orElseThrow(() -> ErrorCode.STAGE_PROGRESS_NOT_FOUND.exception(stageId));

        if (progress.getClaimedRating() >= progress.getClearRating()) {
            throw ErrorCode.STAGE_RATING_REWARD_NOTHING_TO_CLAIM.exception(stageId);
        }

        List<StageRatingReward> rewards =
                stageRatingRewardRepository.findByStageIdAndRatingBetween(
                        stageId, progress.getClaimedRating() + 1, progress.getClearRating());

        int exp = rewards.stream().mapToInt(StageRatingReward::getExp).sum();
        int gold = rewards.stream().mapToInt(StageRatingReward::getGold).sum();
        Map<String, Integer> items = new HashMap<>();
        for (StageRatingReward reward : rewards) {
            for (StageRewardItem item : reward.getItems()) {
                items.merge(item.itemId(), item.finalAmount(), Integer::sum);
            }
        }

        userRewardService.grant(userId, exp, gold, items);
        progress.updateClaimedRating(progress.getClearRating());

        return UserStageProgressResponse.from(progress);
    }
}
