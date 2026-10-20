package com.novaserver.friend.controller;

import com.novaserver.friend.dto.FriendCandidateResponse;
import com.novaserver.friend.dto.FriendRequestResponse;
import com.novaserver.friend.dto.FriendResponse;
import com.novaserver.friend.service.FriendService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 친구 목록·신청·수락·거절·친구 찾기 API. */
@Tag(name = "친구", description = "친구 목록·신청·수락·거절·친구 찾기 API")
@RestController
@RequestMapping("/friends")
@RequiredArgsConstructor
public class FriendController {
    private final FriendService friendService;

    @Operation(summary = "친구 목록 조회", description = "내 친구 목록을 최근 성사순으로 반환합니다.")
    @GetMapping
    public List<FriendResponse> getFriends(@AuthenticationPrincipal Long userId) {
        return friendService.getFriends(userId);
    }

    @Operation(summary = "받은 친구 신청 조회", description = "내가 받은 친구 신청 목록을 최근 신청순으로 반환합니다.")
    @GetMapping("/requests")
    public List<FriendRequestResponse> getReceivedRequests(@AuthenticationPrincipal Long userId) {
        return friendService.getReceivedRequests(userId);
    }

    @Operation(
            summary = "친구 찾기 후보 조회",
            description = "친구 찾기 화면에 보여줄, 나와 신청·친구 관계가 없는 유저를 무작위로 최대 10명 반환합니다.")
    @GetMapping("/candidates")
    public List<FriendCandidateResponse> getCandidates(@AuthenticationPrincipal Long userId) {
        return friendService.getCandidates(userId);
    }

    @Operation(
            summary = "친구 신청",
            description = "대상 유저에게 친구 신청을 보냅니다. 상대가 이미 나에게 신청한 상태라면 바로 친구가 됩니다.")
    @PostMapping("/requests/{targetUserId}")
    public FriendRequestResponse sendRequest(
            @AuthenticationPrincipal Long userId, @PathVariable Long targetUserId) {
        return friendService.sendRequest(userId, targetUserId);
    }

    @Operation(summary = "친구 신청 수락", description = "받은 친구 신청을 수락해 친구가 됩니다.")
    @PostMapping("/requests/{requestId}/accept")
    public FriendResponse accept(
            @AuthenticationPrincipal Long userId, @PathVariable Long requestId) {
        return friendService.acceptRequest(userId, requestId);
    }

    @Operation(summary = "친구 신청 거절", description = "받은 친구 신청을 거절합니다.")
    @PostMapping("/requests/{requestId}/reject")
    public void reject(@AuthenticationPrincipal Long userId, @PathVariable Long requestId) {
        friendService.rejectRequest(userId, requestId);
    }
}
