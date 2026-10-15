package com.novaserver.cards.controller;

import com.novaserver.cards.dto.RequiredSkillRequest;
import com.novaserver.cards.dto.RequiredSkillResponse;
import com.novaserver.cards.service.RequiredSkillService;
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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** 필요 스킬 조건 관리 API. */
@Tag(name = "필요 스킬 조건", description = "카드 선택에 필요한 보유 스킬 조건 목록·등록·삭제")
@RestController
@RequiredArgsConstructor
public class RequiredSkillController {
    private final RequiredSkillService requiredSkillService;

    @Operation(summary = "필요 스킬 조건 목록 조회", description = "없는 카드 id면 404 반환합니다.")
    @GetMapping("/admin/cards/{cardId}/required-skills")
    public List<RequiredSkillResponse> getRequiredSkills(@PathVariable Long cardId) {
        return requiredSkillService.getRequiredSkills(cardId);
    }

    @Operation(summary = "필요 스킬 조건 등록", description = "없는 카드·스킬 id면 404, 같은 조건이 있으면 409 반환합니다.")
    @PostMapping("/admin/cards/{cardId}/required-skills")
    public RequiredSkillResponse createRequiredSkill(
            @PathVariable Long cardId, @Valid @RequestBody RequiredSkillRequest request) {
        return requiredSkillService.createRequiredSkill(cardId, request);
    }

    @Operation(summary = "필요 스킬 조건 삭제", description = "없는 id면 404 반환합니다.")
    @DeleteMapping("/admin/required-skills/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteRequiredSkill(@PathVariable Long id) {
        requiredSkillService.deleteRequiredSkill(id);
    }
}
