package com.novaserver.cards.service;

import com.novaserver.cards.dto.RequiredSkillRequest;
import com.novaserver.cards.dto.RequiredSkillResponse;
import com.novaserver.cards.entity.RequiredSkill;
import com.novaserver.cards.entity.UpgradeCard;
import com.novaserver.cards.repository.RequiredSkillRepository;
import com.novaserver.global.error.ErrorCode;
import com.novaserver.skill.entity.Skill;
import com.novaserver.skill.repository.SkillRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 필요 스킬 조건 관리 로직. */
@Service
@RequiredArgsConstructor
public class RequiredSkillService {
    private final RequiredSkillRepository requiredSkillRepository;
    private final CardService cardService;
    private final SkillRepository skillRepository;

    @Transactional(readOnly = true)
    public List<RequiredSkillResponse> getRequiredSkills(Long cardId) {
        cardService.findCard(cardId);
        return requiredSkillRepository.findAllByCard_Id(cardId).stream()
                .map(RequiredSkillResponse::from)
                .toList();
    }

    /** 필요 스킬 조건을 등록한다. 없는 스킬 id면 404, 같은 조건이 있으면 409를 던진다. */
    @Transactional
    public RequiredSkillResponse createRequiredSkill(Long cardId, RequiredSkillRequest request) {
        UpgradeCard card = cardService.findCard(cardId);
        Skill skill = findSkill(request.getSkillId());
        if (requiredSkillRepository.existsByCard_IdAndSkill_Id(cardId, request.getSkillId())) {
            throw ErrorCode.REQUIRED_SKILL_DUPLICATED.exception(request.getSkillId());
        }
        RequiredSkill saved = requiredSkillRepository.save(new RequiredSkill(card, skill));
        return RequiredSkillResponse.from(saved);
    }

    @Transactional
    public void deleteRequiredSkill(Long id) {
        requiredSkillRepository.delete(findRequiredSkill(id));
    }

    private RequiredSkill findRequiredSkill(Long id) {
        return requiredSkillRepository
                .findById(id)
                .orElseThrow(() -> ErrorCode.REQUIRED_SKILL_NOT_FOUND.exception(id));
    }

    private Skill findSkill(Long id) {
        return skillRepository
                .findById(id)
                .orElseThrow(() -> ErrorCode.SKILL_NOT_FOUND.exception(id));
    }
}
