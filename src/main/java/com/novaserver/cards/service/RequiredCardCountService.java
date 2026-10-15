package com.novaserver.cards.service;

import com.novaserver.cards.dto.RequiredCardCountRequest;
import com.novaserver.cards.dto.RequiredCardCountResponse;
import com.novaserver.cards.entity.RequiredCardCount;
import com.novaserver.cards.entity.UpgradeCard;
import com.novaserver.cards.repository.RequiredCardCountRepository;
import com.novaserver.global.error.ErrorCode;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 선행 카드 조건 관리 로직. */
@Service
@RequiredArgsConstructor
public class RequiredCardCountService {
    private final RequiredCardCountRepository requiredCardCountRepository;
    private final CardService cardService;

    /** 카드의 선행 카드 조건을 조회한다. */
    @Transactional(readOnly = true)
    public List<RequiredCardCountResponse> getRequiredCardCounts(Long cardId) {
        cardService.findCard(cardId);
        return requiredCardCountRepository.findAllByCardId(cardId).stream()
                .map(RequiredCardCountResponse::from)
                .toList();
    }

    /** 선행 카드 조건을 등록한다. */
    @Transactional
    public RequiredCardCountResponse createRequiredCardCount(
            Long cardId, RequiredCardCountRequest request) {
        if (cardId.equals(request.getRequiredCardId())) {
            throw ErrorCode.REQUIRED_CARD_COUNT_SELF_REFERENCE.exception(cardId);
        }
        UpgradeCard card = cardService.findCard(cardId);
        UpgradeCard requiredCard = cardService.findCard(request.getRequiredCardId());
        if (requiredCardCountRepository.existsByCardIdAndRequiredCardId(
                cardId, request.getRequiredCardId())) {
            throw ErrorCode.REQUIRED_CARD_COUNT_DUPLICATED.exception(request.getRequiredCardId());
        }
        RequiredCardCount saved =
                requiredCardCountRepository.save(
                        new RequiredCardCount(card, requiredCard, request.getCount()));
        return RequiredCardCountResponse.from(saved);
    }

    @Transactional
    public void deleteRequiredCardCount(Long id) {
        requiredCardCountRepository.delete(findRequiredCardCount(id));
    }

    private RequiredCardCount findRequiredCardCount(Long id) {
        return requiredCardCountRepository
                .findById(id)
                .orElseThrow(() -> ErrorCode.REQUIRED_CARD_COUNT_NOT_FOUND.exception(id));
    }
}
