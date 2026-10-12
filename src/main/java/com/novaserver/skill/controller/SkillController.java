package com.novaserver.skill.controller;

import com.novaserver.skill.dto.SkillDataResponse;
import com.novaserver.skill.service.SkillService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 스킬 관리 API.
 */
@Tag(name = "스킬 조회", description = "스킬 조회")
@RestController
@RequestMapping("/skill")
@RequiredArgsConstructor
public class SkillController {
    private final SkillService skillService;

    @Operation(summary = "(유니티) 스킬 데이터 조회", description = "스킬 데이터를 조회합니다.")
    @GetMapping
    public List<SkillDataResponse> getSkillData() {
        return skillService.getSkillData();
    }
}
