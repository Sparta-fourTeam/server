package com.novaserver.cards.controller;

import com.novaserver.cards.dto.CardEffectRequest;
import com.novaserver.cards.dto.CardEffectResponse;
import com.novaserver.cards.service.CardEffectService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/** 카드 효과 관리 API. */
@Tag(name = "카드 효과", description = "카드 효과 목록·등록·수정·삭제")
@RestController
@RequiredArgsConstructor
public class CardEffectController {
    private final CardEffectService cardEffectService;

    @Operation(summary = "카드 효과 목록 조회", description = "없는 카드 id면 404 반환합니다.")
    @GetMapping("/admin/cards/{cardId}/effects")
    public List<CardEffectResponse> getCardEffects(@PathVariable Long cardId) {
        return cardEffectService.getCardEffects(cardId);
    }

    @Operation(
            summary = "카드 효과 등록",
            description = "없는 카드 id, kind, form, trigger, skillId면 404 반환합니다.")
    @PostMapping("/admin/cards/{cardId}/effects")
    public CardEffectResponse createCardEffect(
            @PathVariable Long cardId, @Valid @RequestBody CardEffectRequest request) {
        return cardEffectService.createCardEffect(cardId, request);
    }

    @Operation(summary = "변형 효과 목록 조회", description = "없는 변형 id면 404 반환합니다.")
    @GetMapping("/admin/card-variants/{variantId}/effects")
    public List<CardEffectResponse> getVariantEffects(@PathVariable Long variantId) {
        return cardEffectService.getVariantEffects(variantId);
    }

    @Operation(
            summary = "변형 효과 등록",
            description = "없는 변형 id, kind, form, trigger, skillId면 404 반환합니다.")
    @PostMapping("/admin/card-variants/{variantId}/effects")
    public CardEffectResponse createVariantEffect(
            @PathVariable Long variantId, @Valid @RequestBody CardEffectRequest request) {
        return cardEffectService.createVariantEffect(variantId, request);
    }

    @Operation(summary = "효과 수정", description = "없는 id, kind, form, trigger, skillId면 404 반환합니다.")
    @PutMapping("/admin/card-effects/{effectId}")
    public CardEffectResponse updateEffect(
            @PathVariable Long effectId, @Valid @RequestBody CardEffectRequest request) {
        return cardEffectService.updateEffect(effectId, request);
    }

    @Operation(summary = "효과 삭제", description = "없는 id면 404 반환합니다.")
    @DeleteMapping("/admin/card-effects/{effectId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteEffect(@PathVariable Long effectId) {
        cardEffectService.deleteEffect(effectId);
    }
}
