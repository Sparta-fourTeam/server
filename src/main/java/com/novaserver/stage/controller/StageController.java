package com.novaserver.stage.controller;

import com.novaserver.stage.dto.StageDataResponse;
import com.novaserver.stage.service.StageRewardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 스테이지 조회 API. */
@Tag(name = "스테이지 조회", description = "스테이지 조회")
@RestController
@RequestMapping("/stage")
@RequiredArgsConstructor
public class StageController {
    private final StageRewardService stageRewardService;

    @Operation(summary = "(유니티) 스테이지 보상 데이터 조회", description = "스테이지 보상 데이터를 조회합니다.")
    @GetMapping
    public List<StageDataResponse> getStageData() {
        return stageRewardService.getStageRewardData();
    }
}
