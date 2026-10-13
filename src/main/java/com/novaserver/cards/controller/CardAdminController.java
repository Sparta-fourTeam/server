package com.novaserver.cards.controller;

import com.novaserver.cards.dto.CardDetailResponse;
import com.novaserver.cards.dto.CardRequest;
import com.novaserver.cards.dto.CardSummaryResponse;
import com.novaserver.cards.service.CardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

/** 강화 카드 관리 API. */
@Tag(name = "강화 카드", description = "강화 카드 조회·등록·수정·삭제")
@RestController
@RequestMapping("/admin/cards")
@RequiredArgsConstructor
public class CardAdminController {
    private final CardService cardService;

    @Operation(summary = "카드 목록 조회", description = "등록된 강화 카드를 조회합니다.")
    @GetMapping
    public List<CardSummaryResponse> getCards() {
        return cardService.getCards();
    }

    @Operation(summary = "카드 상세 조회", description = "없는 id면 404 반환합니다.")
    @GetMapping("/{cardId}")
    public CardDetailResponse getCard(@PathVariable Long cardId) {
        return cardService.getCard(cardId);
    }

    @Operation(summary = "카드 등록", description = "없는 스킬 id면 404 반환합니다.")
    @PostMapping
    public CardDetailResponse createCard(@Valid @RequestBody CardRequest request) {
        return cardService.createCard(request);
    }

    @Operation(summary = "카드 수정", description = "없는 카드 id면 404, 없는 스킬 id면 404 반환합니다.")
    @PutMapping("/{cardId}")
    public CardDetailResponse updateCard(
            @PathVariable Long cardId, @Valid @RequestBody CardRequest request) {
        return cardService.updateCard(cardId, request);
    }

    @Operation(summary = "카드 삭제", description = "없는 id면 404 반환합니다. 효과·변형도 함께 삭제됩니다.")
    @DeleteMapping("/{cardId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCard(@PathVariable Long cardId) {
        cardService.deleteCard(cardId);
    }
}
