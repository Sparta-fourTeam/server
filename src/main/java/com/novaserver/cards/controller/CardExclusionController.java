package com.novaserver.cards.controller;

import com.novaserver.cards.dto.CardExclusionRequest;
import com.novaserver.cards.dto.CardExclusionResponse;
import com.novaserver.cards.service.CardExclusionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/** 카드 배제 조건 관리 API. */
@Tag(name = "카드 배제 조건", description = "카드 배제 조건 목록·등록·삭제")
@RestController
@RequiredArgsConstructor
public class CardExclusionController {
    private final CardExclusionService cardExclusionService;

    @Operation(summary = "배제 조건 목록 조회", description = "없는 카드 id면 404 반환합니다.")
    @GetMapping("/admin/cards/{cardId}/exclusions")
    public List<CardExclusionResponse> getExclusions(@PathVariable Long cardId) {
        return cardExclusionService.getExclusions(cardId);
    }

    @Operation(
            summary = "배제 조건 등록",
            description = "자기 자신을 가리키면 400, 없는 카드·스킬 id면 404, 같은 배제 카드가 있으면 409 반환합니다.")
    @PostMapping("/admin/cards/{cardId}/exclusions")
    public CardExclusionResponse createExclusion(
            @PathVariable Long cardId, @Valid @RequestBody CardExclusionRequest request) {
        return cardExclusionService.createExclusion(cardId, request);
    }

    @Operation(summary = "배제 조건 삭제", description = "없는 id면 404 반환합니다.")
    @DeleteMapping("/admin/card-exclusions/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteExclusion(@PathVariable Long id) {
        cardExclusionService.deleteExclusion(id);
    }
}
