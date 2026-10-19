package com.novaserver.quest.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.novaserver.global.error.ErrorCode;
import com.novaserver.quest.dto.QuestCreateRequest;
import com.novaserver.quest.dto.QuestRequest;
import com.novaserver.quest.dto.QuestResponse;
import com.novaserver.quest.entity.Quest;
import com.novaserver.quest.entity.QuestKind;
import com.novaserver.quest.entity.QuestPeriod;
import com.novaserver.quest.repository.QuestRepository;
import com.novaserver.user.repository.UserQuestProgressRepository;
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
class QuestServiceTest {

    @Mock private QuestRepository questRepository;
    @Mock private UserQuestProgressRepository userQuestProgressRepository;

    @InjectMocks private QuestService questService;

    private QuestCreateRequest createRequest(
            String questId, QuestPeriod period, QuestKind kind, int target, int points) {
        QuestCreateRequest request = new QuestCreateRequest();
        ReflectionTestUtils.setField(request, "questId", questId);
        ReflectionTestUtils.setField(request, "period", period);
        ReflectionTestUtils.setField(request, "kind", kind);
        ReflectionTestUtils.setField(request, "target", target);
        ReflectionTestUtils.setField(request, "points", points);
        return request;
    }

    private QuestRequest updateRequest(QuestKind kind, int target, int points) {
        QuestRequest request = new QuestRequest();
        ReflectionTestUtils.setField(request, "kind", kind);
        ReflectionTestUtils.setField(request, "target", target);
        ReflectionTestUtils.setField(request, "points", points);
        return request;
    }

    @Test
    @DisplayName("퀘스트 목록을 questId 기준으로 정렬해서 조회한다")
    void shouldReturnQuestList_onGetQuestList() {
        when(questRepository.findAll(Sort.by("questId")))
                .thenReturn(
                        List.of(
                                new Quest(
                                        "daily.login", QuestPeriod.DAILY, QuestKind.LOGIN, 1, 40)));

        List<QuestResponse> responses = questService.getQuestList();

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).questId()).isEqualTo("daily.login");
    }

    @Test
    @DisplayName("존재하지 않는 퀘스트를 조회하면 404 QUEST_NOT_FOUND를 던진다")
    void shouldThrowNotFound_whenQuestMissingOnGet() {
        when(questRepository.findById("daily.login")).thenReturn(Optional.empty());

        assertStatus(
                () -> questService.getQuest("daily.login"),
                HttpStatus.NOT_FOUND,
                ErrorCode.QUEST_NOT_FOUND,
                "daily.login");
    }

    @Test
    @DisplayName("이미 존재하는 id로 등록하면 409 QUEST_DUPLICATED를 던지고 저장하지 않는다")
    void shouldThrowConflict_whenCreatingDuplicatedId() {
        when(questRepository.existsById("daily.login")).thenReturn(true);

        assertStatus(
                () ->
                        questService.createQuest(
                                createRequest(
                                        "daily.login", QuestPeriod.DAILY, QuestKind.LOGIN, 1, 40)),
                HttpStatus.CONFLICT,
                ErrorCode.QUEST_DUPLICATED,
                "daily.login");
        verify(questRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("퀘스트를 정상적으로 등록한다")
    void shouldCreateQuestSuccessfully() {
        when(questRepository.existsById("daily.login")).thenReturn(false);
        when(questRepository.saveAndFlush(any()))
                .thenReturn(new Quest("daily.login", QuestPeriod.DAILY, QuestKind.LOGIN, 1, 40));

        QuestResponse response =
                questService.createQuest(
                        createRequest("daily.login", QuestPeriod.DAILY, QuestKind.LOGIN, 1, 40));

        assertThat(response.questId()).isEqualTo("daily.login");
        assertThat(response.period()).isEqualTo(QuestPeriod.DAILY);
        assertThat(response.kind()).isEqualTo(QuestKind.LOGIN);
        assertThat(response.target()).isEqualTo(1);
        assertThat(response.points()).isEqualTo(40);
    }

    @Test
    @DisplayName("저장 시점에 유니크 제약이 깨지면 409 QUEST_DUPLICATED를 던진다")
    void shouldThrowConflict_whenSaveViolatesUniqueConstraint() {
        when(questRepository.existsById("daily.login")).thenReturn(false);
        when(questRepository.saveAndFlush(any()))
                .thenThrow(new DataIntegrityViolationException(""));

        assertStatus(
                () ->
                        questService.createQuest(
                                createRequest(
                                        "daily.login", QuestPeriod.DAILY, QuestKind.LOGIN, 1, 40)),
                HttpStatus.CONFLICT,
                ErrorCode.QUEST_DUPLICATED,
                "daily.login");
    }

    @Test
    @DisplayName("존재하지 않는 퀘스트를 수정하면 404 QUEST_NOT_FOUND를 던진다")
    void shouldThrowNotFound_whenUpdatingMissingQuest() {
        when(questRepository.findById("daily.login")).thenReturn(Optional.empty());

        assertStatus(
                () ->
                        questService.updateQuest(
                                "daily.login", updateRequest(QuestKind.LOGIN, 2, 50)),
                HttpStatus.NOT_FOUND,
                ErrorCode.QUEST_NOT_FOUND,
                "daily.login");
    }

    @Test
    @DisplayName("퀘스트를 정상적으로 수정한다")
    void shouldUpdateQuestSuccessfully() {
        Quest quest = new Quest("daily.login", QuestPeriod.DAILY, QuestKind.LOGIN, 1, 40);
        when(questRepository.findById("daily.login")).thenReturn(Optional.of(quest));

        QuestResponse response =
                questService.updateQuest("daily.login", updateRequest(QuestKind.LOGIN, 2, 50));

        assertThat(response.target()).isEqualTo(2);
        assertThat(response.points()).isEqualTo(50);
    }

    @Test
    @DisplayName("존재하지 않는 퀘스트를 삭제하면 404 QUEST_NOT_FOUND를 던진다")
    void shouldThrowNotFound_whenDeletingMissingQuest() {
        when(questRepository.findById("daily.login")).thenReturn(Optional.empty());

        assertStatus(
                () -> questService.deleteQuest("daily.login"),
                HttpStatus.NOT_FOUND,
                ErrorCode.QUEST_NOT_FOUND,
                "daily.login");
    }

    @Test
    @DisplayName("진행 중인 유저가 있는 퀘스트를 삭제하면 409 QUEST_IN_USE를 던지고 삭제하지 않는다")
    void shouldThrowConflict_whenDeletingQuestInUse() {
        Quest quest = new Quest("daily.login", QuestPeriod.DAILY, QuestKind.LOGIN, 1, 40);
        when(questRepository.findById("daily.login")).thenReturn(Optional.of(quest));
        when(userQuestProgressRepository.existsByQuestQuestId("daily.login")).thenReturn(true);

        assertStatus(
                () -> questService.deleteQuest("daily.login"),
                HttpStatus.CONFLICT,
                ErrorCode.QUEST_IN_USE,
                "daily.login");
        verify(questRepository, never()).delete(any());
    }

    @Test
    @DisplayName("퀘스트를 정상적으로 삭제한다")
    void shouldDeleteQuestSuccessfully() {
        Quest quest = new Quest("daily.login", QuestPeriod.DAILY, QuestKind.LOGIN, 1, 40);
        when(questRepository.findById("daily.login")).thenReturn(Optional.of(quest));
        when(userQuestProgressRepository.existsByQuestQuestId("daily.login")).thenReturn(false);

        questService.deleteQuest("daily.login");

        verify(questRepository).delete(quest);
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
