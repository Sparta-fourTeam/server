package com.novaserver.friend.service;

import com.novaserver.friend.FriendStatus;
import com.novaserver.friend.dto.FriendCandidateResponse;
import com.novaserver.friend.dto.FriendRequestResponse;
import com.novaserver.friend.dto.FriendResponse;
import com.novaserver.friend.entity.Friend;
import com.novaserver.friend.repository.FriendRepository;
import com.novaserver.global.error.ErrorCode;
import com.novaserver.user.entity.User;
import com.novaserver.user.repository.UserRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 친구 신청·수락·거절과 친구·신청 목록 조회. */
@Service
@RequiredArgsConstructor
public class FriendService {
    private static final int CANDIDATE_COUNT = 10;

    private final FriendRepository friendRepository;
    private final UserRepository userRepository;

    /** 내 친구 목록을 최근 성사순으로 반환한다. */
    public List<FriendResponse> getFriends(Long userId) {
        return friendRepository.findAllByStatusAndEitherUser(FriendStatus.ACCEPTED, userId).stream()
                .map(friend -> FriendResponse.from(friend, userId))
                .toList();
    }

    /** 내가 받은 친구 신청 목록을 최근 신청순으로 반환한다. */
    public List<FriendRequestResponse> getReceivedRequests(Long userId) {
        return friendRepository
                .findAllByReceiverIdAndStatusOrderByCreatedAtDesc(userId, FriendStatus.PENDING)
                .stream()
                .map(friend -> FriendRequestResponse.from(friend, userId))
                .toList();
    }

    /** 친구 찾기용으로 나와 신청·친구 관계가 없는 유저를 무작위로 반환한다. */
    public List<FriendCandidateResponse> getCandidates(Long userId) {
        return friendRepository.findRandomNonFriends(userId, CANDIDATE_COUNT).stream()
                .map(FriendCandidateResponse::from)
                .toList();
    }

    /** 친구 신청을 보낸다. 상대가 이미 나에게 신청한 상태(맞신청)면 그 신청을 바로 수락해 친구로 만든다. */
    @Transactional
    public FriendRequestResponse sendRequest(Long userId, Long targetUserId) {
        if (userId.equals(targetUserId)) {
            throw ErrorCode.FRIEND_SELF_REQUEST.exception(targetUserId);
        }
        if (!userRepository.existsById(targetUserId)) {
            throw ErrorCode.USER_NOT_FOUND.exception(targetUserId);
        }

        Friend friend =
                friendRepository
                        .findBetweenUsersForUpdate(userId, targetUserId)
                        .map(existing -> handleExistingRelation(existing, userId, targetUserId))
                        .orElseGet(() -> createRequest(userId, targetUserId));

        return FriendRequestResponse.from(friend, userId);
    }

    private Friend handleExistingRelation(Friend existing, Long userId, Long targetUserId) {
        if (existing.getStatus() == FriendStatus.ACCEPTED) {
            throw ErrorCode.FRIEND_ALREADY_FRIEND.exception(targetUserId);
        }
        if (existing.getRequester().getId().equals(userId)) {
            throw ErrorCode.FRIEND_REQUEST_ALREADY_SENT.exception(targetUserId);
        }
        existing.accept();
        return existing;
    }

    private Friend createRequest(Long userId, Long targetUserId) {
        User requester = userRepository.getReferenceById(userId);
        User receiver = userRepository.getReferenceById(targetUserId);
        return friendRepository.save(new Friend(requester, receiver));
    }

    /** 받은 친구 신청을 수락해 친구로 만든다. */
    @Transactional
    public FriendResponse acceptRequest(Long userId, Long requestId) {
        Friend friend = findOwnPendingRequest(userId, requestId);
        friend.accept();
        return FriendResponse.from(friend, userId);
    }

    /** 받은 친구 신청을 거절한다. 거절된 신청은 다시 신청할 수 있도록 삭제한다. */
    @Transactional
    public void rejectRequest(Long userId, Long requestId) {
        Friend friend = findOwnPendingRequest(userId, requestId);
        friendRepository.delete(friend);
    }

    private Friend findOwnPendingRequest(Long userId, Long requestId) {
        Friend friend =
                friendRepository
                        .findByIdAndReceiverIdForUpdate(requestId, userId)
                        .orElseThrow(() -> ErrorCode.FRIEND_REQUEST_NOT_FOUND.exception(requestId));
        if (friend.getStatus() == FriendStatus.ACCEPTED) {
            throw ErrorCode.FRIEND_ALREADY_FRIEND.exception(friend.getRequester().getId());
        }
        return friend;
    }
}
