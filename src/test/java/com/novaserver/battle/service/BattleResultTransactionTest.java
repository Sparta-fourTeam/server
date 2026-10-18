package com.novaserver.battle.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.novaserver.auth.jwt.ClientType;
import com.novaserver.auth.jwt.JwtProvider;
import com.novaserver.auth.repository.TokenBlocklistRepository;
import com.novaserver.battle.dto.BattleEndRequest;
import com.novaserver.battle.repository.BattleRepository;
import com.novaserver.battle.repository.BattleResultRepository;
import com.novaserver.item.entity.Item;
import com.novaserver.item.repository.ItemRepository;
import com.novaserver.stage.dto.ClearRewardResult;
import com.novaserver.stage.entity.Stage;
import com.novaserver.stage.repository.StageRepository;
import com.novaserver.stage.service.StageRewardService;
import com.novaserver.user.Provider;
import com.novaserver.user.Role;
import com.novaserver.user.entity.User;
import com.novaserver.user.repository.UserDataRepository;
import com.novaserver.user.repository.UserItemRepository;
import com.novaserver.user.repository.UserRepository;
import com.novaserver.user.repository.UserStageProgressRepository;
import com.novaserver.user.repository.WalletRepository;
import com.novaserver.user.service.UserService;
import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

@SpringBootTest
@AutoConfigureMockMvc
class BattleResultTransactionTest {
    private static final Instant NOW = Instant.parse("2026-10-18T20:21:22.123456789Z");
    private static final String ITEM = "battle.test.material";
    @Autowired private BattleService battles;
    @Autowired private UserService users;
    @Autowired private UserRepository userRepository;
    @Autowired private UserDataRepository data;
    @Autowired private WalletRepository wallets;
    @Autowired private UserItemRepository inventory;
    @Autowired private UserStageProgressRepository progress;
    @Autowired private BattleRepository sessions;
    @Autowired private StageRepository stages;
    @Autowired private ItemRepository items;
    @Autowired private PlatformTransactionManager transactions;
    @Autowired private JwtProvider jwtProvider;
    @Autowired private MockMvc mvc;
    @MockitoSpyBean private BattleResultRepository results;
    @MockitoBean private StageRewardService rewards;
    @MockitoBean private Clock clock;
    @MockitoBean private TokenBlocklistRepository blocklist;
    private Long userId;
    private TransactionTemplate transaction;

    @BeforeEach
    void setUp() {
        transaction = new TransactionTemplate(transactions);
        when(clock.instant()).thenReturn(NOW);
        transaction.executeWithoutResult(
                status -> {
                    stages.save(Stage.create(901));
                    items.save(new Item(ITEM, "test.skill"));
                    User user =
                            userRepository.saveAndFlush(
                                    new User(Provider.GUEST, UUID.randomUUID().toString()));
                    userId = user.getId();
                    users.createInitialState(user);
                });
        when(rewards.calculateClearReward(eq(901), anyInt(), anyInt()))
                .thenReturn(new ClearRewardResult(13, 17, Map.of(ITEM, 9)));
    }

    @AfterEach
    void cleanUp() {
        if (userId != null) {
            users.deleteAccount(userId);
        }
        transaction.executeWithoutResult(
                status -> {
                    items.deleteById(ITEM);
                    stages.deleteById(901);
                });
    }

    @Test
    void lostResponseReplaysPersistentResultAfterNewBattleAndUnrelatedBalanceChange() {
        String id = battles.createBattle(userId, "first").getBattleId();
        BattleEndRequest request = request(id, 12, 2);
        var first = battles.endBattle(userId, request);
        assertThat(first.completedAt()).isEqualTo(Instant.parse("2026-10-18T20:21:22.123456Z"));
        final String next = battles.createBattle(userId, "next").getBattleId();
        transaction.executeWithoutResult(
                status -> wallets.findByUserIdForUpdate(userId).orElseThrow().addGold(31));
        // 새 트랜잭션에서 DB를 읽는다. 이후 계산 기준 변경도 과거 결과를 바꾸지 않는다.
        when(rewards.calculateClearReward(eq(901), anyInt(), anyInt()))
                .thenReturn(new ClearRewardResult(999, 999, Map.of()));
        assertThat(battles.endBattle(userId, request)).isEqualTo(first);
        assertThat(sessions.findById(userId).orElseThrow().getBattleId()).isEqualTo(next);
        assertBalances(13, 48, 9);
        assertThat(results.findAllByUserId(userId)).hasSize(1);
        assertThatThrownBy(() -> battles.endBattle(userId, request(id, 11, 2)))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("이미 확정한 전투 결과");
    }

