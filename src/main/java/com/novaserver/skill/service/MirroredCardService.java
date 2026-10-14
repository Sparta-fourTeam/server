package com.novaserver.skill.service;

import com.novaserver.cards.entity.UpgradeCard;
import com.novaserver.cards.service.CardService;
import com.novaserver.global.error.ErrorCode;
import com.novaserver.skill.dto.MirroredCardRequest;
import com.novaserver.skill.dto.MirroredCardResponse;
import com.novaserver.skill.entity.MirroredCard;
import com.novaserver.skill.entity.Skill;
import com.novaserver.skill.repository.MirroredCardRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 카드 효과 적용 조건 관리 로직. */
@Service
@RequiredArgsConstructor
public class MirroredCardService {
    private final MirroredCardRepository mirroredCardRepository;
    private final SkillService skillService;
    private final CardService cardService;

    /** 카드 효과 적용 조건 목록을 조회한다. */
    @Transactional(readOnly = true)
    public List<MirroredCardResponse> getMirroredCards(Long skillId) {
        skillService.findSkill(skillId);
        return mirroredCardRepository.findAllBySkillId(skillId).stream()
                .map(MirroredCardResponse::from)
                .toList();
    }

    /** 카드 효과 적용 조건을 등록한다. 없는 스킬·카드 id면 404, 같은 조건이 있으면 409를 던진다. */
    @Transactional
    public MirroredCardResponse createMirroredCard(Long skillId, MirroredCardRequest request) {
        Skill skill = skillService.findSkill(skillId);
        UpgradeCard card = cardService.findCard(request.getCardId());
        if (mirroredCardRepository.existsBySkillIdAndCardId(skillId, request.getCardId())) {
            throw ErrorCode.MIRRORED_CARD_DUPLICATED.exception(request.getCardId());
        }
        MirroredCard saved = mirroredCardRepository.save(new MirroredCard(skill, card));
        return MirroredCardResponse.from(saved);
    }

    @Transactional
    public void deleteMirroredCard(Long id) {
        mirroredCardRepository.delete(findMirroredCard(id));
    }

    private MirroredCard findMirroredCard(Long id) {
        return mirroredCardRepository
                .findById(id)
                .orElseThrow(() -> ErrorCode.MIRRORED_CARD_NOT_FOUND.exception(id));
    }
}
