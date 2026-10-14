package com.novaserver.skill.dto;

import com.novaserver.skill.entity.MirroredCard;

/** 카드 효과 적용 조건 응답. */
public record MirroredCardResponse(Long id, NamedRef sourceSkill, NamedRef card) {

    /** 연관 테이블 값. */
    public record NamedRef(Long id, String name) {}

    public static MirroredCardResponse from(MirroredCard mirroredCard) {
        return new MirroredCardResponse(
                mirroredCard.getId(),
                new NamedRef(
                        mirroredCard.getCard().getSkill().getId(),
                        mirroredCard.getCard().getSkill().getName()),
                new NamedRef(mirroredCard.getCard().getId(), mirroredCard.getCard().getName()));
    }
}