    @Test
    void concurrentSameBattleSubmissionsReturnIdenticalReceiptAndGrantOnce() throws Exception {
        String id = battles.createBattle(userId, "race").getBattleId();
        var ready = new CountDownLatch(2);
        var go = new CountDownLatch(1);
        try (var pool = Executors.newFixedThreadPool(2)) {
            var attempts =
                    java.util.stream.IntStream.range(0, 2)
                            .mapToObj(
                                    i ->
                                            pool.submit(
                                                    () -> {
                                                        ready.countDown();
                                                        if (!go.await(5, TimeUnit.SECONDS)) {
                                                            throw new IllegalStateException(
                                                                    "동시 요청 시작 시간 초과");
                                                        }
                                                        return battles.endBattle(
                                                                userId, request(id, 12, 2));
                                                    }))
                            .toList();
            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            go.countDown();
            assertThat(attempts.get(0).get(10, TimeUnit.SECONDS))
                    .isEqualTo(attempts.get(1).get(10, TimeUnit.SECONDS));
        }
        assertBalances(13, 17, 9);
        assertThat(results.findAllByUserId(userId)).hasSize(1);
    }

    @Test
    void receiptFailureRollsBackSessionRewardItemsAndProgressTogether() {
        String id = battles.createBattle(userId, "rollback").getBattleId();
        doThrow(new DataIntegrityViolationException("test receipt failure"))
                .when(results)
                .saveAndFlush(any());
        assertThatThrownBy(() -> battles.endBattle(userId, request(id, 12, 2)))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThat(sessions.findById(userId))
                .get()
                .extracting(session -> session.getBattleId())
                .isEqualTo(id);
        assertBalances(0, 0, 0);
        assertThat(progress.findByUserIdAndStageId(userId, 901)).isEmpty();
        assertThat(results.findAllByUserId(userId)).isEmpty();
    }

    @Test
    void authenticatedHttpReturnsUtcOriginalReceiptAndIsolatesOtherAccounts() throws Exception {
        String id = battles.createBattle(userId, "http").getBattleId();
        String authorization =
                "Bearer " + jwtProvider.createAccessToken(userId, ClientType.GAME, Role.USER);
        String payload =
                "{\"battleId\":\"" + id + "\",\"stageId\":901,\"clearedWave\":12,\"rating\":2}";
        for (int i = 0; i < 2; i++) {
            mvc.perform(
                            post("/api/battle/end")
                                    .header("Authorization", authorization)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(payload))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.gold").value(17))
                    .andExpect(jsonPath("$.items['" + ITEM + "']").value(9))
                    .andExpect(jsonPath("$.completedAt").value("2026-10-18T20:21:22.123456Z"));
        }
        Long otherId =
                transaction.execute(
                        status -> {
                            User otherUser =
                                    userRepository.saveAndFlush(
                                            new User(Provider.GUEST, UUID.randomUUID().toString()));
                            users.createInitialState(otherUser);
                            return otherUser.getId();
                        });
        try {
            String other =
                    "Bearer " + jwtProvider.createAccessToken(otherId, ClientType.GAME, Role.USER);
            mvc.perform(
                            post("/api/battle/end")
                                    .header("Authorization", other)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(payload))
                    .andExpect(status().isNotFound());
            assertThat(wallets.findByUserId(otherId).orElseThrow().getGold()).isZero();
            assertThat(results.findAllByUserId(otherId)).isEmpty();
        } finally {
            users.deleteAccount(otherId);
        }
        mvc.perform(
                        post("/api/battle/end")
                                .header("Authorization", authorization)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        payload.replace(
                                                "\"clearedWave\":12", "\"clearedWave\":11")))
                .andExpect(status().isConflict());
        assertBalances(13, 17, 9);
    }

    @Test
    void invalidCalculatedRewardCannotLeaveReceiptForAnAmountThatWasNotGranted() {
        String id = battles.createBattle(userId, "invalid-reward").getBattleId();
        when(rewards.calculateClearReward(eq(901), anyInt(), anyInt()))
                .thenReturn(new ClearRewardResult(13, -1, Map.of(ITEM, 9)));
        assertThatThrownBy(() -> battles.endBattle(userId, request(id, 12, 2)))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("전투 보상 기준 데이터");
        assertBalances(0, 0, 0);
        assertThat(results.findAllByUserId(userId)).isEmpty();
        assertThat(sessions.findById(userId)).isPresent();
    }

    @Test
    void accountDeletionRemovesReceiptAndItemSnapshot() {
        String id = battles.createBattle(userId, "delete").getBattleId();
        battles.endBattle(userId, request(id, 12, 2));
        users.deleteAccount(userId);
        assertThat(results.findByUserIdAndBattleId(userId, id)).isEmpty();
        userId = null;
    }

    private BattleEndRequest request(String battleId, int wave, int rating) {
        var request = new BattleEndRequest();
        ReflectionTestUtils.setField(request, "battleId", battleId);
        ReflectionTestUtils.setField(request, "stageId", 901);
        ReflectionTestUtils.setField(request, "clearedWave", wave);
        ReflectionTestUtils.setField(request, "rating", rating);
        return request;
    }

    private void assertBalances(int exp, int gold, int amount) {
        assertThat(data.findByUserId(userId).orElseThrow().getExp()).isEqualTo(exp);
        assertThat(wallets.findByUserId(userId).orElseThrow().getGold()).isEqualTo(gold);
        assertThat(
                        inventory
                                .findByUserIdAndItemId(userId, ITEM)
                                .map(item -> item.getQuantity())
                                .orElse(0))
                .isEqualTo(amount);
    }
}
