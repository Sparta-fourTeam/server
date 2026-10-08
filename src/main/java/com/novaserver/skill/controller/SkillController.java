package com.novaserver.skill.controller;

import com.novaserver.skill.dto.SkillDataResponse;
import com.novaserver.skill.dto.SkillDetailResponse;
import com.novaserver.skill.dto.SkillRequest;
import com.novaserver.skill.dto.SkillSummaryResponse;
import com.novaserver.skill.service.SkillService;
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

/** 스킬 관리 API. */
@Tag(name = "스킬 관리", description = "스킬 등록·수정·삭제")
@RestController
@RequestMapping("/skill")
@RequiredArgsConstructor
public class SkillController {
    private final SkillService skillService;

    @Operation(summary = "스킬 목록 조회", description = "스킬 목록을 조회합니다.")
    @GetMapping("/list")
    public List<SkillSummaryResponse> getSkills() {
        return skillService.getSkillList();
    }

    @Operation(summary = "스킬 상세 조회", description = "해당 스킬 정보를 조회합니다.")
    @GetMapping("/{id}")
    public SkillDetailResponse getSkill(@PathVariable Long id) {
        return skillService.getSkillDetail(id);
    }

    @Operation(summary = "(유니티) 스킬 데이터 조회", description = "스킬 데이터를 조회합니다.")
    @GetMapping
    public List<SkillDataResponse> getSkillData() {
        return skillService.getSkillData();
    }

    @Operation(summary = "스킬 등록", description = "스킬 정보를 등록합니다.")
    @PostMapping
    public SkillDetailResponse createSkill(@Valid @RequestBody SkillRequest request) {
        return skillService.createSkill(request);
    }

    @Operation(summary = "스킬 수정", description = "스킬을 수정합니다.")
    @PutMapping("/{id}")
    public SkillDetailResponse updateSkill(
            @PathVariable Long id, @Valid @RequestBody SkillRequest request) {
        return skillService.updateSkill(id, request);
    }

    @Operation(summary = "스킬 삭제", description = "스킬을 삭제합니다.")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteSkill(@PathVariable Long id) {
        skillService.deleteSkill(id);
    }
}
