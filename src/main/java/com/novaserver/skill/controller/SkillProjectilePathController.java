package com.novaserver.skill.controller;

import com.novaserver.skill.dto.SkillProjectilePathRequest;
import com.novaserver.skill.dto.SkillProjectilePathResponse;
import com.novaserver.skill.service.SkillProjectilePathService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/** 투사체 경로 관리 API. */
@Tag(name = "투사체 경로", description = "투사체 경로 목록·등록·수정·삭제")
@RestController
@RequestMapping("/skill-projectile-paths")
@RequiredArgsConstructor
public class SkillProjectilePathController {
    private final SkillProjectilePathService skillProjectilePathService;

    @Operation(summary = "투사체 경로 목록", description = "등록된 투사체 경로 목록을 id 순으로 조회합니다.")
    @GetMapping
    public List<SkillProjectilePathResponse> getProjectilePaths() {
        return skillProjectilePathService.getSkillProjectilePaths();
    }

    @Operation(summary = "투사체 경로 등록", description = "이름이 있으면 409를 반환합니다.")
    @PostMapping
    public SkillProjectilePathResponse createProjectilePath(
            @Valid @RequestBody SkillProjectilePathRequest request) {
        return skillProjectilePathService.createSkillProjectilePath(request);
    }

    @Operation(summary = "투사체 경로 수정", description = "없는 id면 404, 다른 투사체 경로와 이름이 겹치면 409 반환합니다.")
    @PutMapping("/{id}")
    public SkillProjectilePathResponse updateProjectilePath(
            @PathVariable Long id, @Valid @RequestBody SkillProjectilePathRequest request) {
        return skillProjectilePathService.updateSkillProjectilePath(id, request);
    }

    @Operation(summary = "투사체 경로 삭제", description = "없는 id면 404, 사용 중인 스킬이 있으면 409 반환합니다.")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteProjectilePath(@PathVariable Long id) {
        skillProjectilePathService.deleteSkillProjectilePath(id);
    }
}
