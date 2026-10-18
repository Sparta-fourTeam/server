package com.novaserver.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.novaserver.auth.jwt.ClientType;
import com.novaserver.auth.jwt.JwtProvider;
import com.novaserver.auth.repository.TokenBlocklistRepository;
import com.novaserver.user.EnergyRecoverySource;
import com.novaserver.user.Provider;
import com.novaserver.user.Role;
import com.novaserver.user.dto.EnergyRecoveryRequest;
import com.novaserver.user.dto.EnergyRecoveryResponse;
import com.novaserver.user.entity.User;
import com.novaserver.user.entity.UserData;
import com.novaserver.user.entity.UserEnergy;
import com.novaserver.user.entity.Wallet;
import com.novaserver.user.repository.UserDataRepository;
import com.novaserver.user.repository.UserEnergyPurchaseStateRepository;
import com.novaserver.user.repository.UserEnergyRecoveryReceiptRepository;
import com.novaserver.user.repository.UserEnergyRepository;
import com.novaserver.user.repository.UserRepository;
import com.novaserver.user.repository.WalletRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

@SpringBootTest
@AutoConfigureMockMvc
class UserEnergyRecoveryTransactionTest {
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 10, 19, 4, 0);
    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");
    private static final int PRICE = EnergyPurchasePolicy.GEM_COST;
    private static final int AMOUNT = EnergyPurchasePolicy.ENERGY_AMOUNT;
    private static final int LIMIT = EnergyPurchasePolicy.DAILY_LIMIT;
    private static final int INITIAL_GEMS = PRICE * (LIMIT + 2);

    @Autowired private UserEnergyRecoveryService purchases;
    @Autowired private UserService users;
    @Autowired private UserRepository userRepository;
    @Autowired private UserDataRepository dataRepository;
    @Autowired private WalletRepository wallets;
    @Autowired private UserEnergyRepository energies;
    @Autowired private UserEnergyPurchaseStateRepository purchaseStates;
    @Autowired private JdbcTemplate jdbc;
    @Autowired private PlatformTransactionManager transactionManager;
    @Autowired private MockMvc mvc;
    @Autowired private JwtProvider jwtProvider;
    @MockitoSpyBean private UserEnergyRecoveryReceiptRepository receipts;
    @MockitoBean private Clock clock;
    @MockitoBean private TokenBlocklistRepository tokenBlocklistRepository;

    private TransactionTemplate transaction;
    private Long userId;

    @BeforeEach
    void setUp() {
        transaction = new TransactionTemplate(transactionManager);
        when(clock.getZone()).thenReturn(SEOUL);
        at(NOW);
        transaction.executeWithoutResult(
                status -> {
                    User user =
                            userRepository.saveAndFlush(
                                    new User(Provider.GUEST, UUID.randomUUID().toString()));
                    userId = user.getId();
                    dataRepository.save(new UserData(user, 0));
                    wallets.save(new Wallet(user, 321, INITIAL_GEMS));
                    energies.save(new UserEnergy(user, 2, NOW.minusSeconds(30)));
                });
    }

    @AfterEach
    void tearDown() {
        if (userId != null) {
            users.deleteAccount(userId);
        }
    }

    @Test
    @DisplayName("새 트랜잭션·다음 영업일 재요청도 최초 영수증을 반환하고 재차 차감하지 않는다")
    void replaysOriginalReceiptAfterLaterPurchaseAndDayChange() {
        at(NOW.withNano(123456789));
        var first = purchases.recover(userId, request("first"));
        purchases.recover(userId, request("second"));
        at(NOW.plusDays(1));

        assertThat(purchases.recover(userId, request("first"))).isEqualTo(first);
        assertThat(first.energyGranted()).isEqualTo(AMOUNT);
        assertThat(first.gemsSpent()).isEqualTo(PRICE);
        assertThat(first.purchaseCount()).isEqualTo(1);
        assertThat(first.purchaseDay()).isEqualTo(NOW.toLocalDate());
        assertThat(first.completedAt()).isEqualTo(Instant.parse("2026-10-18T19:00:00.123456Z"));
        assertPurchaseState(NOW.toLocalDate(), 2);
        purchases.recover(userId, request("next-day"));
        assertThat(purchases.recover(userId, request("first"))).isEqualTo(first);
        assertPurchaseState(NOW.toLocalDate().plusDays(1), 1);
        assertState(INITIAL_GEMS - 3 * PRICE, 2 + 3 * AMOUNT, 3);
    }

    @Test
    @DisplayName("마지막 허용 구매 뒤 새 요청은 거절하지만 같은 키 복구는 허용한다")
    void rejectsNewPurchaseAtDailyLimitButAllowsReplay() {
        EnergyRecoveryResponse last = null;
        for (int i = 0; i < LIMIT; i++) {
            last = purchases.recover(userId, request("limit-" + i));
        }
        assertThat(last.purchaseCount()).isEqualTo(LIMIT);
        assertThatThrownBy(() -> purchases.recover(userId, request("over-limit")))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("일일 에너지 구매 한도");
        assertThat(purchases.recover(userId, request("limit-" + (LIMIT - 1)))).isEqualTo(last);
        assertState(INITIAL_GEMS - LIMIT * PRICE, 2 + LIMIT * AMOUNT, LIMIT);
    }

    @Test
    @DisplayName("서울 03:59:59에는 전일 한도를 유지하고 04:00:00에 새 한도를 사용한다")
    void resetsAtFourAmSeoulRatherThanMidnight() {
        at(NOW.minusSeconds(1));
        for (int i = 0; i < LIMIT; i++) {
            purchases.recover(userId, request("day-" + i));
        }
        assertPurchaseState(NOW.toLocalDate().minusDays(1), LIMIT);
        assertThatThrownBy(() -> purchases.recover(userId, request("before-reset")))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("일일 에너지 구매 한도");
        at(NOW);
        var nextDay = purchases.recover(userId, request("after-reset"));
        assertThat(nextDay.purchaseCount()).isEqualTo(1);
        assertThat(nextDay.purchaseDay()).isEqualTo(NOW.toLocalDate());
        assertPurchaseState(NOW.toLocalDate(), 1);
        assertState(INITIAL_GEMS - (LIMIT + 1) * PRICE, 2 + (LIMIT + 1) * AMOUNT, LIMIT + 1);
    }

    @Test
    @DisplayName("잔액 부족은 자연회복 정산·지갑·일일 횟수·영수증을 변경하지 않는다")
    void rejectsInsufficientGemsWithoutChangingAnyState() {
        setGem(PRICE - 1);
        setEnergy(2, NOW.minusMinutes(20));
        assertThatThrownBy(() -> purchases.recover(userId, request("poor")))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("젬이 부족");
        assertState(PRICE - 1, 2, 0);
        assertThat(purchaseStates.findByUserId(userId)).isEmpty();
        assertThat(energies.findByUserId(userId).orElseThrow().getEnergyUpdatedAt())
                .isEqualTo(NOW.minusMinutes(20));
    }

    @Test
    @DisplayName("영수증 저장 실패 시 이미 수행한 차감·지급·회복 기준 시각도 롤백한다")
    void rollsBackChargeAndGrantWhenReceiptFlushFails() {
        doThrow(new DataIntegrityViolationException("test receipt failure"))
                .when(receipts)
                .saveAndFlush(any());
        setEnergy(2, NOW.minusMinutes(20));
        assertThatThrownBy(() -> purchases.recover(userId, request("rollback")))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertState(INITIAL_GEMS, 2, 0);
        assertThat(purchaseStates.findByUserId(userId)).isEmpty();
        assertThat(energies.findByUserId(userId).orElseThrow().getEnergyUpdatedAt())
                .isEqualTo(NOW.minusMinutes(20));
    }

    @Test
    @DisplayName("자연회복을 먼저 정산하고 남은 부분 회복 시간을 보존한다")
    void settlesNaturalRecoveryBeforeGrantAndKeepsPartialInterval() {
        setEnergy(2, NOW.minusSeconds(630));
        purchases.recover(userId, request("natural"));
        assertState(INITIAL_GEMS - PRICE, 3 + AMOUNT, 1);
        assertThat(energies.findByUserId(userId).orElseThrow().getEnergyUpdatedAt())
                .isEqualTo(NOW.minusSeconds(30));
    }

    @Test
    @DisplayName("상한에서도 구매 에너지 전량을 지급하고 상한 아래로 소모한 뒤 타이머를 시작한다")
    void grantsFullPurchaseOverNaturalCapAndRestartsTimerAfterSpend() {
        setEnergy(UserEnergy.MAX_ENERGY, NOW.minusDays(3));
        purchases.recover(userId, request("over-natural-cap"));
        assertState(INITIAL_GEMS - PRICE, UserEnergy.MAX_ENERGY + AMOUNT, 1);
        users.useEnergy(userId, AMOUNT + 2);
        at(NOW.plusSeconds(599));
        assertThat(users.getMe(userId).getEnergyStored()).isEqualTo(UserEnergy.MAX_ENERGY - 2);
        at(NOW.plusSeconds(600));
        assertThat(users.getMe(userId).getEnergyStored()).isEqualTo(UserEnergy.MAX_ENERGY - 1);
    }

    @Test
    @DisplayName("정수 저장 경계까지 전량 지급하고 초과 구매는 차감 없이 거절한다")
    void rejectsOverflowWithoutTakingPaymentOrLosingPaidEnergy() {
        setEnergy(Integer.MAX_VALUE - AMOUNT, NOW);
        purchases.recover(userId, request("exact-storage-limit"));
        assertThatThrownBy(() -> purchases.recover(userId, request("storage-overflow")))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("에너지 저장 범위");
        assertState(INITIAL_GEMS - PRICE, Integer.MAX_VALUE, 1);
    }

    @Test
    @DisplayName("검증 없는 광고는 지급하지 않고 구매 키를 광고로 바꿔 재사용하지 못한다")
    void blocksUnverifiedAdsAndConflictingSourceReuse() {
        assertThatThrownBy(
                        () ->
                                purchases.recover(
                                        userId,
                                        new EnergyRecoveryRequest("ad", EnergyRecoverySource.AD)))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("503");
        assertState(INITIAL_GEMS, 2, 0);
        purchases.recover(userId, request("purchase"));
        assertThatThrownBy(
                        () ->
                                purchases.recover(
                                        userId,
                                        new EnergyRecoveryRequest(
                                                "purchase", EnergyRecoverySource.AD)))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("409");
        assertState(INITIAL_GEMS - PRICE, 2 + AMOUNT, 1);
    }

    @Test
    @DisplayName("두 동시 동일 키 요청은 같은 영수증을 받고 한 번만 차감·지급된다")
    void serializesConcurrentDuplicateRequests() throws Exception {
        var results = concurrently("same-key", "same-key");
        assertThat(results.get(0))
                .isInstanceOf(EnergyRecoveryResponse.class)
                .isEqualTo(results.get(1));
        assertState(INITIAL_GEMS - PRICE, 2 + AMOUNT, 1);
        assertPurchaseState(NOW.toLocalDate(), 1);
    }

    @Test
    @DisplayName("일일 한도가 한 번 남았을 때 다른 키 동시 구매도 한 건만 성공한다")
    void serializesDifferentKeysAtRemainingDailyLimit() throws Exception {
        for (int i = 0; i < LIMIT - 1; i++) {
            purchases.recover(userId, request("used-" + i));
        }
        var results = concurrently("last-left", "last-right");
        assertThat(results.stream().filter(EnergyRecoveryResponse.class::isInstance).count())
                .isEqualTo(1);
        assertThat(results).contains(409);
        assertState(INITIAL_GEMS - LIMIT * PRICE, 2 + LIMIT * AMOUNT, LIMIT);
        assertPurchaseState(NOW.toLocalDate(), LIMIT);
    }

    @Test
    @DisplayName("실제 JWT 계정 구매와 새 상태 조회·응답 유실 재요청이 영구 결과와 일치한다")
    void authenticatesHttpPurchaseAndReplaysPersistedReceipt() throws Exception {
        String body = "{\"createId\":\"http\",\"source\":\"Purchase\"}";
        String first =
                mvc.perform(
                                post("/api/user/energy/recover")
                                        .header("Authorization", authorization())
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(body))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.source").value("Purchase"))
                        .andExpect(jsonPath("$.energyGranted").value(AMOUNT))
                        .andExpect(jsonPath("$.gemsSpent").value(PRICE))
                        .andExpect(jsonPath("$.completedAt").value("2026-10-18T19:00:00Z"))
                        .andReturn()
                        .getResponse()
                        .getContentAsString();
        String replay =
                mvc.perform(
                                post("/api/user/energy/recover")
                                        .header("Authorization", authorization())
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(body))
                        .andExpect(status().isOk())
                        .andReturn()
                        .getResponse()
                        .getContentAsString();
        assertThat(replay).isEqualTo(first);
        mvc.perform(get("/api/user/me").header("Authorization", authorization()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.gem").value(INITIAL_GEMS - PRICE))
                .andExpect(jsonPath("$.energyStored").value(2 + AMOUNT));
        assertState(INITIAL_GEMS - PRICE, 2 + AMOUNT, 1);
    }

    @Test
    @DisplayName("같은 키도 계정별로 구분하고 본문의 가격·지급량·계정ID로 정책을 덮어쓰지 못한다")
    void isolatesAccountKeysAndIgnoresForgedPriceAmountAndUser() throws Exception {
        Long other =
                transaction.execute(
                        status -> {
                            User user =
                                    userRepository.saveAndFlush(
                                            new User(Provider.GUEST, UUID.randomUUID().toString()));
                            users.createInitialState(user);
                            ReflectionTestUtils.setField(
                                    wallets.findByUserId(user.getId()).orElseThrow(),
                                    "gem",
                                    INITIAL_GEMS);
                            return user.getId();
                        });
        try {
            String body =
                    "{\"createId\":\"shared\",\"source\":\"Purchase\","
                            + "\"userId\":"
                            + other
                            + ",\"price\":0,\"energyAmount\":999999}";
            mvc.perform(
                            post("/api/user/energy/recover")
                                    .header("Authorization", authorization())
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(body))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.energyGranted").value(AMOUNT))
                    .andExpect(jsonPath("$.gemsSpent").value(PRICE));
            assertThat(wallets.findByUserId(other).orElseThrow().getGem()).isEqualTo(INITIAL_GEMS);
            assertThat(receipts.findByUserIdAndCreateId(other, "shared")).isEmpty();
            purchases.recover(other, request("shared"));
            assertThat(wallets.findByUserId(other).orElseThrow().getGem())
                    .isEqualTo(INITIAL_GEMS - PRICE);
            assertThat(receipts.findByUserIdAndCreateId(other, "shared")).isPresent();
            assertState(INITIAL_GEMS - PRICE, 2 + AMOUNT, 1);
        } finally {
            users.deleteAccount(other);
        }
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                "{}",
                "{\"createId\":\" \",\"source\":\"Purchase\"}",
                "{\"createId\":\"x\",\"source\":\"Unknown\"}"
            })
    @DisplayName("HTTP 입력 누락·잘못된 경로 값은 차감 전에 거절한다")
    void rejectsMalformedHttpRequestBeforeMutation(String body) throws Exception {
        mvc.perform(
                        post("/api/user/energy/recover")
                                .header("Authorization", authorization())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body))
                .andExpect(status().isBadRequest());
        assertState(INITIAL_GEMS, 2, 0);
    }

    @Test
    @DisplayName("인증 없는 요청과 서버 검증 없는 광고 요청은 지급할 수 없다")
    void rejectsUnauthenticatedAndUnverifiedAdHttpRequests() throws Exception {
        mvc.perform(
                        post("/api/user/energy/recover")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"createId\":\"anonymous\",\"source\":\"Purchase\"}"))
                .andExpect(status().isUnauthorized());
        mvc.perform(
                        post("/api/user/energy/recover")
                                .header("Authorization", authorization())
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"createId\":\"fake-ad\",\"source\":\"Ad\"}"))
                .andExpect(status().isServiceUnavailable());
        assertState(INITIAL_GEMS, 2, 0);
    }

    @Test
    @DisplayName("계정 삭제는 새 회복 영수증도 제거해 외래키·잔여 기록을 남기지 않는다")
    void deletesReceiptsTogetherWithAccount() {
        final Long deletedId = userId;
        purchases.recover(userId, request("before-delete"));
        users.deleteAccount(userId);
        userId = null;
        assertThat(receipts.findByUserIdAndCreateId(deletedId, "before-delete")).isEmpty();
        assertThat(userRepository.findById(deletedId)).isEmpty();
        assertThat(purchaseStates.findByUserId(deletedId)).isEmpty();
    }

    @Test
    @DisplayName("새 구매 시각은 UTC로 저장하고 서울 04시 기간과 구분한다")
    void persistsUtcInstantSeparatelyFromSeoulPurchaseDate() {
        at(NOW.withNano(123456789));
        var result = purchases.recover(userId, request("utc"));
        Instant expected = Instant.parse("2026-10-18T19:00:00.123456Z");
        OffsetDateTime stored =
                jdbc.queryForObject(
                        "select completed_at from user_energy_recovery_receipt where user_id = ?",
                        OffsetDateTime.class,
                        userId);

        assertThat(result.completedAt()).isEqualTo(expected);
        assertThat(stored.getOffset()).isEqualTo(ZoneOffset.UTC);
        assertThat(stored.toInstant()).isEqualTo(expected);
        assertThat(receipts.findByUserIdAndCreateId(userId, "utc").orElseThrow().getCompletedAt())
                .isEqualTo(expected);
        assertPurchaseState(java.time.LocalDate.of(2026, 10, 19), 1);
    }

    @Test
    @DisplayName("새 기간 첫 구매의 저장 실패는 기존 기준 날짜·횟수와 결제·지급을 모두 복원한다")
    void rollsBackPeriodResetAndCountWithFailedReceipt() {
        purchases.recover(userId, request("old-day"));
        at(NOW.plusDays(1));
        doThrow(new DataIntegrityViolationException("test next-period failure"))
                .when(receipts)
                .saveAndFlush(any());

        assertThatThrownBy(() -> purchases.recover(userId, request("new-day-failed")))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertPurchaseState(NOW.toLocalDate(), 1);
        assertState(INITIAL_GEMS - PRICE, 2 + AMOUNT, 1);
        assertThat(energies.findByUserId(userId).orElseThrow().getEnergyUpdatedAt())
                .isEqualTo(NOW.minusSeconds(30));
    }

    @Test
    @DisplayName("기존 영수증만 있는 계정은 상태를 복원해 이미 쓴 한도를 다시 지급하지 않는다")
    void initializesMissingCounterFromExistingReceiptsWithoutDeletingThem() {
        final var first = purchases.recover(userId, request("legacy-0"));
        for (int i = 1; i < LIMIT - 1; i++) {
            purchases.recover(userId, request("legacy-" + i));
        }
        transaction.executeWithoutResult(status -> purchaseStates.deleteByUserId(userId));
        assertThat(purchaseStates.findByUserId(userId)).isEmpty();

        var last = purchases.recover(userId, request("new-counter"));
        assertThat(last.purchaseCount()).isEqualTo(LIMIT);
        assertPurchaseState(NOW.toLocalDate(), LIMIT);
        assertThat(purchases.recover(userId, request("legacy-0"))).isEqualTo(first);
        assertThatThrownBy(() -> purchases.recover(userId, request("over-restored-limit")))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("일일 에너지 구매 한도");
        assertState(INITIAL_GEMS - LIMIT * PRICE, 2 + LIMIT * AMOUNT, LIMIT);
    }

    private List<Object> concurrently(String first, String second) throws Exception {
        var ready = new CountDownLatch(2);
        var start = new CountDownLatch(1);
        try (var pool = Executors.newFixedThreadPool(2)) {
            List<Callable<Object>> tasks =
                    List.of(
                            () -> attempt(first, ready, start),
                            () -> attempt(second, ready, start));
            var futures = tasks.stream().map(pool::submit).toList();
            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            return List.of(
                    futures.get(0).get(10, TimeUnit.SECONDS),
                    futures.get(1).get(10, TimeUnit.SECONDS));
        }
    }

    private Object attempt(String key, CountDownLatch ready, CountDownLatch start)
            throws Exception {
        ready.countDown();
        if (!start.await(5, TimeUnit.SECONDS)) {
            throw new IllegalStateException("테스트 동시 요청 시작 시간 초과");
        }
        try {
            return purchases.recover(userId, request(key));
        } catch (ResponseStatusException error) {
            return error.getStatusCode().value();
        }
    }

    private EnergyRecoveryRequest request(String key) {
        return new EnergyRecoveryRequest(key, EnergyRecoverySource.PURCHASE);
    }

    private void at(LocalDateTime now) {
        when(clock.instant()).thenReturn(now.atZone(SEOUL).toInstant());
    }

    private String authorization() {
        return "Bearer " + jwtProvider.createAccessToken(userId, ClientType.GAME, Role.USER);
    }

    private void setGem(int amount) {
        transaction.executeWithoutResult(
                status ->
                        ReflectionTestUtils.setField(
                                wallets.findByUserId(userId).orElseThrow(), "gem", amount));
    }

    private void setEnergy(int amount, LocalDateTime updatedAt) {
        transaction.executeWithoutResult(
                status -> {
                    UserEnergy energy = energies.findByUserId(userId).orElseThrow();
                    ReflectionTestUtils.setField(energy, "energyStored", amount);
                    ReflectionTestUtils.setField(energy, "energyUpdatedAt", updatedAt);
                });
    }

    private void assertPurchaseState(java.time.LocalDate day, int count) {
        var state = purchaseStates.findByUserId(userId).orElseThrow();
        assertThat(state.getPurchaseDay()).isEqualTo(day);
        assertThat(state.getPurchaseCount()).isEqualTo(count);
    }

    private void assertState(int gems, int energy, int count) {
        assertThat(wallets.findByUserId(userId).orElseThrow().getGem()).isEqualTo(gems);
        assertThat(wallets.findByUserId(userId).orElseThrow().getGold()).isEqualTo(321);
        assertThat(energies.findByUserId(userId).orElseThrow().getEnergyStored()).isEqualTo(energy);
        assertThat(
                        receipts.findAll().stream()
                                .filter(r -> r.getUser().getId().equals(userId))
                                .count())
                .isEqualTo(count);
    }
}
