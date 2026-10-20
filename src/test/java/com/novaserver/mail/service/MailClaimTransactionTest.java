package com.novaserver.mail.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.novaserver.mail.entity.Mail;
import com.novaserver.mail.repository.MailRepository;
import com.novaserver.user.Provider;
import com.novaserver.user.entity.User;
import com.novaserver.user.repository.UserRepository;
import com.novaserver.user.repository.WalletRepository;
import com.novaserver.user.service.UserService;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

/** 같은 우편에 대한 동시 수령 요청이 실제 DB 잠금으로 한 번만 처리되는지 확인한다. */
@SpringBootTest
class MailClaimTransactionTest {
    @Autowired private MailService mailService;
    @Autowired private MailRepository mailRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private UserService userService;
    @Autowired private WalletRepository walletRepository;
    @Autowired private PlatformTransactionManager transactionManager;

    private TransactionTemplate transaction;
    private Long userId;
    private Long mailId;

    @BeforeEach
    void setUp() {
        transaction = new TransactionTemplate(transactionManager);
        transaction.executeWithoutResult(
                status -> {
                    User user =
                            userRepository.saveAndFlush(
                                    new User(Provider.GUEST, "mail-claim-" + UUID.randomUUID()));
                    userId = user.getId();
                    userService.createInitialState(user);
                    Mail mail = new Mail(user, "출석 보상", "감사합니다", 0, 100, 0, 0);
                    mailId = mailRepository.saveAndFlush(mail).getId();
                });
    }

    @AfterEach
    void tearDown() {
        if (userId != null) {
            transaction.executeWithoutResult(
                    status -> {
                        mailRepository.deleteAll(
                                mailRepository.findAllByUserIdOrderByCreatedAtDesc(userId));
                        userService.deleteAccount(userId);
                    });
        }
    }

    @Test
    @DisplayName("같은 우편에 대한 동시 수령 요청은 한쪽만 성공하고 골드도 한 번만 지급된다")
    void shouldClaimOnlyOnce_whenConcurrentClaimsRaceSameMail() throws Exception {
        // given
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);

        // when
        List<Boolean> results;
        try (var executor = Executors.newFixedThreadPool(2)) {
            Callable<Boolean> claim = () -> claimAfterStart(ready, start);
            var first = executor.submit(claim);
            var second = executor.submit(claim);
            try {
                assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
                start.countDown();
                results =
                        List.of(first.get(15, TimeUnit.SECONDS), second.get(15, TimeUnit.SECONDS));
            } finally {
                start.countDown();
                executor.shutdownNow();
            }
        }

        // then: 둘 중 하나만 성공하고, 골드는 100(우편 1회분)만 지급된다
        assertThat(results).containsExactlyInAnyOrder(true, false);
        assertThat(walletRepository.findByUserId(userId).orElseThrow().getGold()).isEqualTo(100);
    }

    private boolean claimAfterStart(CountDownLatch ready, CountDownLatch start)
            throws InterruptedException {
        ready.countDown();
        if (!start.await(5, TimeUnit.SECONDS)) {
            throw new IllegalStateException("동시 요청 시작 시간 초과");
        }
        try {
            mailService.claimMail(userId, mailId);
            return true;
        } catch (ResponseStatusException e) {
            return false;
        }
    }
}
