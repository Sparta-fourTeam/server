package com.novaserver.friend.dto;

import com.novaserver.user.entity.User;
import io.swagger.v3.oas.annotations.media.Schema;

/** 친구 찾기 화면에 보여줄, 아직 친구가 아닌 유저. */
public record FriendCandidateResponse(
        @Schema(description = "후보 유저 id") Long userId, String nickname, int avatarId) {
    /** 유저를 친구 찾기 후보 응답으로 변환한다. */
    public static FriendCandidateResponse from(User user) {
        return new FriendCandidateResponse(user.getId(), user.getNickname(), user.getAvatarId());
    }
}
