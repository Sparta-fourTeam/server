package com.novaserver.cards.service;

import com.novaserver.cards.dto.CardDataResponse;
import com.novaserver.cards.dto.CardDetailResponse;
import com.novaserver.cards.dto.CardRequest;
import com.novaserver.cards.dto.CardSummaryResponse;
import com.novaserver.cards.entity.UpgradeCard;
import com.novaserver.cards.repository.UpgradeCardRepository;
import com.novaserver.global.error.ErrorCode;
import com.novaserver.skill.entity.Skill;
import com.novaserver.skill.repository.SkillRepository;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 강화 카드 관리 로직. */
@Service
@RequiredArgsConstructor
public class CardService {
    private final UpgradeCardRepository upgradeCardRepository;
    private final SkillRepository skillRepository;

    @Transactional(readOnly = true)
    public List<CardSummaryResponse> getCards() {
        return upgradeCardRepository.findAll().stream().map(CardSummaryResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public CardDetailResponse getCard(Long id) {
        return CardDetailResponse.from(findCard(id));
    }

    /** 스킬 id별 유니티용 카드 데이터를 모아서 돌려준다. */
    @Transactional(readOnly = true)
    public Map<Long, List<CardDataResponse>> getCardDataBySkillIds(List<Long> skillIds) {
        return upgradeCardRepository.findAllBySkillIdIn(skillIds).stream()
                .collect(
                        Collectors.groupingBy(
                                card -> card.getSkill().getId(),
                                Collectors.mapping(CardDataResponse::from, Collectors.toList())));
    }

    /** 카드를 등록한다. */
    @Transactional
    public CardDetailResponse createCard(CardRequest request) {
        Skill skill = findSkill(request.getSkillId());
        int minBattleLevel = request.getMinBattleLevel() != null ? request.getMinBattleLevel() : 1;
        validateMinBattleLevel(minBattleLevel, skill);
        UpgradeCard saved =
                upgradeCardRepository.save(
                        new UpgradeCard(
                                skill,
                                request.getName(),
                                request.getDesc(),
                                request.getMaxPickCount(),
                                request.getEnabled(),
                                request.getDisabledReason(),
                                minBattleLevel,
                                request.getMinPermanentLevel(),
                                request.getSharedId()));
        return CardDetailResponse.from(saved);
    }

    /** 카드를 수정한다. */
    @Transactional
    public CardDetailResponse updateCard(Long id, CardRequest request) {
        UpgradeCard card = findCard(id);
        Skill skill = findSkill(request.getSkillId());
        int minBattleLevel = request.getMinBattleLevel() != null ? request.getMinBattleLevel() : 1;
        validateMinBattleLevel(minBattleLevel, skill);
        card.update(
                skill,
                request.getName(),
                request.getDesc(),
                request.getMaxPickCount() != null ? request.getMaxPickCount() : 1,
                request.getEnabled() == null || request.getEnabled(),
                request.getDisabledReason(),
                minBattleLevel,
                request.getMinPermanentLevel() != null ? request.getMinPermanentLevel() : 0,
                request.getSharedId());
        return CardDetailResponse.from(card);
    }

    @Transactional
    public void deleteCard(Long id) {
        upgradeCardRepository.delete(findCard(id));
    }

    /** 카드를 id로 찾는다. */
    public UpgradeCard findCard(Long id) {
        return upgradeCardRepository
                .findById(id)
                .orElseThrow(() -> ErrorCode.UPGRADE_CARD_NOT_FOUND.exception(id));
    }

    private Skill findSkill(Long id) {
        return skillRepository
                .findById(id)
                .orElseThrow(() -> ErrorCode.SKILL_NOT_FOUND.exception(id));
    }

    private void validateMinBattleLevel(int minBattleLevel, Skill skill) {
        int max = skill.getMaxLevel() + 1;
        if (minBattleLevel < 1 || minBattleLevel > max) {
            throw ErrorCode.CARD_MIN_BATTLE_LEVEL_INVALID.exception(max, minBattleLevel);
        }
    }
}
