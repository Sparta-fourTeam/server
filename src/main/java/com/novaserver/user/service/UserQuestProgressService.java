package com.novaserver.user.service;

import com.novaserver.global.error.ErrorCode;
import com.novaserver.quest.entity.Quest;
import com.novaserver.quest.entity.QuestKind;
import com.novaserver.quest.entity.QuestPeriod;
import com.novaserver.quest.entity.QuestReward;
import com.novaserver.quest.repository.QuestRepository;
import com.novaserver.quest.repository.QuestRewardRepository;
import com.novaserver.user.dto.QuestRewardClaimResponse;
import com.novaserver.user.dto.UserQuestPeriodResponse;
import com.novaserver.user.dto.UserQuestResponse;
import com.novaserver.user.dto.UserQuestRewardResponse;
import com.novaserver.user.entity.User;
import com.novaserver.user.entity.UserQuestPoint;
import com.novaserver.user.entity.UserQuestProgress;
import com.novaserver.user.repository.UserQuestPointRepository;
import com.novaserver.user.repository.UserQuestProgressRepository;
import com.novaserver.user.repository.UserRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 유저 퀘스트 진행도·누적 포인트·보상 수령 관련 API. */
@Service
@RequiredArgsConstructor
public class UserQuestProgressService {
    private final UserRepository userRepository;
    private final UserQuestProgressRepository userQuestProgressRepository;
    private final UserQuestPointRepository userQuestPointRepository;
    private final QuestRepository questRepository;
    private final QuestRewardRepository questRewardRepository;
    private final UserRewardService userRewardService;
    private final UserService userService;
    private final Clock clock;

    /** 일일·주간 퀘스트 진행도와 보상 수령 가능 여부를 조회한다. 지난 주기 데이터는 화면 표시용으로만 리셋해 보여준다. */
    @Transactional(readOnly = true)
    public List<UserQuestPeriodResponse> getMyQuestStatus(Long userId) {
        LocalDate today = LocalDate.now(clock);

        Map<String, UserQuestProgress> progressByQuestId =
                userQuestProgressRepository.findByUserId(userId).stream()
                        .collect(Collectors.toMap(p -> p.getQuest().getQuestId(), p -> p));
        Map<QuestPeriod, UserQuestPoint> pointByPeriod =
                userQuestPointRepository.findByUserId(userId).stream()
                        .collect(Collectors.toMap(UserQuestPoint::getPeriod, p -> p));

        return Arrays.stream(QuestPeriod.values())
                .map(period -> buildPeriodStatus(period, today, progressByQuestId, pointByPeriod))
                .toList();
    }

    /**
     * 퀘스트 진행도를 올리고, target에 도달하면 완료 처리한다. 완료만으로는 포인트가 쌓이지 않고, {@link #claimQuestReward}로 따로 수령해야
     * 한다. 로그인·배틀·에너지 소모 등 다른 도메인에서 이벤트가 발생할 때 호출하는 진입점이다.
     *
     * @param userId 유저 id
     * @param kind 진행시킬 퀘스트 종류
     * @param amount 올릴 진행도
     */
    @Transactional
    public void increaseProgress(Long userId, QuestKind kind, int amount) {
        if (amount <= 0) {
            return;
        }

        List<Quest> quests = questRepository.findByKind(kind);
        if (quests.isEmpty()) {
            return;
        }

        LocalDate today = LocalDate.now(clock);
        List<String> questIds = quests.stream().map(Quest::getQuestId).toList();
        Map<String, UserQuestProgress> progressByQuestId =
                userQuestProgressRepository
                        .findByUserIdAndQuestIdInForUpdate(userId, questIds)
                        .stream()
                        .collect(Collectors.toMap(p -> p.getQuest().getQuestId(), p -> p));

        for (Quest quest : quests) {
            LocalDate periodKey = quest.getPeriod().currentPeriodKey(today);

            UserQuestProgress progress =
                    progressByQuestId.computeIfAbsent(
                            quest.getQuestId(), id -> createProgress(userId, quest, periodKey));
            progress.resetIfStale(periodKey);
            progress.increase(amount, quest.getTarget());
        }
    }

    /** 완료한 퀘스트의 보상을 수령한다. 진행도가 없으면 404, 아직 완료하지 않았거나 이미 수령했으면 409를 던진다. */
    @Transactional
    public UserQuestResponse claimQuestReward(Long userId, String questId) {
        UserQuestProgress progress =
                userQuestProgressRepository
                        .findByUserIdAndQuestIdForUpdate(userId, questId)
                        .orElseThrow(() -> ErrorCode.QUEST_PROGRESS_NOT_FOUND.exception(questId));

        Quest quest = progress.getQuest();
        LocalDate periodKey = quest.getPeriod().currentPeriodKey(LocalDate.now(clock));
        progress.resetIfStale(periodKey);

        if (!progress.isCompleted()) {
            throw ErrorCode.QUEST_NOT_COMPLETED.exception(questId);
        }
        if (progress.isClaimed()) {
            throw ErrorCode.QUEST_REWARD_ALREADY_CLAIMED.exception(questId);
        }

        progress.claim();

        UserQuestPoint point = findOrCreatePoint(userId, quest.getPeriod(), periodKey);
        point.resetIfStale(periodKey);
        point.addPoints(quest.getPoints());

        return UserQuestResponse.from(
                quest, progress.getCount(), progress.isCompleted(), progress.isClaimed());
    }

