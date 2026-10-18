package com.novaserver.condex.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.novaserver.auth.jwt.JwtProvider;
import com.novaserver.auth.repository.RefreshTokenRepository;
import com.novaserver.auth.repository.TokenBlocklistRepository;
import com.novaserver.auth.service.AuthService;
import com.novaserver.condex.entity.Codex;
import com.novaserver.condex.entity.MonsterGrade;
import com.novaserver.condex.repository.CodexRepository;
import com.novaserver.item.entity.Item;
import com.novaserver.reward.dto.RewardDefinitionRequest;
import com.novaserver.reward.entity.RewardDefinition;
import com.novaserver.reward.repository.RewardDefinitionRepository;
import com.novaserver.reward.service.RewardDefinitionService;
import com.novaserver.user.entity.UserCodex;
import com.novaserver.user.repository.UserCodexRepository;
import com.novaserver.user.repository.UserRepository;
import com.novaserver.user.repository.WalletRepository;
import com.novaserver.user.service.UserService;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

@SpringBootTest
@AutoConfigureMockMvc
class CodexAccountTransactionTest {
    private static final Instant FIRST = Instant.parse("2026-10-19T01:02:03.123456Z");
    @Autowired private UserCodexService service;
    @Autowired private UserService users;
    @Autowired private UserRepository accounts;
    @Autowired private CodexRepository monsters;
    @Autowired private WalletRepository wallets;
    @Autowired private RewardDefinitionService rewards;
    @Autowired private RewardDefinitionRepository definitions;
    @Autowired private PlatformTransactionManager transactions;
    @Autowired private JwtProvider tokens;
    @Autowired private AuthService authentication;
    @MockitoBean private RefreshTokenRepository refreshTokens;
    @Autowired private MockMvc mvc;
    @MockitoSpyBean private UserCodexRepository states;
    @MockitoBean private CodexRewardSource source;
    @MockitoBean private TokenBlocklistRepository blocklist;
    @MockitoBean private Clock clock;
    private TransactionTemplate tx;
    private final List<Long> userIds = new ArrayList<>();
    private final Map<Long, String> guestIds = new HashMap<>();
    private Long user;
    private Long monster;
    private Long second;
    private Long reward;

    @BeforeEach
    void setUp() {
        tx = new TransactionTemplate(transactions);
        when(clock.instant()).thenReturn(FIRST);
        monster =
                tx.execute(
                        s -> monsters.saveAndFlush(new Codex(50001L, MonsterGrade.NORMAL)).getId());
        second =
                tx.execute(
                        s -> monsters.saveAndFlush(new Codex(50002L, MonsterGrade.NORMAL)).getId());
        reward =
                rewards.create(new RewardDefinitionRequest("codex fixture", 500, Map.of(), null))
                        .id();
        when(source.findForMonster(any())).thenAnswer(i -> definitions.findById(reward));
        user = account();
    }

    private Long account() {
        var signup = authentication.guestSignUp();
        Long id = tokens.parse(signup.getTokens().getAccessToken()).userId();
        guestIds.put(id, signup.getGuestId());
        userIds.add(id);
        return id;
    }

    @AfterEach
    void cleanUp() {
        reset(states);
        for (Long id : userIds) {
            if (accounts.existsById(id)) {
                users.deleteAccount(id);
            }
        }
        monsters.deleteAllById(List.of(monster, second));
        definitions.deleteById(reward);
    }

    @Test
    void duplicateDiscoveryKeepsOriginalTimeAndClaimWithoutPaying() {
        var first = service.recordVerifiedEncounter(user, monster);
        when(clock.instant()).thenReturn(FIRST.plusSeconds(100));
        assertThat(service.recordVerifiedEncounter(user, monster)).isEqualTo(first);
        assertThat(first.claimed()).isFalse();
        assertThat(first.receipt()).isNull();
        assertThat(service.list(user)).containsExactly(first);
        assertThat(gold(user)).isZero();
    }

    @Test
    void concurrentFirstDiscoveryStoresOneRow() throws Exception {
        var results =
                race(
                        () -> service.recordVerifiedEncounter(user, monster),
                        () -> service.recordVerifiedEncounter(user, monster));
        assertThat(results.getFirst()).isEqualTo(results.getLast());
        assertThat(states.findByUserId(user)).hasSize(1);
        assertThat(gold(user)).isZero();
    }

