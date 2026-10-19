package com.novaserver.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.novaserver.quest.entity.Quest;
import com.novaserver.quest.entity.QuestKind;
import com.novaserver.quest.entity.QuestPeriod;
import com.novaserver.quest.entity.QuestReward;
import com.novaserver.quest.repository.QuestRepository;
import com.novaserver.quest.repository.QuestRewardRepository;
import com.novaserver.user.dto.QuestRewardClaimResponse;
import com.novaserver.user.dto.UserQuestPeriodResponse;
import com.novaserver.user.entity.User;
import com.novaserver.user.entity.UserQuestPoint;
import com.novaserver.user.entity.UserQuestProgress;
import com.novaserver.user.repository.UserQuestPointRepository;
import com.novaserver.user.repository.UserQuestProgressRepository;
import com.novaserver.user.repository.UserRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class UserQuestProgressServiceTest {

    private static final Long USER_ID = 1L;
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 14, 12, 0);
    private static final LocalDate TODAY = NOW.toLocalDate();

    @Mock private UserRepository userRepository;
    @Mock private UserQuestProgressRepository userQuestProgressRepository;
    @Mock private UserQuestPointRepository userQuestPointRepository;
    @Mock private QuestRepository questRepository;
    @Mock private QuestRewardRepository questRewardRepository;
    @Mock private UserRewardService userRewardService;
    @Mock private UserService userService;

    private UserQuestProgressService userQuestProgressService;

    @BeforeEach
    void setUp() {
        ZoneId zone = ZoneId.systemDefault();
        Clock clock = Clock.fixed(NOW.atZone(zone).toInstant(), zone);

        userQuestProgressService =
                new UserQuestProgressService(
                        userRepository,
                        userQuestProgressRepository,
                        userQuestPointRepository,
                        questRepository,
                        questRewardRepository,
                        userRewardService,
                        userService,
                        clock);
    }

    @Test
    @DisplayName("증가량이 0 이하면 아무 것도 하지 않는다")
    void shouldDoNothing_whenAmountNotPositive() {
        userQuestProgressService.increaseProgress(USER_ID, QuestKind.LOGIN, 0);

        verifyNoInteractions(
                questRepository, userQuestProgressRepository, userQuestPointRepository);
    }

    @Test
    @DisplayName("진행도가 없으면 새로 만들고, 목표를 채우면 완료 처리한다. 포인트는 따로 수령해야 쌓인다")
    void shouldCreateProgressAndMarkCompleted_whenTargetReachedFirstTime() {
        Quest quest = new Quest("daily.login", QuestPeriod.DAILY, QuestKind.LOGIN, 1, 40);
        when(questRepository.findByKind(QuestKind.LOGIN)).thenReturn(List.of(quest));

        when(userQuestProgressRepository.findByUserIdAndQuestIdInForUpdate(
                        USER_ID, List.of("daily.login")))
                .thenReturn(List.of());
        User user = new User(null, null);
        when(userRepository.getReferenceById(USER_ID)).thenReturn(user);
        UserQuestProgress progress = new UserQuestProgress(user, quest, TODAY);
        when(userQuestProgressRepository.save(org.mockito.ArgumentMatchers.any()))
                .thenReturn(progress);

        userQuestProgressService.increaseProgress(USER_ID, QuestKind.LOGIN, 1);

        assertThat(progress.getCount()).isEqualTo(1);
        assertThat(progress.isCompleted()).isTrue();
        assertThat(progress.isClaimed()).isFalse();
        verifyNoInteractions(userQuestPointRepository);
    }

    @Test
    @DisplayName("이미 완료한 퀘스트는 진행도를 더 올리지 않는다")
    void shouldNotIncreaseFurther_whenQuestAlreadyCompleted() {
        Quest quest = new Quest("daily.login", QuestPeriod.DAILY, QuestKind.LOGIN, 1, 40);
        when(questRepository.findByKind(QuestKind.LOGIN)).thenReturn(List.of(quest));

        UserQuestProgress progress = new UserQuestProgress(null, quest, TODAY);
        progress.increase(1, 1);
        when(userQuestProgressRepository.findByUserIdAndQuestIdInForUpdate(
                        USER_ID, List.of("daily.login")))
                .thenReturn(List.of(progress));

        userQuestProgressService.increaseProgress(USER_ID, QuestKind.LOGIN, 1);

        assertThat(progress.getCount()).isEqualTo(1);
        verifyNoInteractions(userQuestPointRepository);
    }

    @Test
    @DisplayName("진행도가 없으면 404 QUEST_PROGRESS_NOT_FOUND를 던진다")
    void shouldThrowNotFound_whenClaimingQuestWithoutProgress() {
        when(userQuestProgressRepository.findByUserIdAndQuestIdForUpdate(USER_ID, "daily.login"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> userQuestProgressService.claimQuestReward(USER_ID, "daily.login"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("퀘스트 진행도가 없습니다");
        verifyNoInteractions(userQuestPointRepository);
    }

    @Test
    @DisplayName("아직 완료하지 않은 퀘스트를 수령하려 하면 409 QUEST_NOT_COMPLETED를 던진다")
    void shouldThrowConflict_whenClaimingIncompleteQuest() {
        Quest quest = new Quest("daily.login", QuestPeriod.DAILY, QuestKind.LOGIN, 2, 40);
        UserQuestProgress progress = new UserQuestProgress(null, quest, TODAY);
        progress.increase(1, 2);
        when(userQuestProgressRepository.findByUserIdAndQuestIdForUpdate(USER_ID, "daily.login"))
                .thenReturn(Optional.of(progress));

        assertThatThrownBy(() -> userQuestProgressService.claimQuestReward(USER_ID, "daily.login"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("아직 완료하지 않은 퀘스트입니다");
        verifyNoInteractions(userQuestPointRepository);
    }

    @Test
    @DisplayName("이미 수령한 퀘스트를 다시 수령하려 하면 409 QUEST_REWARD_ALREADY_CLAIMED를 던진다")
    void shouldThrowConflict_whenClaimingAlreadyClaimedQuest() {
        Quest quest = new Quest("daily.login", QuestPeriod.DAILY, QuestKind.LOGIN, 1, 40);
        UserQuestProgress progress = new UserQuestProgress(null, quest, TODAY);
        progress.increase(1, 1);
        progress.claim();
        when(userQuestProgressRepository.findByUserIdAndQuestIdForUpdate(USER_ID, "daily.login"))
                .thenReturn(Optional.of(progress));

        assertThatThrownBy(() -> userQuestProgressService.claimQuestReward(USER_ID, "daily.login"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("이미 보상을 수령한 퀘스트입니다");
        verifyNoInteractions(userQuestPointRepository);
    }

    @Test
    @DisplayName("완료하고 아직 수령하지 않은 퀘스트는 수령 처리하고 주기 포인트를 올린다")
    void shouldClaimCompletedQuestAndAddPoints() {
        Quest quest = new Quest("daily.login", QuestPeriod.DAILY, QuestKind.LOGIN, 1, 40);
        UserQuestProgress progress = new UserQuestProgress(null, quest, TODAY);
        progress.increase(1, 1);
        when(userQuestProgressRepository.findByUserIdAndQuestIdForUpdate(USER_ID, "daily.login"))
                .thenReturn(Optional.of(progress));

        UserQuestPoint point = new UserQuestPoint(null, QuestPeriod.DAILY, TODAY);
        when(userQuestPointRepository.findByUserIdAndPeriodForUpdate(USER_ID, QuestPeriod.DAILY))
                .thenReturn(Optional.of(point));

        var response = userQuestProgressService.claimQuestReward(USER_ID, "daily.login");

        assertThat(progress.isClaimed()).isTrue();
        assertThat(point.getPoints()).isEqualTo(40);
        assertThat(response.claimed()).isTrue();
        assertThat(response.completed()).isTrue();
    }

    @Test
    @DisplayName("수령 시 주기 포인트 행이 없으면 새로 만들어서 포인트를 올린다")
    void shouldCreatePointRow_whenClaimingWithNoExistingPointRow() {
        Quest quest = new Quest("daily.login", QuestPeriod.DAILY, QuestKind.LOGIN, 1, 40);
        UserQuestProgress progress = new UserQuestProgress(null, quest, TODAY);
        progress.increase(1, 1);
        when(userQuestProgressRepository.findByUserIdAndQuestIdForUpdate(USER_ID, "daily.login"))
                .thenReturn(Optional.of(progress));

        when(userQuestPointRepository.findByUserIdAndPeriodForUpdate(USER_ID, QuestPeriod.DAILY))
                .thenReturn(Optional.empty());
        User user = new User(null, null);
        when(userRepository.getReferenceById(USER_ID)).thenReturn(user);
        UserQuestPoint point = new UserQuestPoint(user, QuestPeriod.DAILY, TODAY);
        when(userQuestPointRepository.save(any())).thenReturn(point);

        userQuestProgressService.claimQuestReward(USER_ID, "daily.login");

        assertThat(point.getPoints()).isEqualTo(40);
    }

    @Test
    @DisplayName("같은 종류의 퀘스트가 여러 개면 한 번의 배치 조회로 각각 진행도를 갱신한다")
    void shouldHandleMultipleQuestsOfSameKind_inSingleBatchLookup() {
        Quest dailyQuest =
                new Quest("daily.kill", QuestPeriod.DAILY, QuestKind.KILL_MONSTERS, 10, 20);
        Quest weeklyQuest =
                new Quest("weekly.kill", QuestPeriod.WEEKLY, QuestKind.KILL_MONSTERS, 100, 40);
        when(questRepository.findByKind(QuestKind.KILL_MONSTERS))
                .thenReturn(List.of(dailyQuest, weeklyQuest));

        UserQuestProgress existingDaily = new UserQuestProgress(null, dailyQuest, TODAY);
        when(userQuestProgressRepository.findByUserIdAndQuestIdInForUpdate(
                        USER_ID, List.of("daily.kill", "weekly.kill")))
                .thenReturn(List.of(existingDaily));

        User user = new User(null, null);
        when(userRepository.getReferenceById(USER_ID)).thenReturn(user);
        UserQuestProgress newWeekly = new UserQuestProgress(user, weeklyQuest, TODAY);
        when(userQuestProgressRepository.save(any())).thenReturn(newWeekly);

        userQuestProgressService.increaseProgress(USER_ID, QuestKind.KILL_MONSTERS, 5);

        assertThat(existingDaily.getCount()).isEqualTo(5);
        assertThat(newWeekly.getCount()).isEqualTo(5);
        verify(userQuestProgressRepository, times(1))
                .findByUserIdAndQuestIdInForUpdate(USER_ID, List.of("daily.kill", "weekly.kill"));
        verifyNoInteractions(userQuestPointRepository);
    }

    @Test
    @DisplayName("적립된 포인트가 없으면 409 QUEST_REWARD_NOTHING_TO_CLAIM을 던진다")
    void shouldThrowConflict_whenNoPointRow() {
        when(userQuestPointRepository.findByUserIdAndPeriodForUpdate(USER_ID, QuestPeriod.DAILY))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> userQuestProgressService.claimRewards(USER_ID, QuestPeriod.DAILY))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("받을 수 있는 보상이 없습니다");
        verifyNoInteractions(userRewardService, userService);
    }

    @Test
    @DisplayName("이미 모두 수령했으면 409 QUEST_REWARD_NOTHING_TO_CLAIM을 던진다")
    void shouldThrowConflict_whenNothingLeftToClaim() {
        UserQuestPoint point = new UserQuestPoint(null, QuestPeriod.DAILY, TODAY);
        point.addPoints(40);
        point.updateClaimedPoints(40);
        when(userQuestPointRepository.findByUserIdAndPeriodForUpdate(USER_ID, QuestPeriod.DAILY))
                .thenReturn(Optional.of(point));

        assertThatThrownBy(() -> userQuestProgressService.claimRewards(USER_ID, QuestPeriod.DAILY))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("받을 수 있는 보상이 없습니다");
        verifyNoInteractions(userRewardService, userService);
    }

    @Test
    @DisplayName("아직 받지 못한 보상을 모두 수령하고 골드·에너지를 지급한다")
    void shouldClaimUnclaimedRewardsAndGrantThem() {
        UserQuestPoint point = new UserQuestPoint(null, QuestPeriod.DAILY, TODAY);
        point.addPoints(60);
        when(userQuestPointRepository.findByUserIdAndPeriodForUpdate(USER_ID, QuestPeriod.DAILY))
                .thenReturn(Optional.of(point));

        QuestReward reward20 = new QuestReward("daily.reward.20", QuestPeriod.DAILY, 20, 300, 5);
        QuestReward reward40 = new QuestReward("daily.reward.40", QuestPeriod.DAILY, 40, 500, 5);
        when(questRewardRepository.findByPeriodAndPointsBetween(QuestPeriod.DAILY, 1, 60))
                .thenReturn(List.of(reward20, reward40));

        QuestRewardClaimResponse response =
                userQuestProgressService.claimRewards(USER_ID, QuestPeriod.DAILY);

        verify(userRewardService).grant(USER_ID, 0, 800, null);
        verify(userService).grantEnergy(USER_ID, 10);
        assertThat(point.getClaimedPoints()).isEqualTo(60);
        assertThat(response.grantedGold()).isEqualTo(800);
        assertThat(response.grantedEnergy()).isEqualTo(10);
    }

    @Test
    @DisplayName("두 번째 수령은 새로 올라간 포인트 구간의 보상만 지급한다")
    void shouldOnlyClaimNewTier_onSecondClaim() {
        UserQuestPoint point = new UserQuestPoint(null, QuestPeriod.DAILY, TODAY);
        point.addPoints(60);
        point.updateClaimedPoints(40);
        when(userQuestPointRepository.findByUserIdAndPeriodForUpdate(USER_ID, QuestPeriod.DAILY))
                .thenReturn(Optional.of(point));

        QuestReward reward60 = new QuestReward("daily.reward.60", QuestPeriod.DAILY, 60, 800, 10);
        when(questRewardRepository.findByPeriodAndPointsBetween(QuestPeriod.DAILY, 41, 60))
                .thenReturn(List.of(reward60));

        userQuestProgressService.claimRewards(USER_ID, QuestPeriod.DAILY);

        verify(userRewardService).grant(USER_ID, 0, 800, null);
        verify(userService).grantEnergy(USER_ID, 10);
        assertThat(point.getClaimedPoints()).isEqualTo(60);
    }

    @Test
    @DisplayName("퀘스트 현황 조회는 카탈로그와 진행도를 합쳐 주기별로 반환한다")
    void shouldReturnCombinedStatus_onGetMyQuestStatus() {
        Quest dailyLogin = new Quest("daily.login", QuestPeriod.DAILY, QuestKind.LOGIN, 1, 40);
        when(questRepository.findByPeriodOrderByQuestId(QuestPeriod.DAILY))
                .thenReturn(List.of(dailyLogin));
        when(questRepository.findByPeriodOrderByQuestId(QuestPeriod.WEEKLY)).thenReturn(List.of());

        QuestReward dailyReward = new QuestReward("daily.reward.20", QuestPeriod.DAILY, 20, 300, 5);
        when(questRewardRepository.findByPeriodOrderByPoints(QuestPeriod.DAILY))
                .thenReturn(List.of(dailyReward));
        when(questRewardRepository.findByPeriodOrderByPoints(QuestPeriod.WEEKLY))
                .thenReturn(List.of());

        UserQuestProgress progress = new UserQuestProgress(null, dailyLogin, TODAY);
        progress.increase(1, 1);
        when(userQuestProgressRepository.findByUserId(USER_ID)).thenReturn(List.of(progress));

        UserQuestPoint point = new UserQuestPoint(null, QuestPeriod.DAILY, TODAY);
        point.addPoints(40);
        when(userQuestPointRepository.findByUserId(USER_ID)).thenReturn(List.of(point));

        List<UserQuestPeriodResponse> statuses = userQuestProgressService.getMyQuestStatus(USER_ID);

        UserQuestPeriodResponse daily =
                statuses.stream()
                        .filter(s -> s.period() == QuestPeriod.DAILY)
                        .findFirst()
                        .orElseThrow();
        assertThat(daily.points()).isEqualTo(40);
        assertThat(daily.quests()).hasSize(1);
        assertThat(daily.quests().get(0).completed()).isTrue();
        assertThat(daily.quests().get(0).claimed()).isFalse();
        assertThat(daily.rewards()).hasSize(1);
        assertThat(daily.rewards().get(0).unlocked()).isTrue();
        assertThat(daily.rewards().get(0).claimed()).isFalse();

        UserQuestPeriodResponse weekly =
                statuses.stream()
                        .filter(s -> s.period() == QuestPeriod.WEEKLY)
                        .findFirst()
                        .orElseThrow();
        assertThat(weekly.points()).isZero();
        assertThat(weekly.quests()).isEmpty();
    }
}
