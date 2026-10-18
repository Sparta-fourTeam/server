package com.novaserver.tutorial.controller;

import com.novaserver.tutorial.dto.TutorialConfigRequest;
import com.novaserver.tutorial.dto.TutorialConfigResponse;
import com.novaserver.tutorial.service.TutorialConfigService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** 관리자 전용 튜토리얼 조건·고정 보상 연결 API. 대사·번역은 클라이언트가 관리한다. */
@RestController
@RequestMapping("/admin/tutorials")
@RequiredArgsConstructor
public class TutorialAdminController {
    private final TutorialConfigService service;

    @GetMapping
    public List<TutorialConfigResponse> list() {
        return service.list();
    }

    @Operation(
            summary = "튜토리얼 조건 전체 등록·교체",
            description =
                    "선행 완료·최소 레벨·startStage 해금 조건을 검사합니다. 완료 조건은 clearStageId, "
                            + "upgradeId/upgradeLevel, playerLevel 중 하나입니다. 기능 해금 시작 조건은 해당 해금 "
                            + "스테이지를 startStageId로 지정합니다. 보상은 사전 등록한 rewardDefinitionId 필수이며 "
                            + "setLevel은 0이면 유지, 양수면 최초 완료 시 해당 레벨 시작 경험치로 맞춥니다. "
                            + "강화 비용·재료의 동적 산출 및 출시 데이터 자동 입력은 수행하지 않습니다.")
    @PutMapping("/{id}")
    public TutorialConfigResponse save(
            @PathVariable String id, @Valid @RequestBody TutorialConfigRequest request) {
        return service.save(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable String id) {
        service.delete(id);
    }
}