    @ParameterizedTest
    @ValueSource(ints = {500, 750, 1000})
    void sourceGoldIsGrantedOnceAndReceiptSurvivesDefinitionChanges(int amount) {
        rewards.update(reward, new RewardDefinitionRequest("grade fixture", amount, Map.of(), 1));
        service.recordVerifiedEncounter(user, monster);
        var first = service.claim(user, monster);
        assertThat(first.gold()).isEqualTo(amount);
        assertThat(first.claimedAt()).isEqualTo(FIRST);
        rewards.delete(reward);
        // 과거 영수증은 RewardDefinition FK나 현재 등급 공급 없이 복원한다.
        when(source.findForMonster(any())).thenThrow(new IllegalStateException("must not resolve"));
        when(clock.instant()).thenReturn(FIRST.plusSeconds(100));
        assertThat(service.claim(user, monster)).isEqualTo(first);
        assertThat(service.recordVerifiedEncounter(user, monster).receipt()).isEqualTo(first);
        assertThat(gold(user)).isEqualTo(amount);
        reward =
                rewards.create(new RewardDefinitionRequest("cleanup fixture", 1, Map.of(), null))
                        .id();
    }

    @Test
    void concurrentSameMonsterClaimsReturnOneOriginalReceiptAndPayout() throws Exception {
        service.recordVerifiedEncounter(user, monster);
        var result = race(() -> service.claim(user, monster), () -> service.claim(user, monster));
        assertThat(result.getFirst()).isEqualTo(result.getLast());
        assertThat(gold(user)).isEqualTo(500);
        assertThat(states.findByUserId(user)).hasSize(1);
    }

    @Test
    void concurrentDifferentMonstersPayBothWithoutLosingWalletUpdate() throws Exception {
        service.recordVerifiedEncounter(user, monster);
        service.recordVerifiedEncounter(user, second);
        race(() -> service.claim(user, monster), () -> service.claim(user, second));
        assertThat(gold(user)).isEqualTo(1000);
        assertThat(service.list(user)).allMatch(s -> s.claimed());
    }

    @Test
    void rediscoveryRacingClaimKeepsOriginalDiscoveryAndReceipt() throws Exception {
        service.recordVerifiedEncounter(user, monster);
        race(
                () -> {
                    service.claim(user, monster);
                    return true;
                },
                () -> {
                    service.recordVerifiedEncounter(user, monster);
                    return true;
                });
        assertThat(service.list(user).getFirst().discoveredAt()).isEqualTo(FIRST);
        assertThat(service.list(user).getFirst().claimed()).isTrue();
        assertThat(gold(user)).isEqualTo(500);
    }

    @Test
    void separateAccountsClaimSameMonsterIndependently() throws Exception {
        Long other = account();
        service.recordVerifiedEncounter(user, monster);
        assertThat(service.list(other)).isEmpty();
        assertThatThrownBy(() -> service.claim(other, monster)).hasMessageContaining("발견하지 않은");
        service.recordVerifiedEncounter(other, monster);
        race(() -> service.claim(user, monster), () -> service.claim(other, monster));
        assertThat(gold(user)).isEqualTo(500);
        assertThat(gold(other)).isEqualTo(500);
    }

    @Test
    void rejectsUnknownAndUndiscoveredMonster() {
        assertThatThrownBy(() -> service.claim(user, Long.MAX_VALUE))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("404");
        assertThatThrownBy(() -> service.recordVerifiedEncounter(user, Long.MAX_VALUE))
                .hasMessageContaining("404");
        assertThatThrownBy(() -> service.claim(user, monster)).hasMessageContaining("409");
        assertThat(states.findByUserId(user)).isEmpty();
        assertThat(gold(user)).isZero();
    }

