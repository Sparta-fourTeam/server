package com.novaserver.skill.controller;

import com.novaserver.skill.dto.MirroredCardRequest;
import com.novaserver.skill.dto.MirroredCardResponse;
import com.novaserver.skill.service.MirroredCardService;
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

/** 카드 효과 적용 조건 관리 API. */
@Tag(name = "카드 효과 적용 조건", description = "다른 스킬에서 고른 카드 효과를 이 스킬에 함께 적용하는 조건 목록·등록·삭제")
@RestController
@RequiredArgsConstructor
public class MirroredCardController {
    private final MirroredCardService mirroredCardService;

    @Operation(summary = "카드 효과 적용 조건 목록 조회", description = "없는 스킬 id면 404 반환합니다.")
    @GetMapping("/admin/skills/{skillId}/mirror-cards")
    public List<MirroredCardResponse> getMirroredCards(@PathVariable Long skillId) {
        return mirroredCardService.getMirroredCards(skillId);
    }

    @Operation(summary = "카드 효과 적용 조건 등록", description = "없는 스킬·카드 id면 404, 같은 조건이 있으면 409 반환합니다.")
    @PostMapping("/admin/skills/{skillId}/mirror-cards")
    public MirroredCardResponse createMirroredCard(
            @PathVariable Long skillId, @Valid @RequestBody MirroredCardRequest request) {
        return mirroredCardService.createMirroredCard(skillId, request);
    }

    @Operation(summary = "카드 효과 적용 조건 삭제", description = "없는 id면 404 반환합니다.")
    @DeleteMapping("/admin/mirror-cards/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteMirroredCard(@PathVariable Long id) {
        mirroredCardService.deleteMirroredCard(id);
    }
}
