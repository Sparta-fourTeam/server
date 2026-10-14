package com.novaserver.cards.service;

import com.novaserver.cards.dto.CardExclusionRequest;
import com.novaserver.cards.dto.CardExclusionResponse;
import com.novaserver.cards.entity.CardExclusion;
import com.novaserver.cards.entity.UpgradeCard;
import com.novaserver.cards.repository.CardExclusionRepository;
import com.novaserver.global.error.ErrorCode;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 카드 배제 조건 관리 로직. */
@Service
@RequiredArgsConstructor
public class CardExclusionService {
    private final CardExclusionRepository cardExclusionRepository;
    private final CardService cardService;

    /** 카드의 배제 조건을 조회한다. */
    @Transactional(readOnly = true)
    public List<CardExclusionResponse> getExclusions(Long cardId) {
        cardService.findCard(cardId);
        return cardExclusionRepository.findAllByCardId(cardId).stream()
                .map(CardExclusionResponse::from)
                .toList();
    }

    /** 배제 조건을 등록한다. */
    @Transactional
    public CardExclusionResponse createExclusion(Long cardId, CardExclusionRequest request) {
        if (cardId.equals(request.getExcludingCardId())) {
            throw ErrorCode.CARD_EXCLUSION_SELF_REFERENCE.exception(cardId);
        }
        UpgradeCard card = cardService.findCard(cardId);
        UpgradeCard excludingCard = cardService.findCard(request.getExcludingCardId());
        if (cardExclusionRepository.existsByCardIdAndExcludingCardId(
                cardId, request.getExcludingCardId())) {
            throw ErrorCode.CARD_EXCLUSION_DUPLICATED.exception(request.getExcludingCardId());
        }
        CardExclusion saved =
                cardExclusionRepository.save(
                        new CardExclusion(card, excludingCard, request.getBelowPermanentLevel()));
        return CardExclusionResponse.from(saved);
    }

    @Transactional
    public void deleteExclusion(Long id) {
        cardExclusionRepository.delete(findExclusion(id));
    }

    private CardExclusion findExclusion(Long id) {
        return cardExclusionRepository
                .findById(id)
                .orElseThrow(() -> ErrorCode.CARD_EXCLUSION_NOT_FOUND.exception(id));
    }
}
