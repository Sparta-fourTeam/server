package com.novaserver.quest.controller;

import com.novaserver.quest.dto.QuestCreateRequest;
import com.novaserver.quest.dto.QuestRequest;
import com.novaserver.quest.dto.QuestResponse;
import com.novaserver.quest.service.QuestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** 일일·주간 퀘스트 관리 관리자용 API. */
@Tag(name = "퀘스트 관리", description = "일일·주간 퀘스트 등록·수정·삭제")
@RestController
@RequestMapping("/admin/quest")
@RequiredArgsConstructor
public class QuestAdminController {
    private final QuestService questService;

    @Operation(summary = "퀘스트 목록 조회", description = "등록된 모든 퀘스트를 조회합니다.")
    @GetMapping("/list")
    public List<QuestResponse> getQuests() {
        return questService.getQuestList();
    }

    @Operation(summary = "퀘스트 상세 조회", description = "해당 퀘스트 정보를 조회합니다.")
    @GetMapping("/{questId}")
    public QuestResponse getQuest(@PathVariable String questId) {
        return questService.getQuest(questId);
    }

    @Operation(summary = "퀘스트 등록", description = "퀘스트를 등록합니다.")
    @PostMapping
    public QuestResponse createQuest(@Valid @RequestBody QuestCreateRequest request) {
        return questService.createQuest(request);
    }

    @Operation(summary = "퀘스트 수정", description = "없는 id면 404를 반환합니다.")
    @PutMapping("/{questId}")
    public QuestResponse updateQuest(
            @PathVariable String questId, @Valid @RequestBody QuestRequest request) {
        return questService.updateQuest(questId, request);
    }

    @Operation(summary = "퀘스트 삭제", description = "퀘스트를 삭제합니다.")
    @DeleteMapping("/{questId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteQuest(@PathVariable String questId) {
        questService.deleteQuest(questId);
    }
}
