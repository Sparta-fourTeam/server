package com.novaserver.skill.dto;

import com.novaserver.skill.entity.MirroredCard;

/** 유니티용 카드 효과 적용 조건 데이터 응답. 소속 스킬은 cardId로 찾으므로 내려주지 않는다. */
public record MirroredCardDataResponse(Long cardId) {

    public static MirroredCardDataResponse from(MirroredCard mirroredCard) {
        return new MirroredCardDataResponse(mirroredCard.getCard().getId());
    }
}
