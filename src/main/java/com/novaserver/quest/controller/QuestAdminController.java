package com.novaserver.quest.controller;

import com.novaserver.quest.dto.QuestChestRequest;
import com.novaserver.quest.dto.QuestChestResponse;
import com.novaserver.quest.dto.QuestConfigRequest;
import com.novaserver.quest.dto.QuestConfigResponse;
import com.novaserver.quest.service.QuestConfigService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 관리자 퀘스트 조건과 상자 보상 연결 API. */
@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
public class QuestAdminController {
    private final QuestConfigService service;

    @GetMapping("/quests")
    public List<QuestConfigResponse> quests() {
        return service.quests();
    }

    @GetMapping("/quest-chests")
    public List<QuestChestResponse> chests() {
        return service.chests();
    }

    @Operation(
            summary = "퀘스트 조건 등록·교체",
            description =
                    "LOGIN, SPEND_ENERGY와 서버가 인정한 KILL_MONSTERS를 지원합니다. 수령 이력이 있으면 409이며 새 ID로 "
                            + "등록해야 합니다. 현재 기간의 서버 행동 누계로 새 조건도 평가합니다. 운영 정의를 자동 생성하지 않습니다.")
    @PutMapping("/quests/{id}")
    public QuestConfigResponse quest(
            @PathVariable String id, @Valid @RequestBody QuestConfigRequest r) {
        return service.saveQuest(id, r);
    }

    @Operation(
            summary = "상자 조건·보상 등록·교체",
            description =
                    "골드·아이템은 기존 공통 rewardDefinitionId를 참조하고 energy·gems는 별도 "
                            + "비음수 정수입니다. 수령한 정의는 교체·삭제할 수 없습니다. 공통 묶음 수정은 "
                            + "이후 미수령 상자에 반영되며 이미 확정한 영수증은 유지합니다.")
    @PutMapping("/quest-chests/{id}")
    public QuestChestResponse chest(
            @PathVariable String id, @Valid @RequestBody QuestChestRequest r) {
        return service.saveChest(id, r);
    }

    @DeleteMapping("/quests/{id}")
    public void deleteQuest(@PathVariable String id) {
        service.deleteQuest(id);
    }

    @DeleteMapping("/quest-chests/{id}")
    public void deleteChest(@PathVariable String id) {
        service.deleteChest(id);
    }
}
