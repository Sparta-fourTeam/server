package com.novaserver.mail.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.novaserver.item.entity.Item;
import com.novaserver.item.repository.ItemRepository;
import com.novaserver.mail.dto.MailBroadcastRequest;
import com.novaserver.mail.dto.MailBulkRequest;
import com.novaserver.mail.dto.MailItemRequest;
import com.novaserver.mail.dto.MailRequest;
import com.novaserver.mail.dto.MailResponse;
import com.novaserver.mail.entity.Mail;
import com.novaserver.mail.repository.MailRepository;
import com.novaserver.user.Provider;
import com.novaserver.user.entity.User;
import com.novaserver.user.repository.UserRepository;
import com.novaserver.user.service.UserRewardService;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class MailServiceTest {

    @Mock private MailRepository mailRepository;
    @Mock private UserRepository userRepository;
    @Mock private ItemRepository itemRepository;
    @Mock private UserRewardService userRewardService;
    @Mock private Clock clock;

    @InjectMocks private MailService mailService;

    @Test
    @DisplayName("아이템이 첨부된 우편을 보내면 제목·내용과 함께 아이템 목록도 저장한다")
    void shouldSaveMailWithItems_onSendMail() {
        // given
        MailItemRequest itemRequest = new MailItemRequest();
        ReflectionTestUtils.setField(itemRequest, "itemId", "ITEM_POTION");
        ReflectionTestUtils.setField(itemRequest, "quantity", 3);
        MailRequest request = new MailRequest();
        ReflectionTestUtils.setField(request, "userId", 1L);
        ReflectionTestUtils.setField(request, "title", "출석 보상");
        ReflectionTestUtils.setField(request, "content", "감사합니다");
        ReflectionTestUtils.setField(request, "items", List.of(itemRequest));

        User user = withId(new User(Provider.GUEST, "guest-1"), 1L);
        Item potion = new Item("ITEM_POTION", null);
        when(userRepository.existsById(1L)).thenReturn(true);
        when(userRepository.getReferenceById(1L)).thenReturn(user);
        when(itemRepository.findAllById(Set.of("ITEM_POTION"))).thenReturn(List.of(potion));
        when(mailRepository.save(any(Mail.class)))
                .thenAnswer(
                        invocation -> {
                            Mail mail = invocation.getArgument(0);
                            ReflectionTestUtils.setField(mail, "id", 10L);
                            ReflectionTestUtils.setField(
                                    mail, "createdAt", Instant.parse("2026-10-19T00:00:00Z"));
                            return mail;
                        });

        // when
        MailResponse response = mailService.sendMail(request);

        // then
        assertThat(response.id()).isEqualTo(10L);
        assertThat(response.title()).isEqualTo("출석 보상");
        assertThat(response.content()).isEqualTo("감사합니다");
        assertThat(response.claimed()).isFalse();
        assertThat(response.items()).hasSize(1);
        assertThat(response.items().getFirst().itemId()).isEqualTo("ITEM_POTION");
        assertThat(response.items().getFirst().quantity()).isEqualTo(3);
    }

    @Test
    @DisplayName("존재하지 않는 유저에게 보내면 예외가 발생한다")
    void shouldThrow_whenUserNotFound() {
        // given
        MailRequest request = new MailRequest();
        ReflectionTestUtils.setField(request, "userId", 999L);
        ReflectionTestUtils.setField(request, "title", "제목");
        ReflectionTestUtils.setField(request, "content", "내용");
        when(userRepository.existsById(999L)).thenReturn(false);

        // when & then
        assertThatThrownBy(() -> mailService.sendMail(request))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    @DisplayName("우편을 수령하면 첨부 아이템을 지급하고 수령 처리한다")
    void shouldGrantItemsAndMarkClaimed_onClaimMail() {
        // given
        User user = withId(new User(Provider.GUEST, "guest-1"), 1L);
        Mail mail = new Mail(user, "출석 보상", "감사합니다", 0, 0, 0, 0);
        mail.addItem(new Item("ITEM_POTION", null), 3);
        ReflectionTestUtils.setField(mail, "id", 10L);
        when(mailRepository.findByIdAndUserIdForUpdate(10L, 1L)).thenReturn(Optional.of(mail));

        // when
        MailResponse response = mailService.claimMail(1L, 10L);

        // then
        assertThat(response.claimed()).isTrue();
        verify(userRewardService).grant(1L, 0, 0, 0, 0, Map.of("ITEM_POTION", 3));
    }

    @Test
    @DisplayName("경험치·골드·젬·에너지가 포함된 우편을 보내면 전부 함께 저장한다")
    void shouldSaveExpGoldGemEnergy_onSendMail() {
        // given
        MailRequest request = new MailRequest();
        ReflectionTestUtils.setField(request, "userId", 1L);
        ReflectionTestUtils.setField(request, "title", "출석 보상");
        ReflectionTestUtils.setField(request, "content", "감사합니다");
        ReflectionTestUtils.setField(request, "exp", 50);
        ReflectionTestUtils.setField(request, "gold", 100);
        ReflectionTestUtils.setField(request, "gem", 10);
        ReflectionTestUtils.setField(request, "energy", 5);

        User user = withId(new User(Provider.GUEST, "guest-1"), 1L);
        when(userRepository.existsById(1L)).thenReturn(true);
        when(userRepository.getReferenceById(1L)).thenReturn(user);
        when(mailRepository.save(any(Mail.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // when
        MailResponse response = mailService.sendMail(request);

        // then
        assertThat(response.exp()).isEqualTo(50);
        assertThat(response.gold()).isEqualTo(100);
        assertThat(response.gem()).isEqualTo(10);
        assertThat(response.energy()).isEqualTo(5);
    }

    @Test
    @DisplayName("경험치·골드·젬·에너지가 포함된 우편을 수령하면 아이템 없이도 전부 지급한다")
    void shouldGrantExpGoldGemEnergy_onClaimMail() {
        // given
        User user = withId(new User(Provider.GUEST, "guest-1"), 1L);
        Mail mail = new Mail(user, "출석 보상", "감사합니다", 50, 100, 10, 5);
        ReflectionTestUtils.setField(mail, "id", 10L);
        when(mailRepository.findByIdAndUserIdForUpdate(10L, 1L)).thenReturn(Optional.of(mail));

        // when
        MailResponse response = mailService.claimMail(1L, 10L);

        // then
        assertThat(response.claimed()).isTrue();
        verify(userRewardService).grant(1L, 50, 100, 10, 5, Map.of());
    }

    @Test
    @DisplayName("이미 수령한 우편을 다시 수령하면 예외가 발생한다")
    void shouldThrow_whenAlreadyClaimed() {
        // given
        User user = withId(new User(Provider.GUEST, "guest-1"), 1L);
        Mail mail = new Mail(user, "출석 보상", "감사합니다", 0, 0, 0, 0);
        mail.claim();
        ReflectionTestUtils.setField(mail, "id", 10L);
        when(mailRepository.findByIdAndUserIdForUpdate(10L, 1L)).thenReturn(Optional.of(mail));

        // when & then
        assertThatThrownBy(() -> mailService.claimMail(1L, 10L))
                .isInstanceOf(ResponseStatusException.class);
        verify(userRewardService, never())
                .grant(any(), anyInt(), anyInt(), anyInt(), anyInt(), anyMap());
    }

    @Test
    @DisplayName("등록된 지 30일 지난 우편을 수령 여부와 상관없이 지운다")
    void shouldDeleteMailsOlderThanThirtyDays() {
        // given
        when(clock.instant()).thenReturn(Instant.parse("2026-11-19T04:00:00Z"));
        User user = withId(new User(Provider.GUEST, "guest-1"), 1L);
        Mail old = new Mail(user, "출석 보상", "감사합니다", 0, 0, 0, 0);
        when(mailRepository.findAllByCreatedAtBefore(Instant.parse("2026-10-20T04:00:00Z")))
                .thenReturn(List.of(old));

        // when
        mailService.deleteOldMails();

        // then
        verify(mailRepository).deleteAll(List.of(old));
    }

    @Test
    @DisplayName("유저의 우편함을 조회하면 최신순 목록을 반환한다")
    void shouldReturnMails_onGetMails() {
        // given
        User user = withId(new User(Provider.GUEST, "guest-1"), 1L);
        Mail mail = new Mail(user, "출석 보상", "감사합니다", 0, 0, 0, 0);
        when(mailRepository.findAllByUserIdOrderByCreatedAtDesc(1L)).thenReturn(List.of(mail));

        // when
        List<MailResponse> responses = mailService.getMails(1L);

        // then
        assertThat(responses).hasSize(1);
        assertThat(responses.getFirst().title()).isEqualTo("출석 보상");
    }

    @Test
    @DisplayName("대량 발송하면 존재하는 유저에게만 보내고 보낸 수만큼 반환한다")
    void shouldSendOnlyToExistingUsers_onSendBulkMail() {
        // given
        MailBulkRequest request = new MailBulkRequest();
        ReflectionTestUtils.setField(request, "userIds", List.of(1L, 999L));
        ReflectionTestUtils.setField(request, "title", "이벤트 보상");
        ReflectionTestUtils.setField(request, "content", "참여 감사합니다");
        ReflectionTestUtils.setField(request, "gold", 50);

        User user = withId(new User(Provider.GUEST, "guest-1"), 1L);
        when(userRepository.findAllById(List.of(1L, 999L))).thenReturn(List.of(user));
        when(mailRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        // when
        List<MailResponse> sent = mailService.sendBulkMail(request);

        // then
        assertThat(sent).hasSize(1);
        assertThat(sent.getFirst().userId()).isEqualTo(1L);
        assertThat(sent.getFirst().gold()).isEqualTo(50);
    }

    @Test
    @DisplayName("전체 발송하면 모든 유저에게 같은 우편을 보낸다")
    void shouldSendToAllUsers_onSendBroadcastMail() {
        // given
        MailBroadcastRequest request = new MailBroadcastRequest();
        ReflectionTestUtils.setField(request, "title", "점검 보상");
        ReflectionTestUtils.setField(request, "content", "불편을 드려 죄송합니다");
        ReflectionTestUtils.setField(request, "gold", 200);

        User userA = withId(new User(Provider.GUEST, "guest-a"), 1L);
        User userB = withId(new User(Provider.GUEST, "guest-b"), 2L);
        when(userRepository.findAll()).thenReturn(List.of(userA, userB));
        when(mailRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        // when
        List<MailResponse> sent = mailService.sendBroadcastMail(request);

        // then
        assertThat(sent).hasSize(2);
        assertThat(sent).extracting(MailResponse::userId).containsExactlyInAnyOrder(1L, 2L);
        assertThat(sent).allMatch(response -> response.gold() == 200);
    }

    private static User withId(User user, Long id) {
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }
}
