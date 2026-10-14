package com.novaserver.cards.service;

import com.novaserver.cards.dto.CardEffectRequest;
import com.novaserver.cards.dto.CardEffectResponse;
import com.novaserver.cards.entity.CardEffect;
import com.novaserver.cards.entity.CardVariant;
import com.novaserver.cards.entity.EffectKind;
import com.novaserver.cards.entity.EffectTrigger;
import com.novaserver.cards.entity.SkillForm;
import com.novaserver.cards.entity.UpgradeCard;
import com.novaserver.cards.repository.CardEffectRepository;
import com.novaserver.cards.repository.EffectKindRepository;
import com.novaserver.cards.repository.EffectTriggerRepository;
import com.novaserver.cards.repository.SkillFormRepository;
import com.novaserver.global.error.ErrorCode;
import com.novaserver.skill.entity.Skill;
import com.novaserver.skill.repository.SkillRepository;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 카드 효과 관리 로직. */
@Service
@RequiredArgsConstructor
public class CardEffectService {
    private final CardEffectRepository cardEffectRepository;
    private final CardService cardService;
    private final CardVariantService cardVariantService;
    private final EffectKindRepository effectKindRepository;
    private final SkillFormRepository skillFormRepository;
    private final EffectTriggerRepository effectTriggerRepository;
    private final SkillRepository skillRepository;

    /** 카드에 바로 붙는 효과를 정렬 순서대로 조회한다. */
    @Transactional(readOnly = true)
    public List<CardEffectResponse> getCardEffects(Long cardId) {
        cardService.findCard(cardId);
        return cardEffectRepository.findAllByCardIdOrderBySortOrderAsc(cardId).stream()
                .map(CardEffectResponse::from)
                .toList();
    }

    /** 변형에 붙는 효과를 정렬 순서대로 조회한다. */
    @Transactional(readOnly = true)
    public List<CardEffectResponse> getVariantEffects(Long variantId) {
        cardVariantService.findVariant(variantId);
        return cardEffectRepository.findAllByVariantIdOrderBySortOrderAsc(variantId).stream()
                .map(CardEffectResponse::from)
                .toList();
    }

    /** 카드에 바로 붙는 효과를 등록한다. */
    @Transactional
    public CardEffectResponse createCardEffect(Long cardId, CardEffectRequest request) {
        UpgradeCard card = cardService.findCard(cardId);
        return CardEffectResponse.from(cardEffectRepository.save(buildEffect(card, null, request)));
    }

    /** 변형에 붙는 효과를 등록한다. */
    @Transactional
    public CardEffectResponse createVariantEffect(Long variantId, CardEffectRequest request) {
        CardVariant variant = cardVariantService.findVariant(variantId);
        return CardEffectResponse.from(
                cardEffectRepository.save(buildEffect(null, variant, request)));
    }

    /** 효과를 수정한다. */
    @Transactional
    public CardEffectResponse updateEffect(Long id, CardEffectRequest request) {
        CardEffect effect = findEffect(id);
        effect.update(
                request.getSortOrder(),
                findKind(request.getKind()),
                request.getValue(),
                findFormIfPresent(request.getForm()),
                findTriggerIfPresent(request.getTrigger()),
                findSkillIfPresent(request.getSkillId()),
                request.getChance(),
                request.getDamageScale(),
                findSkillIfPresent(request.getTarget()),
                request.getCount(),
                findFormIfPresent(request.getOnlyForm()),
                request.getExcludeHit(),
                request.getInterval(),
                validatedInherit(request.getInherit()));
        return CardEffectResponse.from(effect);
    }

    @Transactional
    public void deleteEffect(Long id) {
        cardEffectRepository.delete(findEffect(id));
    }

    /** 효과를 id로 찾는다. */
    public CardEffect findEffect(Long id) {
        return cardEffectRepository
                .findById(id)
                .orElseThrow(() -> ErrorCode.CARD_EFFECT_NOT_FOUND.exception(id));
    }

    private CardEffect buildEffect(
            UpgradeCard card, CardVariant variant, CardEffectRequest request) {
        return new CardEffect(
                card,
                variant,
                request.getSortOrder(),
                findKind(request.getKind()),
                request.getValue(),
                findFormIfPresent(request.getForm()),
                findTriggerIfPresent(request.getTrigger()),
                findSkillIfPresent(request.getSkillId()),
                request.getChance(),
                request.getDamageScale(),
                findSkillIfPresent(request.getTarget()),
                request.getCount(),
                findFormIfPresent(request.getOnlyForm()),
                request.getExcludeHit(),
                request.getInterval(),
                validatedInherit(request.getInherit()));
    }

    /** 모든 inherit 값이 유한한 숫자인지 확인한다. 아니면 400을 던진다. */
    private Map<String, Double> validatedInherit(Map<String, Double> inherit) {
        if (inherit == null) {
            return null;
        }
        for (Double value : inherit.values()) {
            if (value == null || !Double.isFinite(value)) {
                throw ErrorCode.CARD_EFFECT_INHERIT_VALUE_INVALID.exception(value);
            }
        }
        return inherit;
    }

    private EffectKind findKind(String name) {
        return effectKindRepository
                .findByName(name)
                .orElseThrow(() -> ErrorCode.EFFECT_KIND_NOT_FOUND.exception(name));
    }

    private SkillForm findFormIfPresent(String name) {
        if (name == null) {
            return null;
        }
        return skillFormRepository
                .findByName(name)
                .orElseThrow(() -> ErrorCode.SKILL_FORM_NOT_FOUND.exception(name));
    }

    private EffectTrigger findTriggerIfPresent(String name) {
        if (name == null) {
            return null;
        }
        return effectTriggerRepository
                .findByName(name)
                .orElseThrow(() -> ErrorCode.EFFECT_TRIGGER_NOT_FOUND.exception(name));
    }

    private Skill findSkillIfPresent(Long id) {
        if (id == null) {
            return null;
        }
        return skillRepository
                .findById(id)
                .orElseThrow(() -> ErrorCode.SKILL_NOT_FOUND.exception(id));
    }
}
