package com.novaserver.cards.controller;

import com.novaserver.cards.dto.CardVariantRequest;
import com.novaserver.cards.dto.CardVariantResponse;
import com.novaserver.cards.service.CardVariantService;
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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** 카드 변형 관리 API. */
@Tag(name = "카드 변형", description = "카드 변형 목록·등록·수정·삭제")
@RestController
@RequiredArgsConstructor
public class CardVariantController {
    private final CardVariantService cardVariantService;

    @Operation(summary = "변형 목록 조회", description = "없는 카드 id면 404 반환합니다.")
    @GetMapping("/admin/cards/{cardId}/variants")
    public List<CardVariantResponse> getVariants(@PathVariable Long cardId) {
        return cardVariantService.getVariants(cardId);
    }

    @Operation(
            summary = "변형 등록",
            description = "없는 카드 id면 404, 같은 minPermanentLevel이 있으면 409 반환합니다.")
    @PostMapping("/admin/cards/{cardId}/variants")
    public CardVariantResponse createVariant(
            @PathVariable Long cardId, @Valid @RequestBody CardVariantRequest request) {
        return cardVariantService.createVariant(cardId, request);
    }

    @Operation(summary = "변형 수정", description = "없는 id면 404, 같은 minPermanentLevel이 있으면 409 반환합니다.")
    @PutMapping("/admin/card-variants/{variantId}")
    public CardVariantResponse updateVariant(
            @PathVariable Long variantId, @Valid @RequestBody CardVariantRequest request) {
        return cardVariantService.updateVariant(variantId, request);
    }

    @Operation(summary = "변형 삭제", description = "없는 id면 404 반환합니다. 변형의 효과도 함께 삭제됩니다.")
    @DeleteMapping("/admin/card-variants/{variantId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteVariant(@PathVariable Long variantId) {
        cardVariantService.deleteVariant(variantId);
    }
}
