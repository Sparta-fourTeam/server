package com.novaserver.quest.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.novaserver.global.error.ErrorCode;
import com.novaserver.quest.dto.QuestRewardCreateRequest;
import com.novaserver.quest.dto.QuestRewardRequest;
import com.novaserver.quest.dto.QuestRewardResponse;
import com.novaserver.quest.entity.QuestPeriod;
import com.novaserver.quest.entity.QuestReward;
import com.novaserver.quest.repository.QuestRewardRepository;
import java.util.List;
import java.util.Optional;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class QuestRewardServiceTest {

    @Mock private QuestRewardRepository questRewardRepository;

    @InjectMocks private QuestRewardService questRewardService;

    private QuestRewardCreateRequest createRequest(
            String rewardId, QuestPeriod period, int points, int gold, int energy) {
        QuestRewardCreateRequest request = new QuestRewardCreateRequest();
        ReflectionTestUtils.setField(request, "rewardId", rewardId);
        ReflectionTestUtils.setField(request, "period", period);
        ReflectionTestUtils.setField(request, "points", points);
        ReflectionTestUtils.setField(request, "gold", gold);
        ReflectionTestUtils.setField(request, "energy", energy);
        return request;
    }

    private QuestRewardRequest updateRequest(int gold, int energy) {
        QuestRewardRequest request = new QuestRewardRequest();
        ReflectionTestUtils.setField(request, "gold", gold);
        ReflectionTestUtils.setField(request, "energy", energy);
        return request;
    }

    @Test
    @DisplayName("보상 목록을 rewardId 기준으로 정렬해서 조회한다")
    void shouldReturnQuestRewardList_onGetQuestRewardList() {
        when(questRewardRepository.findAll(Sort.by("rewardId")))
                .thenReturn(
                        List.of(new QuestReward("daily.reward.20", QuestPeriod.DAILY, 20, 300, 5)));

        List<QuestRewardResponse> responses = questRewardService.getQuestRewardList();

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).rewardId()).isEqualTo("daily.reward.20");
    }

    @Test
    @DisplayName("존재하지 않는 보상을 조회하면 404 QUEST_REWARD_NOT_FOUND를 던진다")
    void shouldThrowNotFound_whenQuestRewardMissingOnGet() {
        when(questRewardRepository.findById("daily.reward.20")).thenReturn(Optional.empty());

        assertStatus(
                () -> questRewardService.getQuestReward("daily.reward.20"),
                HttpStatus.NOT_FOUND,
                ErrorCode.QUEST_REWARD_NOT_FOUND,
                "daily.reward.20");
    }

    @Test
    @DisplayName("이미 존재하는 id로 등록하면 409 QUEST_REWARD_DUPLICATED를 던지고 저장하지 않는다")
    void shouldThrowConflict_whenCreatingDuplicatedId() {
        when(questRewardRepository.existsById("daily.reward.20")).thenReturn(true);

        assertStatus(
                () ->
                        questRewardService.createQuestReward(
                                createRequest("daily.reward.20", QuestPeriod.DAILY, 20, 300, 5)),
                HttpStatus.CONFLICT,
                ErrorCode.QUEST_REWARD_DUPLICATED,
                "daily.reward.20");
        verify(questRewardRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("같은 주기에 같은 포인트 구간이 이미 있으면 409 QUEST_REWARD_THRESHOLD_DUPLICATED를 던지고 저장하지 않는다")
    void shouldThrowConflict_whenCreatingDuplicateThreshold() {
        when(questRewardRepository.existsById("daily.reward.20b")).thenReturn(false);
        when(questRewardRepository.existsByPeriodAndPoints(QuestPeriod.DAILY, 20)).thenReturn(true);

        assertStatus(
                () ->
                        questRewardService.createQuestReward(
                                createRequest("daily.reward.20b", QuestPeriod.DAILY, 20, 300, 5)),
                HttpStatus.CONFLICT,
                ErrorCode.QUEST_REWARD_THRESHOLD_DUPLICATED,
                QuestPeriod.DAILY,
                20);
        verify(questRewardRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("보상을 정상적으로 등록한다")
    void shouldCreateQuestRewardSuccessfully() {
        when(questRewardRepository.existsById("daily.reward.20")).thenReturn(false);
        when(questRewardRepository.existsByPeriodAndPoints(QuestPeriod.DAILY, 20))
                .thenReturn(false);
        when(questRewardRepository.saveAndFlush(any()))
                .thenReturn(new QuestReward("daily.reward.20", QuestPeriod.DAILY, 20, 300, 5));

        QuestRewardResponse response =
                questRewardService.createQuestReward(
                        createRequest("daily.reward.20", QuestPeriod.DAILY, 20, 300, 5));

        assertThat(response.rewardId()).isEqualTo("daily.reward.20");
        assertThat(response.points()).isEqualTo(20);
        assertThat(response.gold()).isEqualTo(300);
        assertThat(response.energy()).isEqualTo(5);
    }

    @Test
    @DisplayName("저장 시점에 유니크 제약이 깨지면 409 QUEST_REWARD_DUPLICATED를 던진다")
    void shouldThrowConflict_whenSaveViolatesUniqueConstraint() {
        when(questRewardRepository.existsById("daily.reward.20")).thenReturn(false);
        when(questRewardRepository.existsByPeriodAndPoints(QuestPeriod.DAILY, 20))
                .thenReturn(false);
        when(questRewardRepository.saveAndFlush(any()))
                .thenThrow(new DataIntegrityViolationException(""));

        assertStatus(
                () ->
                        questRewardService.createQuestReward(
                                createRequest("daily.reward.20", QuestPeriod.DAILY, 20, 300, 5)),
                HttpStatus.CONFLICT,
                ErrorCode.QUEST_REWARD_DUPLICATED,
                "daily.reward.20");
    }

    @Test
    @DisplayName("존재하지 않는 보상을 수정하면 404 QUEST_REWARD_NOT_FOUND를 던진다")
    void shouldThrowNotFound_whenUpdatingMissingQuestReward() {
        when(questRewardRepository.findById("daily.reward.20")).thenReturn(Optional.empty());

        assertStatus(
                () ->
                        questRewardService.updateQuestReward(
                                "daily.reward.20", updateRequest(500, 5)),
                HttpStatus.NOT_FOUND,
                ErrorCode.QUEST_REWARD_NOT_FOUND,
                "daily.reward.20");
    }

    @Test
    @DisplayName("보상을 정상적으로 수정한다. 포인트 구간은 바뀌지 않는다")
    void shouldUpdateQuestRewardSuccessfully() {
        QuestReward reward = new QuestReward("daily.reward.20", QuestPeriod.DAILY, 20, 300, 5);
        when(questRewardRepository.findById("daily.reward.20")).thenReturn(Optional.of(reward));

        QuestRewardResponse response =
                questRewardService.updateQuestReward("daily.reward.20", updateRequest(500, 10));

        assertThat(response.points()).isEqualTo(20);
        assertThat(response.gold()).isEqualTo(500);
        assertThat(response.energy()).isEqualTo(10);
    }

    @Test
    @DisplayName("존재하지 않는 보상을 삭제하면 404 QUEST_REWARD_NOT_FOUND를 던진다")
    void shouldThrowNotFound_whenDeletingMissingQuestReward() {
        when(questRewardRepository.findById("daily.reward.20")).thenReturn(Optional.empty());

        assertStatus(
                () -> questRewardService.deleteQuestReward("daily.reward.20"),
                HttpStatus.NOT_FOUND,
                ErrorCode.QUEST_REWARD_NOT_FOUND,
                "daily.reward.20");
    }

    @Test
    @DisplayName("보상을 정상적으로 삭제한다")
    void shouldDeleteQuestRewardSuccessfully() {
        QuestReward reward = new QuestReward("daily.reward.20", QuestPeriod.DAILY, 20, 300, 5);
        when(questRewardRepository.findById("daily.reward.20")).thenReturn(Optional.of(reward));

        questRewardService.deleteQuestReward("daily.reward.20");

        verify(questRewardRepository).delete(reward);
    }

    private static void assertStatus(
            ThrowingCallable call, HttpStatus status, ErrorCode errorCode, Object... args) {
        assertThatThrownBy(call)
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(
                        e -> {
                            ResponseStatusException ex = (ResponseStatusException) e;
                            assertThat(ex.getStatusCode()).isEqualTo(status);
                            assertThat(ex.getReason())
                                    .isEqualTo(errorCode.getMessage().formatted(args));
                        });
    }
}
