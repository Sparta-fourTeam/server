package com.novaserver.skill.controller;

import com.novaserver.skill.dto.SkillCastTypeRequest;
import com.novaserver.skill.dto.SkillCastTypeResponse;
import com.novaserver.skill.service.SkillCastTypeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/** 스킬 시전 방식 관리 API. */
@Tag(name = "스킬 시전 방식", description = "시전 방식 목록·등록·수정·삭제")
@RestController
@RequestMapping("/skill-cast-types")
@RequiredArgsConstructor
public class SkillCastTypeController {
    private final SkillCastTypeService skillCastTypeService;

    @Operation(summary = "시전 방식 목록", description = "등록된 시전 방식을 id 순으로 조회합니다.")
    @GetMapping
    public List<SkillCastTypeResponse> getCastTypes() {
        return skillCastTypeService.getCastTypes();
    }

    @Operation(summary = "시전 방식 등록", description = "이름이 이미 있으면 409를 반환합니다.")
    @PostMapping
    public SkillCastTypeResponse createCastType(@Valid @RequestBody SkillCastTypeRequest request) {
        return skillCastTypeService.createCastType(request);
    }

    @Operation(summary = "시전 방식 수정", description = "없는 id면 404, 다른 시전 방식과 이름이 겹치면 409 반환합니다.")
    @PutMapping("/{id}")
    public SkillCastTypeResponse updateCastType(
            @PathVariable Long id, @Valid @RequestBody SkillCastTypeRequest request) {
        return skillCastTypeService.updateCastType(id, request);
    }

    @Operation(summary = "시전 방식 삭제", description = "없는 id면 404, 사용 중인 스킬이 있으면 409 반환합니다.")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCastType(@PathVariable Long id) {
        skillCastTypeService.deleteCastType(id);
    }
}
