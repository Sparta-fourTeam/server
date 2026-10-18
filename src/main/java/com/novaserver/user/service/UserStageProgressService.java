package com.novaserver.user.service;

import com.novaserver.global.error.ErrorCode;
import com.novaserver.item.entity.Item;
import com.novaserver.item.repository.ItemRepository;
import com.novaserver.stage.repository.StageRatingRewardRepository;
import com.novaserver.user.dto.StageRatingClaimResponse;
import com.novaserver.user.entity.StageRatingClaim;
import com.novaserver.user.entity.UserStageProgress;
import com.novaserver.user.repository.StageRatingClaimRepository;
import com.novaserver.user.repository.UserRepository;
import com.novaserver.user.repository.UserStageProgressRepository;
import java.time.Clock;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

/** 유저 스테이지 진행도·최초 별등급 보상 수령 관련 API. */
@Service
@RequiredArgsConstructor
public class UserStageProgressService {
    private final UserStageProgressRepository userStageProgressRepository;
    private final StageRatingRewardRepository stageRatingRewardRepository;
    private final UserRewardService userRewardService;
    private final UserRepository users;
    private final StageRatingClaimRepository claims;
    private final ItemRepository catalog;
    private final Clock clock;

    /** 미수령 별 구간만 원자적으로 지급하고 같은 요청 키는 최초 영수증을 반환한다. */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public StageRatingClaimResponse claimRatingReward(
            Long userId, Integer stageId, String createId, int targetRating) {
        users.findByIdForUpdate(userId).orElseThrow(ErrorCode.ACCOUNT_DELETED::exception);
        var prior = claims.findByUserIdAndCreateId(userId, createId);
        if (prior.isPresent()) {
            if (!prior.get().getStageId().equals(stageId)
                    || prior.get().getClaimedRating() != targetRating) {
                throw ErrorCode.STAGE_RATING_CLAIM_CONFLICT.exception();
            }
            return StageRatingClaimResponse.from(prior.get());
        }
        UserStageProgress progress =
                userStageProgressRepository
                        .findByUserIdAndStageIdForUpdate(userId, stageId)
                        .orElseThrow(() -> ErrorCode.STAGE_PROGRESS_NOT_FOUND.exception(stageId));
        int previous = progress.getClaimedRating();
        int reached = targetRating;
        if (reached < 1 || reached > 3 || reached > progress.getClearRating()) {
            throw ErrorCode.STAGE_RATING_CLAIM_TARGET_INVALID.exception();
        }
        if (previous >= reached) {
            throw ErrorCode.STAGE_RATING_REWARD_NOTHING_TO_CLAIM.exception(stageId);
        }
        if (previous < 0 || reached > 3) {
            throw ErrorCode.STAGE_RATING_REWARD_UNAVAILABLE.exception();
        }
        var rewards =
                stageRatingRewardRepository.findByStageIdAndRatingBetween(
                        stageId, previous + 1, reached);
        Map<String, Integer> items = new HashMap<>();
        var seen = new java.util.HashSet<Integer>();
        int gold = 0;
        try {
            for (var reward : rewards) {
                if (reward.getRating() <= previous
                        || reward.getRating() > reached
                        || !seen.add(reward.getRating())
                        || reward.getGold() < 0) {
                    throw ErrorCode.STAGE_RATING_REWARD_UNAVAILABLE.exception();
                }
                gold = Math.addExact(gold, reward.getGold());
                for (var item : reward.getItems().entrySet()) {
                    if (item.getKey() == null
                            || item.getKey().isBlank()
                            || item.getValue() == null
                            || item.getValue() <= 0) {
                        throw ErrorCode.STAGE_RATING_REWARD_UNAVAILABLE.exception();
                    }
                    items.merge(item.getKey(), item.getValue(), Math::addExact);
                }
            }
        } catch (ArithmeticException error) {
            throw ErrorCode.STAGE_RATING_REWARD_UNAVAILABLE.exception();
        }
        if (seen.size() != reached - previous) {
            throw ErrorCode.STAGE_RATING_REWARD_UNAVAILABLE.exception();
        }
        var actualIds =
                catalog.findAllById(items.keySet()).stream()
                        .map(Item::getItemId)
                        .collect(Collectors.toSet());
        for (String id : items.keySet()) {
            if (!actualIds.contains(id)) {
                throw ErrorCode.ITEM_NOT_FOUND.exception(id);
            }
        }
        userRewardService.grant(userId, 0, gold, items);
        progress.updateClaimedRating(reached);
        return StageRatingClaimResponse.from(
                claims.saveAndFlush(
                        new StageRatingClaim(
                                userId,
                                createId,
                                stageId,
                                previous,
                                progress.getClearRating(),
                                reached,
                                gold,
                                items,
                                clock.instant().truncatedTo(ChronoUnit.MICROS))));
    }
}
