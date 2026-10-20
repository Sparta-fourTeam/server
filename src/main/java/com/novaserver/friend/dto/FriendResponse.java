package com.novaserver.friend.dto;

import com.novaserver.friend.entity.Friend;
import com.novaserver.user.entity.User;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

/** 친구 목록 조회에 쓰이는, 상대방 기준으로 변환된 친구 정보. */
public record FriendResponse(
        @Schema(description = "친구의 유저 id") Long userId,
        String nickname,
        int avatarId,
        @Schema(description = "친구가 된 시각(ISO8601 Z)") Instant friendsSince) {
    /** 친구 관계를 viewerId 기준 상대방 정보로 변환한다. */
    public static FriendResponse from(Friend friend, Long viewerId) {
        User counterpart = counterpartOf(friend, viewerId);
        return new FriendResponse(
                counterpart.getId(),
                counterpart.getNickname(),
                counterpart.getAvatarId(),
                friend.getUpdatedAt());
    }

    private static User counterpartOf(Friend friend, Long viewerId) {
        return friend.getRequester().getId().equals(viewerId)
                ? friend.getReceiver()
                : friend.getRequester();
    }
}
