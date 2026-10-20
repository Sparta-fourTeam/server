package com.novaserver.friend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.novaserver.friend.FriendStatus;
import com.novaserver.friend.dto.FriendRequestResponse;
import com.novaserver.friend.dto.FriendResponse;
import com.novaserver.friend.entity.Friend;
import com.novaserver.friend.repository.FriendRepository;
import com.novaserver.user.Provider;
import com.novaserver.user.entity.User;
import com.novaserver.user.repository.UserRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class FriendServiceTest {

    @Mock private FriendRepository friendRepository;
    @Mock private UserRepository userRepository;

    @InjectMocks private FriendService friendService;

    @Test
    @DisplayName("친구 관계가 없으면 신청을 새로 만든다")
    void shouldCreateRequest_onSendRequest() {
        // given
        User me = withId(new User(Provider.GUEST, "guest-1"), 1L);
        User target = withId(new User(Provider.GUEST, "guest-2"), 2L);
        when(userRepository.existsById(2L)).thenReturn(true);
        when(friendRepository.findBetweenUsersForUpdate(1L, 2L)).thenReturn(Optional.empty());
        when(userRepository.getReferenceById(1L)).thenReturn(me);
        when(userRepository.getReferenceById(2L)).thenReturn(target);
        when(friendRepository.save(any(Friend.class)))
                .thenAnswer(
                        invocation -> {
                            Friend friend = invocation.getArgument(0);
                            ReflectionTestUtils.setField(friend, "id", 10L);
                            return friend;
                        });

        // when
        FriendRequestResponse response = friendService.sendRequest(1L, 2L);

        // then
        assertThat(response.requestId()).isEqualTo(10L);
        assertThat(response.userId()).isEqualTo(2L);
        assertThat(response.status()).isEqualTo(FriendStatus.PENDING);
    }

    @Test
    @DisplayName("자기 자신에게 신청하면 예외가 발생한다")
    void shouldThrow_whenSelfRequest() {
        // when & then
        assertThatThrownBy(() -> friendService.sendRequest(1L, 1L))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    @DisplayName("존재하지 않는 유저에게 신청하면 예외가 발생한다")
    void shouldThrow_whenTargetUserNotFound() {
        // given
        when(userRepository.existsById(999L)).thenReturn(false);

        // when & then
        assertThatThrownBy(() -> friendService.sendRequest(1L, 999L))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    @DisplayName("이미 친구인 유저에게 신청하면 예외가 발생한다")
    void shouldThrow_whenAlreadyFriend() {
        // given
        User me = withId(new User(Provider.GUEST, "guest-1"), 1L);
        User target = withId(new User(Provider.GUEST, "guest-2"), 2L);
        Friend accepted = new Friend(me, target);
        accepted.accept();
        when(userRepository.existsById(2L)).thenReturn(true);
        when(friendRepository.findBetweenUsersForUpdate(1L, 2L)).thenReturn(Optional.of(accepted));

        // when & then
        assertThatThrownBy(() -> friendService.sendRequest(1L, 2L))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    @DisplayName("이미 내가 보낸 신청이 대기 중이면 예외가 발생한다")
    void shouldThrow_whenRequestAlreadySent() {
        // given
        User me = withId(new User(Provider.GUEST, "guest-1"), 1L);
        User target = withId(new User(Provider.GUEST, "guest-2"), 2L);
        Friend pending = new Friend(me, target);
        when(userRepository.existsById(2L)).thenReturn(true);
        when(friendRepository.findBetweenUsersForUpdate(1L, 2L)).thenReturn(Optional.of(pending));

        // when & then
        assertThatThrownBy(() -> friendService.sendRequest(1L, 2L))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    @DisplayName("상대가 이미 나에게 신청한 상태면 바로 수락해 친구가 된다")
    void shouldAutoAccept_onMutualRequest() {
        // given
        User me = withId(new User(Provider.GUEST, "guest-1"), 1L);
        User target = withId(new User(Provider.GUEST, "guest-2"), 2L);
        Friend reversePending = new Friend(target, me);
        ReflectionTestUtils.setField(reversePending, "id", 10L);
        when(userRepository.existsById(2L)).thenReturn(true);
        when(friendRepository.findBetweenUsersForUpdate(1L, 2L))
                .thenReturn(Optional.of(reversePending));

        // when
        FriendRequestResponse response = friendService.sendRequest(1L, 2L);

        // then
        assertThat(response.status()).isEqualTo(FriendStatus.ACCEPTED);
        assertThat(response.userId()).isEqualTo(2L);
        verify(friendRepository, never()).save(any());
    }

    @Test
    @DisplayName("받은 신청을 수락하면 친구가 된다")
    void shouldAcceptRequest_onAcceptRequest() {
        // given
        User me = withId(new User(Provider.GUEST, "guest-1"), 1L);
        User requester = withId(new User(Provider.GUEST, "guest-2"), 2L);
        Friend pending = new Friend(requester, me);
        ReflectionTestUtils.setField(pending, "id", 10L);
        when(friendRepository.findByIdAndReceiverIdForUpdate(10L, 1L))
                .thenReturn(Optional.of(pending));

        // when
        FriendResponse response = friendService.acceptRequest(1L, 10L);

        // then
        assertThat(response.userId()).isEqualTo(2L);
        assertThat(pending.getStatus()).isEqualTo(FriendStatus.ACCEPTED);
    }

    @Test
    @DisplayName("존재하지 않는 신청을 수락하면 예외가 발생한다")
    void shouldThrow_whenAcceptingMissingRequest() {
        // given
        when(friendRepository.findByIdAndReceiverIdForUpdate(10L, 1L)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> friendService.acceptRequest(1L, 10L))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    @DisplayName("받은 신청을 거절하면 신청을 지운다")
    void shouldDeleteRequest_onRejectRequest() {
        // given
        User me = withId(new User(Provider.GUEST, "guest-1"), 1L);
        User requester = withId(new User(Provider.GUEST, "guest-2"), 2L);
        Friend pending = new Friend(requester, me);
        ReflectionTestUtils.setField(pending, "id", 10L);
        when(friendRepository.findByIdAndReceiverIdForUpdate(10L, 1L))
                .thenReturn(Optional.of(pending));

        // when
        friendService.rejectRequest(1L, 10L);

        // then
        verify(friendRepository).delete(pending);
    }

    @Test
    @DisplayName("친구 목록을 조회하면 상대방 기준 정보를 반환한다")
    void shouldReturnFriends_onGetFriends() {
        // given
        User me = withId(new User(Provider.GUEST, "guest-1"), 1L);
        User friendUser = withId(new User(Provider.GUEST, "guest-2"), 2L);
        Friend accepted = new Friend(me, friendUser);
        accepted.accept();
        when(friendRepository.findAllByStatusAndEitherUser(FriendStatus.ACCEPTED, 1L))
                .thenReturn(List.of(accepted));

        // when
        List<FriendResponse> responses = friendService.getFriends(1L);

        // then
        assertThat(responses).hasSize(1);
        assertThat(responses.getFirst().userId()).isEqualTo(2L);
    }

    @Test
    @DisplayName("받은 신청 목록을 조회하면 신청자 기준 정보를 반환한다")
    void shouldReturnReceivedRequests_onGetReceivedRequests() {
        // given
        User me = withId(new User(Provider.GUEST, "guest-1"), 1L);
        User requester = withId(new User(Provider.GUEST, "guest-2"), 2L);
        Friend pending = new Friend(requester, me);
        when(friendRepository.findAllByReceiverIdAndStatusOrderByCreatedAtDesc(
                        1L, FriendStatus.PENDING))
                .thenReturn(List.of(pending));

        // when
        List<FriendRequestResponse> responses = friendService.getReceivedRequests(1L);

        // then
        assertThat(responses).hasSize(1);
        assertThat(responses.getFirst().userId()).isEqualTo(2L);
        assertThat(responses.getFirst().status()).isEqualTo(FriendStatus.PENDING);
    }

    private static User withId(User user, Long id) {
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }
}