    /** 아직 받지 못한 보상을 모두 수령한다. 받을 보상이 없으면 409를 던진다. */
    @Transactional
    public QuestRewardClaimResponse claimRewards(Long userId, QuestPeriod period) {
        LocalDate periodKey = period.currentPeriodKey(LocalDate.now(clock));

        UserQuestPoint point =
                userQuestPointRepository
                        .findByUserIdAndPeriodForUpdate(userId, period)
                        .orElseThrow(
                                () -> ErrorCode.QUEST_REWARD_NOTHING_TO_CLAIM.exception(period));
        point.resetIfStale(periodKey);

        if (point.getClaimedPoints() >= point.getPoints()) {
            throw ErrorCode.QUEST_REWARD_NOTHING_TO_CLAIM.exception(period);
        }

        List<QuestReward> rewards =
                questRewardRepository.findByPeriodAndPointsBetween(
                        period, point.getClaimedPoints() + 1, point.getPoints());

        int gold = rewards.stream().mapToInt(QuestReward::getGold).sum();
        int energy = rewards.stream().mapToInt(QuestReward::getEnergy).sum();

        if (gold > 0) {
            userRewardService.grant(userId, 0, gold, null);
        }
        if (energy > 0) {
            userService.grantEnergy(userId, energy);
        }

        point.updateClaimedPoints(point.getPoints());

        return new QuestRewardClaimResponse(
                period, point.getPoints(), point.getClaimedPoints(), gold, energy);
    }

    private UserQuestPeriodResponse buildPeriodStatus(
            QuestPeriod period,
            LocalDate today,
            Map<String, UserQuestProgress> progressByQuestId,
            Map<QuestPeriod, UserQuestPoint> pointByPeriod) {
        LocalDate periodKey = period.currentPeriodKey(today);

        UserQuestPoint point = pointByPeriod.get(period);
        if (point != null) {
            point.resetIfStale(periodKey);
        }
        int points = point == null ? 0 : point.getPoints();
        int claimedPoints = point == null ? 0 : point.getClaimedPoints();

        List<UserQuestResponse> quests =
                questRepository.findByPeriodOrderByQuestId(period).stream()
                        .map(
                                quest -> {
                                    UserQuestProgress progress =
                                            progressByQuestId.get(quest.getQuestId());
                                    if (progress != null) {
                                        progress.resetIfStale(periodKey);
                                    }
                                    int count = progress == null ? 0 : progress.getCount();
                                    boolean completed = progress != null && progress.isCompleted();
                                    boolean claimed = progress != null && progress.isClaimed();
                                    return UserQuestResponse.from(quest, count, completed, claimed);
                                })
                        .toList();

        List<UserQuestRewardResponse> rewards =
                questRewardRepository.findByPeriodOrderByPoints(period).stream()
                        .map(
                                reward ->
                                        UserQuestRewardResponse.from(
                                                reward,
                                                points >= reward.getPoints(),
                                                claimedPoints >= reward.getPoints()))
                        .toList();

        return new UserQuestPeriodResponse(period, points, claimedPoints, quests, rewards);
    }

    private UserQuestProgress createProgress(Long userId, Quest quest, LocalDate periodKey) {
        User user = userRepository.getReferenceById(userId);
        try {
            return userQuestProgressRepository.save(new UserQuestProgress(user, quest, periodKey));
        } catch (DataIntegrityViolationException e) {
            return userQuestProgressRepository
                    .findByUserIdAndQuestIdForUpdate(userId, quest.getQuestId())
                    .orElseThrow(() -> e);
        }
    }

    private UserQuestPoint findOrCreatePoint(Long userId, QuestPeriod period, LocalDate periodKey) {
        return userQuestPointRepository
                .findByUserIdAndPeriodForUpdate(userId, period)
                .orElseGet(
                        () -> {
                            User user = userRepository.getReferenceById(userId);
                            try {
                                return userQuestPointRepository.save(
                                        new UserQuestPoint(user, period, periodKey));
                            } catch (DataIntegrityViolationException e) {
                                return userQuestPointRepository
                                        .findByUserIdAndPeriodForUpdate(userId, period)
                                        .orElseThrow(() -> e);
                            }
                        });
    }
}
