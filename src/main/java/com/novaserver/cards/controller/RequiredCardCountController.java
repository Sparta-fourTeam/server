package com.novaserver.cards.controller;

import com.novaserver.cards.dto.RequiredCardCountRequest;
import com.novaserver.cards.dto.RequiredCardCountResponse;
import com.novaserver.cards.service.RequiredCardCountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/** 선행 카드 조건 관리 API. */
@Tag(name = "선행 카드 조건", description = "선행 카드 선택 횟수 조건 목록·등록·삭제")
@RestController
@RequiredArgsConstructor
public class RequiredCardCountController {
    private final RequiredCardCountService requiredCardCountService;

    @Operation(summary = "선행 카드 조건 목록 조회", description = "없는 카드 id면 404 반환합니다.")
    @GetMapping("/admin/cards/{cardId}/required-card-counts")
    public List<RequiredCardCountResponse> getRequiredCardCounts(@PathVariable Long cardId) {
        return requiredCardCountService.getRequiredCardCounts(cardId);
    }

    @Operation(
            summary = "선행 카드 조건 등록",
            description = "자기 자신을 가리키면 400, 없는 카드·스킬 id면 404, 같은 선행 카드가 있으면 409 반환합니다.")
    @PostMapping("/admin/cards/{cardId}/required-card-counts")
    public RequiredCardCountResponse createRequiredCardCount(
            @PathVariable Long cardId, @Valid @RequestBody RequiredCardCountRequest request) {
        return requiredCardCountService.createRequiredCardCount(cardId, request);
    }

    @Operation(summary = "선행 카드 조건 삭제", description = "없는 id면 404 반환합니다.")
    @DeleteMapping("/admin/required-card-counts/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteRequiredCardCount(@PathVariable Long id) {
        requiredCardCountService.deleteRequiredCardCount(id);
    }
}
