package com.novaserver.battle.service;

import static com.novaserver.support.RewardRulesFixture.CHEST;
import static com.novaserver.support.RewardRulesFixture.EQUIPMENT;
import static com.novaserver.support.RewardRulesFixture.RANDOM_EQUIPMENT;
import static com.novaserver.support.RewardRulesFixture.RANDOM_SKILL;
import static com.novaserver.support.RewardRulesFixture.SKILL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.novaserver.auth.jwt.ClientType;
import com.novaserver.auth.jwt.JwtProvider;
import com.novaserver.auth.repository.TokenBlocklistRepository;
import com.novaserver.battle.dto.BattleEndRequest;
import com.novaserver.battle.repository.BattleRepository;
import com.novaserver.battle.repository.BattleResultRepository;
import com.novaserver.item.dto.ItemCreateRequest;
import com.novaserver.item.dto.ItemRequest;
import com.novaserver.item.entity.Item;
import com.novaserver.item.repository.ItemRepository;
import com.novaserver.item.service.ItemService;
import com.novaserver.stage.dto.ClearRewardResult;
import com.novaserver.stage.dto.RewardItemRule;
import com.novaserver.stage.dto.RewardRulesData;
import com.novaserver.stage.entity.Stage;
import com.novaserver.stage.entity.StageReward;
import com.novaserver.stage.entity.StageRewardItem;
import com.novaserver.stage.repository.RewardRulesConfigRepository;
import com.novaserver.stage.repository.StageRepository;
import com.novaserver.stage.repository.StageRewardRepository;
import com.novaserver.stage.service.RewardRulesService;
import com.novaserver.stage.service.StageRewardSchedule;
import com.novaserver.stage.service.StageRewardService;
import com.novaserver.support.RewardRulesFixture;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
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
    @Autowired private StageRewardRepository stageRewards;
    @Autowired private RewardRulesService rulesService;
    @Autowired private RewardRulesConfigRepository ruleConfigs;
    @Autowired private ItemRepository items;
    @Autowired private ItemService itemService;
    @Autowired private PlatformTransactionManager transactions;
    @Autowired private JwtProvider jwtProvider;
    @Autowired private MockMvc mvc;
    @MockitoSpyBean private BattleResultRepository results;
    @MockitoSpyBean private StageRewardService rewards;
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
        doReturn(new ClearRewardResult(13, 17, Map.of(ITEM, 9), 0))
                .when(rewards)
                .calculateClearReward(eq(901), anyInt(), anyInt());
    }

    @AfterEach
    void cleanUp() {
        if (userId != null) {
            users.deleteAccount(userId);
        }
        transaction.executeWithoutResult(
                status -> {
                    ruleConfigs.deleteAll();
                    stageRewards.deleteById(901);
                    items.deleteById(ITEM);
                    items.deleteAllById(List.of(SKILL, EQUIPMENT, CHEST));
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
        doReturn(new ClearRewardResult(999, 999, Map.of(), 0))
                .when(rewards)
                .calculateClearReward(eq(901), anyInt(), anyInt());
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
                                .content(new ObjectMapper().writeValueAsString(request(id, 11, 2))))
                .andExpect(status().isConflict());
        assertBalances(13, 17, 9);
    }

    @ParameterizedTest
    @CsvSource({"-1,0", "17,-1", "17,14", "17,1"})
    void invalidCalculatedRewardCannotLeaveReceiptForAnAmountThatWasNotGranted(
            int gold, int bonusExp) {
        String id = battles.createBattle(userId, "invalid-reward").getBattleId();
        doReturn(new ClearRewardResult(13, gold, Map.of(ITEM, 9), bonusExp))
                .when(rewards)
                .calculateClearReward(eq(901), anyInt(), anyInt());
        assertThatThrownBy(() -> battles.endBattle(userId, request(id, 12, 2)))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("전투 보상 기준 데이터");
        assertBalances(0, 0, 0);
        assertThat(results.findAllByUserId(userId)).isEmpty();
        assertThat(sessions.findById(userId)).isPresent();
    }

    @ParameterizedTest
    @CsvSource({"0,0,0,3,2,1,2,0", "20,3,30,67,83,26,49,17"})
    void realCalculationIsGrantedAndReceiptedOnce(
            int wave,
            int rating,
            int exp,
            int gold,
            int skill,
            int equipment,
            int chest,
            int bonusExp)
            throws Exception {
        transaction.executeWithoutResult(
                status -> {
                    items.saveAll(RewardRulesFixture.catalog());
                    stageRewards.save(
                            new StageReward(
                                    stages.findById(901).orElseThrow(),
                                    13,
                                    17,
                                    67,
                                    List.of(
                                            new StageRewardItem(SKILL, 83),
                                            new StageRewardItem(EQUIPMENT, 26),
                                            new StageRewardItem(CHEST, 49))));
                });
        var ruleData = RewardRulesFixture.rules();
        rulesService.saveConfig(ruleData);
        doCallRealMethod().when(rewards).calculateClearReward(eq(901), anyInt(), anyInt());
        String id = battles.createBattle(userId, "real-calculation").getBattleId();
        String authorization =
                "Bearer " + jwtProvider.createAccessToken(userId, ClientType.GAME, Role.USER);
        String prefix = "{\"battleId\":\"" + id + "\",\"stageId\":901,";
        mvc.perform(
                        post("/api/battle/end")
                                .header("Authorization", authorization)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(prefix + "\"clearedWave\":19,\"rating\":3}"))
                .andExpect(status().isBadRequest());
        assertThat(sessions.findById(userId)).isPresent();
        assertBalances(0, 0, 0);
        String payload = prefix + "\"clearedWave\":" + wave + ",\"rating\":" + rating + "}";
        String first =
                mvc.perform(
                                post("/api/battle/end")
                                        .header("Authorization", authorization)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(payload))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.exp").value(exp))
                        .andExpect(jsonPath("$.threeStarBonusExp").value(bonusExp))
                        .andExpect(jsonPath("$.gold").value(gold))
                        .andReturn()
                        .getResponse()
                        .getContentAsString();
        mvc.perform(
                        post("/api/battle/end")
                                .header("Authorization", authorization)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(payload))
                .andExpect(status().isOk())
                .andExpect(content().json(first));
        assertBalances(exp, gold, 0);
        assertThat(inventory.findByUserIdAndItemId(userId, SKILL).orElseThrow().getQuantity())
                .isEqualTo(skill);
        assertThat(
                        inventory
                                .findByUserIdAndItemId(userId, EQUIPMENT)
                                .map(item -> item.getQuantity())
                                .orElse(0))
                .isEqualTo(equipment);
        assertThat(
                        inventory
                                .findByUserIdAndItemId(userId, CHEST)
                                .map(item -> item.getQuantity())
                                .orElse(0))
                .isEqualTo(chest);
        assertThat(results.findAllByUserId(userId)).hasSize(1);
        var nextSchedules = new HashMap<>(ruleData.schedules());
        nextSchedules.put("Coin", new StageRewardSchedule(9, 2, 3));
        nextSchedules.put("SkillMaterial", new StageRewardSchedule(2, 1, 2));
        var updated = rulesService.saveConfig(new RewardRulesData(nextSchedules, ruleData.items()));
        transaction.executeWithoutResult(
                status -> {
                    var changed = stageRewards.findById(901).orElseThrow();
                    changed.update(19, 31, changed.getFinalCoin(), changed.getItems());
                });
        mvc.perform(get("/api/reward-rules").header("Authorization", authorization))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.schedules.Coin.start").value(9))
                .andExpect(jsonPath("$.items['" + EQUIPMENT + "'].kind").value("EquipmentMaterial"))
                .andExpect(jsonPath("$.version").value(updated.version()));
        mvc.perform(
                        post("/api/battle/end")
                                .header("Authorization", authorization)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(payload))
                .andExpect(status().isOk())
                .andExpect(content().json(first));
        String nextId = battles.createBattle(userId, "updated-rules").getBattleId();
        var nextResult = battles.endBattle(userId, request(nextId, 0, 0));
        assertThat(nextResult.gold()).isEqualTo(9);
        assertThat(nextResult.items()).containsEntry(SKILL, 2);
        assertBalances(exp, gold + 9, 0);
        assertThat(inventory.findByUserIdAndItemId(userId, SKILL).orElseThrow().getQuantity())
                .isEqualTo(skill + 2);
    }

    @Test
    void missingOrInvalidRulesCannotFallbackOrChangeVersion() throws Exception {
        String authorization =
                "Bearer " + jwtProvider.createAccessToken(userId, ClientType.GAME, Role.USER);
        mvc.perform(get("/api/reward-rules").header("Authorization", authorization))
                .andExpect(status().isServiceUnavailable());
        transaction.executeWithoutResult(status -> items.saveAll(RewardRulesFixture.catalog()));
        var ruleData = RewardRulesFixture.rules();
        var mapper = new ObjectMapper();
        String body = mapper.writeValueAsString(ruleData);
        mvc.perform(
                        put("/api/admin/reward-rules")
                                .header("Authorization", authorization)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body))
                .andExpect(status().isForbidden());
        String admin =
                "Bearer " + jwtProvider.createAccessToken(userId, ClientType.WEB, Role.ADMIN);
        mvc.perform(
                        put("/api/admin/reward-rules")
                                .header("Authorization", admin)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body))
                .andExpect(status().isOk());
        var before = rulesService.getConfig();
        var invalidSchedules = new HashMap<>(ruleData.schedules());
        invalidSchedules.put("Coin", new StageRewardSchedule(3, -1, 7));
        var invalidRules = new RewardRulesData(invalidSchedules, ruleData.items());
        mvc.perform(
                        put("/api/admin/reward-rules")
                                .header("Authorization", admin)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(mapper.writeValueAsString(invalidRules)))
                .andExpect(status().isBadRequest());
        assertThat(rulesService.getConfig()).isEqualTo(before);
        // 랜덤 선택자는 카탈로그에 만들지 않는다. 실제 아이템 수정도 지급 규칙을 바꾸지 않는다.
        assertThat(items.existsById(RANDOM_SKILL)).isFalse();
        assertThat(items.existsById(RANDOM_EQUIPMENT)).isFalse();
        var colliding = new HashMap<>(ruleData.items());
        colliding.put(SKILL, new RewardItemRule("SkillMaterial", true));
        mvc.perform(
                        put("/api/admin/reward-rules")
                                .header("Authorization", admin)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        mapper.writeValueAsString(
                                                new RewardRulesData(
                                                        ruleData.schedules(), colliding))))
                .andExpect(status().isBadRequest());
        var createSelector = new ItemCreateRequest();
        ReflectionTestUtils.setField(createSelector, "itemId", RANDOM_SKILL);
        assertThatThrownBy(() -> itemService.createItem(createSelector))
                .isInstanceOf(ResponseStatusException.class);
        assertThat(items.existsById(RANDOM_SKILL)).isFalse();
        assertThat(rulesService.getConfig()).isEqualTo(before);
        itemService.updateItem(SKILL, new ItemRequest());
        assertThat(rulesService.getConfig()).isEqualTo(before);
        assertThatThrownBy(() -> itemService.deleteItem(SKILL))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("보상 기준에 연결된 아이템");
        var unlinked = new HashMap<>(ruleData.items());
        unlinked.remove(SKILL);
        rulesService.saveConfig(new RewardRulesData(ruleData.schedules(), unlinked));
        itemService.deleteItem(SKILL);
        assertThat(items.existsById(SKILL)).isFalse();
        assertThat(rulesService.getConfig().items()).doesNotContainKey(SKILL);
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
