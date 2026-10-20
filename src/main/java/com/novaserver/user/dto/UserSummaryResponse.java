package com.novaserver.user.dto;

import com.novaserver.user.entity.User;

/** 관리자 유저 검색 결과 요약. */
public record UserSummaryResponse(Long userId, String nickname) {
    public static UserSummaryResponse from(User user) {
        return new UserSummaryResponse(user.getId(), user.getNickname());
    }
}
