package com.novaserver.stage.controller;

import com.novaserver.stage.dto.StageRequest;
import com.novaserver.stage.dto.StageResponse;
import com.novaserver.stage.service.StageService;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** 스테이지 관리 관리자용 API. */
@Tag(name = "스테이지 관리", description = "스테이지 등록·삭제")
@RestController
@RequestMapping("/admin/stage")
@RequiredArgsConstructor
public class StageAdminController {
    private final StageService stageService;

    @Operation(summary = "스테이지 목록 조회", description = "스테이지 목록을 조회합니다.")
    @GetMapping("/list")
    public List<StageResponse> getStages() {
        return stageService.getStageList();
    }

    @Operation(summary = "스테이지 상세 조회", description = "해당 스테이지 정보를 조회합니다.")
    @GetMapping("/{stageId}")
    public StageResponse getStage(@PathVariable Integer stageId) {
        return stageService.getStage(stageId);
    }

    @Operation(summary = "스테이지 등록", description = "스테이지를 등록합니다.")
    @PostMapping
    public StageResponse createStage(@Valid @RequestBody StageRequest request) {
        return stageService.createStage(request);
    }

    @Operation(summary = "스테이지 삭제", description = "없는 id면 404, 등록된 보상이 있으면 409 반환합니다.")
    @DeleteMapping("/{stageId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteStage(@PathVariable Integer stageId) {
        stageService.deleteStage(stageId);
    }
}