    @Test
    void sourceNotConfiguredBlocksPayoutAndLeavesUnclaimed() {
        service.recordVerifiedEncounter(user, monster);
        when(source.findForMonster(monster)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.claim(user, monster)).hasMessageContaining("503");
        assertUnclaimed();
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1})
    void invalidGoldDefinitionBlocksPayout(int amount) {
        service.recordVerifiedEncounter(user, monster);
        when(source.findForMonster(monster))
                .thenReturn(Optional.of(new RewardDefinition("invalid", amount, Map.of())));
        assertThatThrownBy(() -> service.claim(user, monster)).hasMessageContaining("503");
        assertUnclaimed();
    }

    @Test
    void itemBundleCannotBeUsedAsGoldOnlyCodexReward() {
        service.recordVerifiedEncounter(user, monster);
        when(source.findForMonster(monster))
                .thenReturn(
                        Optional.of(
                                new RewardDefinition(
                                        "invalid", 500, Map.of(new Item("unexpected", null), 1))));
        assertThatThrownBy(() -> service.claim(user, monster)).hasMessageContaining("503");
        assertUnclaimed();
    }

    @Test
    void goldOverflowRollsBackClaimAndRetryAfterSpendPaysOnce() {
        service.recordVerifiedEncounter(user, monster);
        tx.executeWithoutResult(
                s ->
                        wallets.findByUserIdForUpdate(user)
                                .orElseThrow()
                                .addGold(Integer.MAX_VALUE - 499));
        assertThatThrownBy(() -> service.claim(user, monster)).hasMessageContaining("저장 범위");
        assertThat(service.list(user).getFirst().claimed()).isFalse();
        assertThat(gold(user)).isEqualTo(Integer.MAX_VALUE - 499);
        tx.executeWithoutResult(s -> wallets.findByUserIdForUpdate(user).orElseThrow().useGold(1));
        service.claim(user, monster);
        assertThat(gold(user)).isEqualTo(Integer.MAX_VALUE);
    }

    @Test
    void receiptFlushFailureRollsBackWalletAndAllowsRetry() {
        service.recordVerifiedEncounter(user, monster);
        doThrow(new IllegalStateException("receipt failure"))
                .when(states)
                .saveAndFlush(any(UserCodex.class));
        assertThatThrownBy(() -> service.claim(user, monster))
                .hasMessageContaining("receipt failure");
        assertUnclaimed();
        reset(states);
        service.claim(user, monster);
        assertThat(gold(user)).isEqualTo(500);
    }

    @Test
    void missingWalletCannotMarkClaimed() {
        service.recordVerifiedEncounter(user, monster);
        tx.executeWithoutResult(s -> wallets.deleteByUserId(user));
        assertThatThrownBy(() -> service.claim(user, monster)).hasMessageContaining("지갑");
        assertThat(service.list(user).getFirst().claimed()).isFalse();
    }

    @Test
    void outerVerifiedEventRollbackAlsoRollsBackDiscovery() {
        assertThatThrownBy(
                        () ->
                                tx.executeWithoutResult(
                                        s -> {
                                            service.recordVerifiedEncounter(user, monster);
                                            throw new IllegalStateException("event rollback");
                                        }))
                .hasMessageContaining("event rollback");
        assertThat(service.list(user)).isEmpty();
    }

    @Test
    void discoveryFlushFailureCreatesNoState() {
        doThrow(new IllegalStateException("discovery failure"))
                .when(states)
                .saveAndFlush(any(UserCodex.class));
        assertThatThrownBy(() -> service.recordVerifiedEncounter(user, monster))
                .hasMessageContaining("discovery failure");
        assertThat(service.list(user)).isEmpty();
    }

    @Test
    void freshJwtRestoresAccountAndOtherAccountCannotReadOrClaimItsState() throws Exception {
        service.recordVerifiedEncounter(user, monster);
        String firstToken = auth(user);
        mvc.perform(
                        post("/api/user/codex/claim")
                                .header("Authorization", firstToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"monsterId\":" + monster + "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.gold").value(500));
        String reissuedToken = auth(user);
        mvc.perform(get("/api/user/me").header("Authorization", reissuedToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.gold").value(500))
                .andExpect(jsonPath("$.codex[0].monsterId").value(monster))
                .andExpect(jsonPath("$.codex[0].claimed").value(true));
        mvc.perform(get("/api/user/codex").header("Authorization", reissuedToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].receipt.gold").value(500));
        Long other = account();
        mvc.perform(get("/api/user/codex").header("Authorization", auth(other)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
        mvc.perform(
                        post("/api/user/codex/claim")
                                .header("Authorization", auth(other))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"monsterId\":" + monster + "}"))
                .andExpect(status().isConflict());
        assertThat(gold(other)).isZero();
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                "{}",
                "{\"monsterId\":null}",
                "{\"monsterId\":0}",
                "{\"monsterId\":-1}",
                "{\"killCount\":999,\"encounteredMonsterIds\":[1]}"
            })
    void malformedOrClientKillReportDoesNotGrant(String body) throws Exception {
        mvc.perform(
                        post("/api/user/codex/claim")
                                .header("Authorization", auth(user))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body))
                .andExpect(status().isBadRequest());
        assertThat(gold(user)).isZero();
        assertThat(service.list(user)).isEmpty();
    }

    @Test
    void anonymousCallsAndDiscoveryHttpAreUnavailable() throws Exception {
        mvc.perform(get("/api/user/codex")).andExpect(status().isUnauthorized());
        mvc.perform(
                        post("/api/user/codex/claim")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"monsterId\":1}"))
                .andExpect(status().isUnauthorized());
        mvc.perform(
                        post("/api/user/codex/discover")
                                .header("Authorization", auth(user))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"monsterId\":" + monster + "}"))
                .andExpect(status().isNotFound());
        assertThat(service.list(user)).isEmpty();
    }

    @Test
    void deletingAccountRemovesDiscoveryAndReceiptAndOldTokenCannotClaim() throws Exception {
        service.recordVerifiedEncounter(user, monster);
        service.claim(user, monster);
        final String old = auth(user);
        users.deleteAccount(user);
        assertThat(states.findByUserId(user)).isEmpty();
        assertThatThrownBy(() -> service.recordVerifiedEncounter(user, monster))
                .hasMessageContaining("401");
        mvc.perform(
                        post("/api/user/codex/claim")
                                .header("Authorization", old)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"monsterId\":" + monster + "}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void guestLoginHttpReissuesTokenForRestoringSameDatabaseState() throws Exception {
        service.recordVerifiedEncounter(user, monster);
        service.claim(user, monster);
        mvc.perform(
                        post("/api/auth/guest/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"guestId\":\"" + guestIds.get(user) + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isString());
        mvc.perform(get("/api/user/me").header("Authorization", auth(user)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.codex[0].claimed").value(true))
                .andExpect(jsonPath("$.gold").value(500));
    }

    @Test
    void clientIdentityAndGoldFieldsCannotClaimAnotherAccountsDiscovery() throws Exception {
        Long other = account();
        service.recordVerifiedEncounter(other, monster);
        mvc.perform(
                        post("/api/user/codex/claim")
                                .header("Authorization", auth(user))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        "{\"monsterId\":"
                                                + monster
                                                + ",\"userId\":"
                                                + other
                                                + ",\"gold\":999999,\"killCount\":999}"))
                .andExpect(status().isConflict());
        assertThat(gold(user)).isZero();
        assertThat(gold(other)).isZero();
    }

    @Test
    void databaseUniqueConstraintPreventsDuplicateAccountMonsterRows() {
        service.recordVerifiedEncounter(user, monster);
        assertThatThrownBy(
                        () ->
                                tx.executeWithoutResult(
                                        s ->
                                                states.saveAndFlush(
                                                        new UserCodex(
                                                                accounts.getReferenceById(user),
                                                                monsters.getReferenceById(monster),
                                                                FIRST))))
                .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
        assertThat(states.findByUserId(user)).hasSize(1);
    }

    private void assertUnclaimed() {
        var state = service.list(user).getFirst();
        assertThat(state.claimed()).isFalse();
        assertThat(state.receipt()).isNull();
        assertThat(gold(user)).isZero();
    }

    private int gold(Long id) {
        return wallets.findByUserId(id).orElseThrow().getGold();
    }

    private String auth(Long id) {
        return "Bearer " + authentication.guestLogIn(guestIds.get(id)).getAccessToken();
    }

    private <T> List<T> race(Callable<T> a, Callable<T> b) throws Exception {
        var ready = new CountDownLatch(2);
        var go = new CountDownLatch(1);
        try (var pool = Executors.newFixedThreadPool(2)) {
            var tasks =
                    List.of(a, b).stream()
                            .map(
                                    task ->
                                            pool.submit(
                                                    () -> {
                                                        ready.countDown();
                                                        if (!go.await(5, TimeUnit.SECONDS)) {
                                                            throw new IllegalStateException(
                                                                    "start timeout");
                                                        }
                                                        return task.call();
                                                    }))
                            .toList();
            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            go.countDown();
            return List.of(
                    tasks.get(0).get(10, TimeUnit.SECONDS), tasks.get(1).get(10, TimeUnit.SECONDS));
        }
    }
}
