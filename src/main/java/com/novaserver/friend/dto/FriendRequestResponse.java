package com.novaserver.friend.dto;

import com.novaserver.friend.FriendStatus;
import com.novaserver.friend.entity.Friend;
import com.novaserver.user.entity.User;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;

/** 친구 신청 한 건. 받은 신청 목록 조회와 신청 보내기 응답 양쪽에 쓰인다. */
public record FriendRequestResponse(
        @Schema(description = "신청 id") Long requestId,
        @Schema(description = "상대방(신청을 보냈거나 받은 유저)의 id") Long userId,
        String nickname,
        int avatarId,
        FriendStatus status,
        @Schema(description = "신청 시각(ISO8601 Z)") Instant createdAt) {
    /** 친구 신청을 viewerId 기준 상대방 정보로 변환한다. */
    public static FriendRequestResponse from(Friend friend, Long viewerId) {
        User counterpart = counterpartOf(friend, viewerId);
        return new FriendRequestResponse(
                friend.getId(),
                counterpart.getId(),
                counterpart.getNickname(),
                counterpart.getAvatarId(),
                friend.getStatus(),
                friend.getCreatedAt());
    }

    private static User counterpartOf(Friend friend, Long viewerId) {
        return friend.getRequester().getId().equals(viewerId)
                ? friend.getReceiver()
                : friend.getRequester();
    }
}
