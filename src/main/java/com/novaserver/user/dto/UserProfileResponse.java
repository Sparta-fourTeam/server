package com.novaserver.user.dto;

import com.novaserver.user.entity.User;

/** 공개 프로필. 로그인 자격 정보는 포함하지 않는다. */
public record UserProfileResponse(Long userId, String nickname, int avatarId) {
    /** 유저의 공개 필드만 응답으로 변환한다. */
    public static UserProfileResponse from(User user) {
        return new UserProfileResponse(
                user.getId(),
                user.getNickname() == null ? "플레이어" + user.getId() : user.getNickname(),
                user.getAvatarId());
    }
}
