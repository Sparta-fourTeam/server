package com.novaserver.cards.controller;

import com.novaserver.cards.dto.SkillFormRequest;
import com.novaserver.cards.dto.SkillFormResponse;
import com.novaserver.cards.service.SkillFormService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 스킬 형태 관리 API. */
@Tag(name = "스킬 형태", description = "형태 목록·등록·수정·삭제")
@RestController
@RequestMapping("/admin/skill-forms")
@RequiredArgsConstructor
public class SkillFormController {
    private final SkillFormService skillFormService;

    @Operation(summary = "형태 목록", description = "등록된 형태를 id 순으로 조회합니다.")
    @GetMapping
    public List<SkillFormResponse> getForms() {
        return skillFormService.getForms();
    }

    @Operation(summary = "형태 등록", description = "이름이 이미 있으면 409를 반환합니다.")
    @PostMapping
    public SkillFormResponse createForm(@Valid @RequestBody SkillFormRequest request) {
        return skillFormService.createForm(request);
    }

    @Operation(summary = "형태 수정", description = "없는 id면 404, 다른 형태와 이름이 겹치면 409 반환합니다.")
    @PutMapping("/{id}")
    public SkillFormResponse updateForm(
            @PathVariable Long id, @Valid @RequestBody SkillFormRequest request) {
        return skillFormService.updateForm(id, request);
    }
}
