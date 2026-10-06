package com.novaserver.version.controller;

import com.novaserver.version.dto.DataVersionResponse;
import com.novaserver.version.entity.DataTable;
import com.novaserver.version.service.DataVersionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/** 게임 데이터 버전 API */
@Tag(name = "데이터 버전")
@RestController
@RequestMapping("/data")
@RequiredArgsConstructor
public class DataVersionController {
    private final DataVersionService dataVersionService;

    @Operation(summary = "버전 정보 조회", description = "버전 정보를 조회합니다.")
    @GetMapping("/version")
    public DataVersionResponse getVersion() {
        return dataVersionService.getVersions();
    }

    @Operation(summary = "버전 정보 업데이트", description = "특정 테이블의 버전 정보를 1 올립니다.")
    @PostMapping("/version")
    public void updateVersion(@RequestBody @Parameter(name = "데이터 테이블") DataTable table) {
        dataVersionService.increase(table);
    }
}
