package com.novaserver.cards.service;

import com.novaserver.cards.dto.CardVariantRequest;
import com.novaserver.cards.dto.CardVariantResponse;
import com.novaserver.cards.entity.CardVariant;
import com.novaserver.cards.entity.UpgradeCard;
import com.novaserver.cards.repository.CardVariantRepository;
import com.novaserver.global.error.ErrorCode;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 카드 변형 관리 로직. */
@Service
@RequiredArgsConstructor
public class CardVariantService {
    private final CardVariantRepository cardVariantRepository;
    private final CardService cardService;

    /** 카드의 변형을 최소 영구 레벨 순으로 조회한다. */
    @Transactional(readOnly = true)
    public List<CardVariantResponse> getVariants(Long cardId) {
        cardService.findCard(cardId);
        return cardVariantRepository.findAllByCardIdOrderByMinPermanentLevelAsc(cardId).stream()
                .map(CardVariantResponse::from)
                .toList();
    }

    /** 변형을 등록한다. */
    @Transactional
    public CardVariantResponse createVariant(Long cardId, CardVariantRequest request) {
        UpgradeCard card = cardService.findCard(cardId);
        if (cardVariantRepository.existsByCardIdAndMinPermanentLevel(
                cardId, request.getMinPermanentLevel())) {
            throw ErrorCode.CARD_VARIANT_DUPLICATED.exception(request.getMinPermanentLevel());
        }
        CardVariant saved =
                cardVariantRepository.save(
                        new CardVariant(
                                card,
                                request.getMinPermanentLevel(),
                                request.getName(),
                                request.getDesc()));
        return CardVariantResponse.from(saved);
    }

    /** 변형을 수정한다. */
    @Transactional
    public CardVariantResponse updateVariant(Long id, CardVariantRequest request) {
        CardVariant variant = findVariant(id);
        if (cardVariantRepository.existsByCardIdAndMinPermanentLevelAndIdNot(
                variant.getCard().getId(), request.getMinPermanentLevel(), id)) {
            throw ErrorCode.CARD_VARIANT_DUPLICATED.exception(request.getMinPermanentLevel());
        }
        variant.update(request.getMinPermanentLevel(), request.getName(), request.getDesc());
        return CardVariantResponse.from(variant);
    }

    @Transactional
    public void deleteVariant(Long id) {
        cardVariantRepository.delete(findVariant(id));
    }

    /** 변형을 id로 찾는다. */
    public CardVariant findVariant(Long id) {
        return cardVariantRepository
                .findById(id)
                .orElseThrow(() -> ErrorCode.CARD_VARIANT_NOT_FOUND.exception(id));
    }
}
