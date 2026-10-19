package com.novaserver.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.novaserver.auth.jwt.ClientType;
import com.novaserver.auth.jwt.JwtProvider;
import com.novaserver.auth.repository.AccessTokenRepository;
import com.novaserver.auth.repository.TokenBlocklistRepository;
import com.novaserver.item.entity.Item;
import com.novaserver.item.repository.ItemRepository;
import com.novaserver.item.service.ItemService;
import com.novaserver.reward.dto.RewardDefinitionRequest;
import com.novaserver.reward.entity.RewardDefinition;
import com.novaserver.reward.repository.RewardDefinitionRepository;
import com.novaserver.reward.service.RewardDefinitionService;
import com.novaserver.stage.dto.StageRatingRewardRequest;
import com.novaserver.stage.entity.Stage;
import com.novaserver.stage.entity.StageRatingReward;
import com.novaserver.stage.repository.StageRatingRewardRepository;
import com.novaserver.stage.repository.StageRepository;
import com.novaserver.stage.service.StageRatingRewardService;
import com.novaserver.user.Provider;
import com.novaserver.user.Role;
import com.novaserver.user.entity.User;
import com.novaserver.user.entity.UserItem;
import com.novaserver.user.entity.UserStageProgress;
import com.novaserver.user.repository.StageRatingClaimRepository;
import com.novaserver.user.repository.UserItemRepository;
import com.novaserver.user.repository.UserRepository;
import com.novaserver.user.repository.UserStageProgressRepository;
import com.novaserver.user.repository.WalletRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
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
class StageRatingRewardTransactionTest {
    private static final int STAGE = 1901;
    private static final String A = "rating.fixture.a";
    private static final String B = "rating.fixture.b";
    private static final String C = "rating.fixture.race";
    @Autowired private UserStageProgressService service;
    @Autowired private UserService users;
    @Autowired private UserRepository accounts;
    @Autowired private StageRepository stages;
    @Autowired private StageRatingRewardRepository rewards;
    @Autowired private UserStageProgressRepository progress;
    @Autowired private WalletRepository wallets;
    @Autowired private UserItemRepository inventory;
    @Autowired private ItemRepository items;
    @Autowired private ItemService itemService;
    @Autowired private RewardDefinitionRepository definitions;
    @Autowired private RewardDefinitionService definitionService;
    @Autowired private StageRatingRewardService ratingService;
    @Autowired private PlatformTransactionManager transactions;
    @Autowired private MockMvc mvc;
    @Autowired private JwtProvider tokens;
    @Autowired private JdbcTemplate jdbc;
    @MockitoSpyBean private StageRatingClaimRepository receipts;
    @MockitoBean private Clock clock;
    @MockitoBean private TokenBlocklistRepository blocklist;
    @MockitoBean private AccessTokenRepository accessTokenRepository;
    private TransactionTemplate tx;
    private Long userId;
    private final List<Long> userIds = new ArrayList<>();

    @BeforeEach
    void setUp() {
        tx = new TransactionTemplate(transactions);
        when(clock.instant()).thenReturn(Instant.parse("2026-10-19T01:02:03.123456789Z"));
        tx.executeWithoutResult(
                status -> {
                    var stage = stages.save(Stage.create(STAGE));
                    items.saveAll(List.of(new Item(A, null), new Item(B, null)));
                    rewards.saveAll(
                            List.of(
                                    new StageRatingReward(
                                            stage, 1, fixedDefinition(13, Map.of(A, 2))),
                                    new StageRatingReward(
                                            stage, 2, fixedDefinition(17, Map.of(A, 3))),
                                    new StageRatingReward(
                                            stage, 3, fixedDefinition(19, Map.of(B, 7)))));
                });
        userId = newAccount();
    }

    private Long newAccount() {
        return tx.execute(
                status -> {
                    User user =
                            accounts.saveAndFlush(
                                    new User(Provider.GUEST, UUID.randomUUID().toString()));
                    userIds.add(user.getId());
                    users.createInitialState(user);
                    var row = new UserStageProgress(user, stages.findById(STAGE).orElseThrow());
                    row.updateClearRating(3);
                    progress.saveAndFlush(row);
                    return user.getId();
                });
    }

    @AfterEach
    void cleanUp() {
        for (Long id : userIds) {
            users.deleteAccount(id);
        }
        tx.executeWithoutResult(
                status -> {
                    rewards.deleteAll(rewards.findByStageStageIdOrderByRating(STAGE));
                    definitions.deleteAll(definitions.findAll());
                    definitions.flush();
                    items.deleteAllById(List.of(A, B, C));
                    stages.deleteById(STAGE);
                });
    }

    @ParameterizedTest
    @CsvSource({"0,49,5,7", "1,36,3,7", "2,19,0,7"})
    void grantsOnlyTheUnclaimedRangeAndPersistsTheExactItems(int previous, int gold, int a, int b) {
        tx.executeWithoutResult(
                status ->
                        progress.findByUserIdAndStageIdForUpdate(userId, STAGE)
                                .orElseThrow()
                                .updateClaimedRating(previous));
        var receipt = service.claimRatingReward(userId, STAGE, "range", 3);
        assertThat(receipt.previousClaimedRating()).isEqualTo(previous);
        assertThat(receipt.claimedRating()).isEqualTo(3);
        assertThat(receipt.gold()).isEqualTo(gold);
        assertThat(receipt.items().getOrDefault(A, 0)).isEqualTo(a);
        assertThat(receipt.items().getOrDefault(B, 0)).isEqualTo(b);
        assertState(gold, a, b, 3);
        assertThat(service.claimRatingReward(userId, STAGE, "range", 3)).isEqualTo(receipt);
        assertState(gold, a, b, 3);
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 3})
    void oldIntentDoesNotConsumeNewStarsAndOldReceiptSurvivesLaterClaimAndBalanceChanges(
            int initialRating) {
        tx.executeWithoutResult(
                status ->
                        ReflectionTestUtils.setField(
                                progress.findByUserIdAndStageIdForUpdate(userId, STAGE)
                                        .orElseThrow(),
                                "clearRating",
                                initialRating));
        var first = service.claimRatingReward(userId, STAGE, "old-intent", 1);
        assertThat(first.clearRating()).isEqualTo(initialRating);
        assertThat(first.claimedRating()).isEqualTo(1);
        assertState(13, 2, 0, 1);
        tx.executeWithoutResult(
                status ->
                        progress.findByUserIdAndStageIdForUpdate(userId, STAGE)
                                .orElseThrow()
                                .updateClearRating(3));
        assertThat(service.claimRatingReward(userId, STAGE, "old-intent", 1)).isEqualTo(first);
        assertState(13, 2, 0, 1);
        var next = service.claimRatingReward(userId, STAGE, "new-intent", 3);
        assertThat(next.previousClaimedRating()).isEqualTo(1);
        assertThat(next.gold()).isEqualTo(36);
        assertState(49, 5, 7, 3);
        tx.executeWithoutResult(
                status -> {
                    rewards.deleteAll(rewards.findByStageStageIdOrderByRating(STAGE));
                    wallets.findByUserIdForUpdate(userId).orElseThrow().addGold(31);
                });
        assertThat(service.claimRatingReward(userId, STAGE, "old-intent", 1)).isEqualTo(first);
        assertState(80, 5, 7, 3);
        assertThatThrownBy(() -> service.claimRatingReward(userId, STAGE, "old-intent", 3))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("목표 등급을 변경");
        assertThatThrownBy(() -> service.claimRatingReward(userId, STAGE + 1, "old-intent", 1))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("스테이지·목표 등급");
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void concurrentClaimsGrantOnceForTheSameOrDifferentKeys(boolean sameKey) throws Exception {
        var ready = new CountDownLatch(2);
        var go = new CountDownLatch(1);
        try (var pool = Executors.newFixedThreadPool(2)) {
            var attempts =
                    java.util.stream.IntStream.range(0, 2)
                            .mapToObj(
                                    index ->
                                            pool.submit(
                                                    () -> {
                                                        ready.countDown();
                                                        if (!go.await(5, TimeUnit.SECONDS)) {
                                                            throw new IllegalStateException(
                                                                    "수령 시작 시간 초과");
                                                        }
                                                        try {
                                                            return service.claimRatingReward(
                                                                    userId,
                                                                    STAGE,
                                                                    sameKey
                                                                            ? "same"
                                                                            : "key-" + index,
                                                                    3);
                                                        } catch (ResponseStatusException error) {
                                                            assertThat(
                                                                            error.getStatusCode()
                                                                                    .value())
                                                                    .isEqualTo(409);
                                                            return null;
                                                        }
                                                    }))
                            .toList();
            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            go.countDown();
            var first = attempts.get(0).get(10, TimeUnit.SECONDS);
            var second = attempts.get(1).get(10, TimeUnit.SECONDS);
            if (sameKey) {
                assertThat(first).isNotNull().isEqualTo(second);
            } else {
                assertThat(List.of(first == null, second == null))
                        .containsExactlyInAnyOrder(true, false);
            }
        }
        assertState(49, 5, 7, 3);
        assertThat(receipts.findAll()).filteredOn(row -> row.getUserId().equals(userId)).hasSize(1);
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                "missing-tier",
                "sum-overflow",
                "inventory-overflow",
                "gold-overflow",
                "receipt-failure"
            })
    void failedClaimRollsBackGoldItemsProgressAndReceipt(String reason) {
        tx.executeWithoutResult(
                status -> {
                    if (reason.equals("missing-tier")) {
                        rewards.delete(rewards.findByStageStageIdAndRating(STAGE, 2).orElseThrow());
                    }
                    if (reason.equals("sum-overflow")) {
                        replaceRating(1, 13, Map.of(A, Integer.MAX_VALUE));
                    }
                    if (reason.equals("inventory-overflow")) {
                        inventory.saveAndFlush(
                                new UserItem(
                                        accounts.getReferenceById(userId),
                                        items.findById(A).orElseThrow(),
                                        Integer.MAX_VALUE));
                    }
                    if (reason.equals("gold-overflow")) {
                        wallets.findByUserIdForUpdate(userId)
                                .orElseThrow()
                                .addGold(Integer.MAX_VALUE);
                    }
                });
        if (reason.equals("receipt-failure")) {
            doThrow(new DataIntegrityViolationException("fixture receipt failure"))
                    .when(receipts)
                    .saveAndFlush(any());
            assertThatThrownBy(() -> service.claimRatingReward(userId, STAGE, "failure", 3))
                    .isInstanceOf(DataIntegrityViolationException.class);
        } else {
            assertThatThrownBy(() -> service.claimRatingReward(userId, STAGE, "failure", 3))
                    .isInstanceOf(ResponseStatusException.class);
        }
        assertState(
                reason.equals("gold-overflow") ? Integer.MAX_VALUE : 0,
                reason.equals("inventory-overflow") ? Integer.MAX_VALUE : 0,
                0,
                0);
        assertThat(receipts.findByUserIdAndCreateId(userId, "failure")).isEmpty();
    }

    @Test
    void httpValidatesIntentRestoresReceiptWithAnotherTokenAndIsolatesAccounts() throws Exception {
        String authorization = auth(userId, Role.USER);
        mvc.perform(
                        post("/api/user/stage-progress/claim")
                                .header("Authorization", authorization)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"stageId\":1901}"))
                .andExpect(status().isBadRequest());
        String body = "{\"stageId\":1901,\"createId\":\"http-key\",\"targetRating\":1}";
        mvc.perform(
                        post("/api/user/stage-progress/claim")
                                .header("Authorization", authorization)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.gold").value(13))
                .andExpect(jsonPath("$.items['" + A + "']").value(2));
        String otherDevice = auth(userId, Role.USER);
        mvc.perform(
                        post("/api/user/stage-progress/claim")
                                .header("Authorization", otherDevice)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.claimedRating").value(1))
                .andExpect(jsonPath("$.completedAt").value("2026-10-19T01:02:03.123456Z"));
        mvc.perform(get("/api/user/me").header("Authorization", otherDevice))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.gold").value(13))
                .andExpect(jsonPath("$.items[0].quantity").value(2));
        Long other = newAccount();
        var separate = service.claimRatingReward(other, STAGE, "http-key", 3);
        assertThat(separate.gold()).isEqualTo(49);
        assertState(13, 2, 0, 1);
    }

    @Test
    void adminDefinitionRejectsBadItemsWithoutReplacingTheReferencedReward() throws Exception {
        Long id = ratingService.getStageRatingReward(STAGE, 1).rewardDefinitionId();
        String url = "/api/admin/reward-definitions/" + id;
        String body =
                "{\"name\":\"edited\",\"gold\":23,\"expectedVersion\":1,"
                        + "\"items\":{\""
                        + A
                        + "\":4}}";
        mvc.perform(
                        put(url).header("Authorization", auth(userId, Role.USER))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body))
                .andExpect(status().isForbidden());
        String admin = auth(userId, Role.ADMIN);
        mvc.perform(
                        put(url).header("Authorization", admin)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items['" + A + "']").value(4));
        for (String invalid :
                List.of(
                        "{\"name\":\"invalid\",\"gold\":23,\"expectedVersion\":2,"
                                + "\"items\":{\"unknown\":4}}",
                        "{\"name\":\"invalid\",\"gold\":23,\"expectedVersion\":2,\"items\":{\""
                                + A
                                + "\":0}}")) {
            mvc.perform(
                            put(url).header("Authorization", admin)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(invalid))
                    .andExpect(status().is4xxClientError());
        }
        var unchanged = rewards.findByStageStageIdAndRating(STAGE, 1).orElseThrow();
        assertThat(unchanged.getGold()).isEqualTo(23);
        assertThat(unchanged.getItems()).containsExactlyInAnyOrderEntriesOf(Map.of(A, 4));
    }

    @ParameterizedTest
    @ValueSource(strings = {"POST", "PUT"})
    void ratingAdminRequiresDefinitionIdAndNeverCreatesAnInlineDefinition(String method)
            throws Exception {
        String url = "/api/admin/stage-rating-reward/1901/1";
        String admin = auth(userId, Role.ADMIN);
        long id =
                definitionService
                        .create(new RewardDefinitionRequest("linked", 23, Map.of(A, 4), null))
                        .id();
        final long definitionCount = definitions.count();
        if (method.equals("POST")) {
            ratingService.deleteStageRatingReward(STAGE, 1);
        }
        mvc.perform(
                        (method.equals("POST") ? post(url) : put(url))
                                .header("Authorization", auth(userId, Role.USER))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"rewardDefinitionId\":" + id + "}"))
                .andExpect(status().isForbidden());
        for (String invalid :
                List.of(
                        "{}",
                        "{\"rewardDefinitionId\":null}",
                        "{\"rewardDefinitionId\":0}",
                        "{\"rewardDefinitionId\":-1}",
                        "{\"gold\":23,\"items\":{}}",
                        "{\"rewardDefinitionId\":" + id + ",\"gold\":0}",
                        "{\"rewardDefinitionId\":" + id + ",\"items\":{}}")) {
            mvc.perform(
                            (method.equals("POST") ? post(url) : put(url))
                                    .header("Authorization", admin)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(invalid))
                    .andExpect(status().isBadRequest());
        }
        mvc.perform(
                        (method.equals("POST") ? post(url) : put(url))
                                .header("Authorization", admin)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"rewardDefinitionId\":9223372036854775807}"))
                .andExpect(status().isNotFound());
        assertThat(rewards.existsByStageStageIdAndRating(STAGE, 1)).isEqualTo(method.equals("PUT"));
        mvc.perform(
                        (method.equals("POST") ? post(url) : put(url))
                                .header("Authorization", admin)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"rewardDefinitionId\":" + id + "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rewardDefinitionId").value(id))
                .andExpect(jsonPath("$.gold").value(23))
                .andExpect(jsonPath("$.items['" + A + "']").value(4));
        mvc.perform(
                        put(url).header("Authorization", admin)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"rewardDefinitionId\":9223372036854775807}"))
                .andExpect(status().isNotFound());
        assertThat(ratingService.getStageRatingReward(STAGE, 1).rewardDefinitionId()).isEqualTo(id);
        assertThat(definitions.count()).isEqualTo(definitionCount);
        assertState(0, 0, 0, 0);
    }

    @Test
    void unownedRatingRewardItemCannotBeDeletedUntilAllTierReferencesAreRemoved() {
        assertThat(inventory.existsByItemId(A)).isFalse();
        assertThatThrownBy(() -> itemService.deleteItem(A))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("보상 묶음에서 참조");
        assertThat(items.existsById(A)).isTrue();
        Long firstId =
                rewards.findByStageStageIdAndRating(STAGE, 1)
                        .orElseThrow()
                        .getRewardDefinition()
                        .getId();
        final Long secondId =
                rewards.findByStageStageIdAndRating(STAGE, 2)
                        .orElseThrow()
                        .getRewardDefinition()
                        .getId();
        replaceRating(1, 13, Map.of());
        replaceRating(2, 17, Map.of());
        definitionService.delete(firstId);
        definitionService.delete(secondId);
        itemService.deleteItem(A);
        assertThat(items.existsById(A)).isFalse();
        service.claimRatingReward(userId, STAGE, "after-unlink", 3);
        assertState(49, 0, 7, 3);
    }

    @Test
    void concurrentDefinitionEditAndItemDeletionPreserveReferences() throws Exception {
        items.saveAndFlush(new Item(C, null));
        Long id = ratingService.getStageRatingReward(STAGE, 1).rewardDefinitionId();
        var request = new RewardDefinitionRequest("item race", 13, Map.of(C, 5), 1);
        var ready = new CountDownLatch(2);
        var go = new CountDownLatch(1);
        try (var pool = Executors.newFixedThreadPool(2)) {
            var edit =
                    pool.submit(
                            () -> {
                                ready.countDown();
                                if (!go.await(5, TimeUnit.SECONDS)) {
                                    throw new IllegalStateException("편집 시작 시간 초과");
                                }
                                try {
                                    definitionService.update(id, request);
                                    return true;
                                } catch (ResponseStatusException error) {
                                    assertThat(error.getStatusCode().value()).isEqualTo(404);
                                    return false;
                                }
                            });
            var deletion =
                    pool.submit(
                            () -> {
                                ready.countDown();
                                if (!go.await(5, TimeUnit.SECONDS)) {
                                    throw new IllegalStateException("삭제 시작 시간 초과");
                                }
                                try {
                                    itemService.deleteItem(C);
                                    return true;
                                } catch (ResponseStatusException error) {
                                    assertThat(error.getStatusCode().value()).isEqualTo(409);
                                    return false;
                                }
                            });
            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            go.countDown();
            boolean edited = edit.get(10, TimeUnit.SECONDS);
            boolean deleted = deletion.get(10, TimeUnit.SECONDS);
            assertThat(edited).isNotEqualTo(deleted);
            assertThat(items.existsById(C)).isEqualTo(edited);
            assertThat(rewards.findByStageStageIdAndRating(STAGE, 1).orElseThrow().getItems())
                    .containsKey(edited ? C : A)
                    .doesNotContainKey(edited ? A : C);
        }
    }

    @Test
    void goldOnlyDefinitionsPayGoldAndReplayTheExactReceipt() {
        replaceRating(1, 13, Map.of());
        replaceRating(2, 17, Map.of());
        replaceRating(3, 19, Map.of());
        var receipt = service.claimRatingReward(userId, STAGE, "gold-only", 3);
        assertThat(receipt.gold()).isEqualTo(49);
        assertThat(receipt.items()).isEmpty();
        assertThat(service.claimRatingReward(userId, STAGE, "gold-only", 3)).isEqualTo(receipt);
        assertState(49, 0, 0, 3);
    }

    @Test
    void targetCannotClaimStarsThatHaveNotBeenEarned() {
        tx.executeWithoutResult(
                status ->
                        ReflectionTestUtils.setField(
                                progress.findByUserIdAndStageIdForUpdate(userId, STAGE)
                                        .orElseThrow(),
                                "clearRating",
                                1));
        assertThatThrownBy(() -> service.claimRatingReward(userId, STAGE, "too-high", 3))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("달성한 등급");
        assertState(0, 0, 0, 0);
    }

    @Test
    void deletingAccountRemovesItsClaimReceipts() {
        service.claimRatingReward(userId, STAGE, "delete", 3);
        users.deleteAccount(userId);
        userIds.remove(userId);
        assertThat(receipts.findByUserIdAndCreateId(userId, "delete")).isEmpty();
    }

    @Test
    void sharedDefinitionEditsChangeFutureRewardsWhileOriginalReceiptsStayFixed() {
        final Long id =
                definitionService
                        .create(new RewardDefinitionRequest("shared", 11, Map.of(A, 4), null))
                        .id();
        linkRating(1, id);
        linkRating(2, id);
        assertThat(definitionService.get(id).ratingReferences()).isEqualTo(2);
        var first = service.claimRatingReward(userId, STAGE, "shared-old", 1);
        var changed =
                definitionService.update(
                        id,
                        new RewardDefinitionRequest("shared edited", 23, Map.of(A, 6, B, 1), 1));
        assertThat(changed.version()).isEqualTo(2);
        assertThat(ratingService.getStageRatingReward(STAGE, 2).gold()).isEqualTo(23);
        assertThat(service.claimRatingReward(userId, STAGE, "shared-old", 1)).isEqualTo(first);
        assertState(11, 4, 0, 1);
        var next = service.claimRatingReward(userId, STAGE, "shared-new", 3);
        assertThat(next.gold()).isEqualTo(42);
        assertThat(next.items()).containsExactlyInAnyOrderEntriesOf(Map.of(A, 6, B, 8));
        assertState(53, 10, 8, 3);
        Long other = newAccount();
        var later = service.claimRatingReward(other, STAGE, "other-shared", 2);
        assertThat(later.gold()).isEqualTo(46);
        assertThat(later.items()).containsExactlyInAnyOrderEntriesOf(Map.of(A, 12, B, 2));
        replaceRating(1, 0, Map.of());
        replaceRating(2, 0, Map.of());
        definitionService.delete(id);
        assertThat(service.claimRatingReward(userId, STAGE, "shared-old", 1)).isEqualTo(first);
        assertState(53, 10, 8, 3);
    }

    @Test
    void relinkingRatingSeparatesTheDefinitionWithoutEditingSharedReferences() {
        final Long id =
                definitionService
                        .create(new RewardDefinitionRequest("shared", 11, Map.of(A, 4), null))
                        .id();
        linkRating(1, id);
        linkRating(2, id);
        replaceRating(1, 17, Map.of(B, 5));
        var first = ratingService.getStageRatingReward(STAGE, 1);
        var second = ratingService.getStageRatingReward(STAGE, 2);
        assertThat(first.rewardDefinitionId()).isNotEqualTo(id);
        assertThat(first.gold()).isEqualTo(17);
        assertThat(first.items()).containsExactlyInAnyOrderEntriesOf(Map.of(B, 5));
        assertThat(second.rewardDefinitionId()).isEqualTo(id);
        assertThat(second.gold()).isEqualTo(11);
        assertThat(second.items()).containsExactlyInAnyOrderEntriesOf(Map.of(A, 4));
        assertThat(definitionService.get(id).version()).isEqualTo(1);
        assertThat(definitionService.get(id).ratingReferences()).isEqualTo(1);
    }

    @Test
    void invalidOrStaleDefinitionUpdatesPreserveTheCurrentVersionAndEntries() {
        final Long id =
                definitionService
                        .create(new RewardDefinitionRequest("versioned", 11, Map.of(A, 4), null))
                        .id();
        var before = definitionService.get(id);
        assertThatThrownBy(
                        () ->
                                definitionService.update(
                                        id,
                                        new RewardDefinitionRequest(
                                                "invalid", 17, Map.of("unknown.item", 3), 1)))
                .isInstanceOf(ResponseStatusException.class);
        assertThat(definitionService.get(id)).isEqualTo(before);
        var changed =
                definitionService.update(
                        id, new RewardDefinitionRequest("changed", 17, Map.of(A, 5, B, 2), 1));
        assertThatThrownBy(
                        () ->
                                definitionService.update(
                                        id, new RewardDefinitionRequest("stale", 999, Map.of(), 1)))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("버전이 변경");
        assertThat(definitionService.get(id)).isEqualTo(changed);
        var reduced =
                definitionService.update(
                        id, new RewardDefinitionRequest("reduced", 19, Map.of(B, 7), 2));
        assertThat(reduced.version()).isEqualTo(3);
        assertThat(reduced.items()).containsExactlyInAnyOrderEntriesOf(Map.of(B, 7));
    }

    @Test
    void databaseForeignKeysProtectItemsAndReferencedDefinitions() {
        final Long id =
                rewards.findByStageStageIdAndRating(STAGE, 1)
                        .orElseThrow()
                        .getRewardDefinition()
                        .getId();
        assertThatThrownBy(
                        () ->
                                tx.executeWithoutResult(
                                        status -> {
                                            items.deleteById(A);
                                            items.flush();
                                        }))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(
                        () ->
                                tx.executeWithoutResult(
                                        status -> {
                                            definitions.deleteById(id);
                                            definitions.flush();
                                        }))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(
                        () ->
                                jdbc.update(
                                        "update reward_entry set quantity = 0 "
                                                + "where definition_id = ?",
                                        id))
                .rootCause()
                .isInstanceOf(java.sql.SQLException.class)
                .hasMessageMatching("(?is).*ck_reward_entry_quantity.*");
        assertThatThrownBy(() -> definitionService.delete(id))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("참조 중인 보상 묶음");
        service.claimRatingReward(userId, STAGE, "after-fk-failure", 3);
        assertState(49, 5, 7, 3);
    }

    @Test
    void unreferencedDefinitionOwnsItsItemUntilTheDefinitionIsDeleted() {
        items.saveAndFlush(new Item(C, null));
        final Long id =
                definitionService
                        .create(new RewardDefinitionRequest("standalone", 0, Map.of(C, 3), null))
                        .id();
        assertThatThrownBy(() -> itemService.deleteItem(C))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("보상 묶음에서 참조");
        definitionService.delete(id);
        itemService.deleteItem(C);
        assertThat(items.existsById(C)).isFalse();
    }

    @Test
    void definitionAdminApiRequiresAdminAndEnforcesExclusiveRatingInput() throws Exception {
        String normal = auth(userId, Role.USER);
        String admin = auth(userId, Role.ADMIN);
        String url = "/api/admin/reward-definitions";
        mvc.perform(get(url).header("Authorization", normal)).andExpect(status().isForbidden());
        mvc.perform(
                        post(url)
                                .header("Authorization", normal)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"name\":\"http\",\"gold\":11,\"items\":{}}"))
                .andExpect(status().isForbidden());
        String json =
                mvc.perform(
                                post(url)
                                        .header("Authorization", admin)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(
                                                "{\"name\":\"http\",\"gold\":11,\"items\":{\""
                                                        + A
                                                        + "\":4}}"))
                        .andExpect(status().isOk())
                        .andExpect(jsonPath("$.version").value(1))
                        .andReturn()
                        .getResponse()
                        .getContentAsString();
        long id =
                new com.fasterxml.jackson.databind.ObjectMapper().readTree(json).get("id").asLong();
        mvc.perform(
                        put("/api/admin/stage-rating-reward/1901/1")
                                .header("Authorization", admin)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"rewardDefinitionId\":" + id + "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rewardDefinitionId").value(id))
                .andExpect(jsonPath("$.items['" + A + "']").value(4));
        mvc.perform(get(url + "/" + id).header("Authorization", admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ratingReferences").value(1));
        mvc.perform(
                        put("/api/admin/stage-rating-reward/1901/1")
                                .header("Authorization", admin)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"rewardDefinitionId\":" + id + ",\"gold\":0}"))
                .andExpect(status().isBadRequest());
        mvc.perform(
                        put(url + "/" + id)
                                .header("Authorization", admin)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        "{\"name\":\"edited\",\"gold\":19,"
                                                + "\"items\":{},\"expectedVersion\":1}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.version").value(2));
        mvc.perform(get(url).header("Authorization", admin)).andExpect(status().isOk());
    }

    @Test
    void concurrentSharedDefinitionEditAndClaimUsesOneDefinitionSnapshot() throws Exception {
        final Long id =
                definitionService
                        .create(new RewardDefinitionRequest("shared", 11, Map.of(A, 4), null))
                        .id();
        linkRating(1, id);
        linkRating(2, id);
        var ready = new CountDownLatch(2);
        var go = new CountDownLatch(1);
        try (var pool = Executors.newFixedThreadPool(2)) {
            var edit =
                    pool.submit(
                            () -> {
                                ready.countDown();
                                if (!go.await(5, TimeUnit.SECONDS)) {
                                    throw new IllegalStateException("편집 시간 초과");
                                }
                                return definitionService.update(
                                        id,
                                        new RewardDefinitionRequest("new", 23, Map.of(B, 6), 1));
                            });
            var claim =
                    pool.submit(
                            () -> {
                                ready.countDown();
                                if (!go.await(5, TimeUnit.SECONDS)) {
                                    throw new IllegalStateException("수령 시간 초과");
                                }
                                return service.claimRatingReward(userId, STAGE, "shared-race", 2);
                            });
            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            go.countDown();
            var receipt = claim.get(10, TimeUnit.SECONDS);
            edit.get(10, TimeUnit.SECONDS);
            assertThat(receipt.gold()).isIn(22, 46);
            assertThat(receipt.items())
                    .containsExactlyInAnyOrderEntriesOf(
                            receipt.gold() == 22 ? Map.of(A, 8) : Map.of(B, 12));
            assertThat(service.claimRatingReward(userId, STAGE, "shared-race", 2))
                    .isEqualTo(receipt);
        }
    }

    @Test
    void concurrentDefinitionDeletionAndRatingLinkPreserveReferences() throws Exception {
        final Long id =
                definitionService
                        .create(new RewardDefinitionRequest("link race", 11, Map.of(A, 4), null))
                        .id();
        var ready = new CountDownLatch(2);
        var go = new CountDownLatch(1);
        try (var pool = Executors.newFixedThreadPool(2)) {
            var link =
                    pool.submit(
                            () -> {
                                ready.countDown();
                                if (!go.await(5, TimeUnit.SECONDS)) {
                                    throw new IllegalStateException("연결 시간 초과");
                                }
                                try {
                                    linkRating(1, id);
                                    return true;
                                } catch (ResponseStatusException error) {
                                    assertThat(error.getStatusCode().value()).isEqualTo(404);
                                    return false;
                                }
                            });
            var delete =
                    pool.submit(
                            () -> {
                                ready.countDown();
                                if (!go.await(5, TimeUnit.SECONDS)) {
                                    throw new IllegalStateException("삭제 시간 초과");
                                }
                                try {
                                    definitionService.delete(id);
                                    return true;
                                } catch (ResponseStatusException error) {
                                    assertThat(error.getStatusCode().value()).isEqualTo(409);
                                    return false;
                                }
                            });
            assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
            go.countDown();
            boolean linked = link.get(10, TimeUnit.SECONDS);
            assertThat(delete.get(10, TimeUnit.SECONDS)).isNotEqualTo(linked);
            assertThat(definitions.existsById(id)).isEqualTo(linked);
        }
    }

    @Test
    void databaseRequiresDefinitionForEveryRating() {
        String sql =
                "update stage_rating_reward set reward_definition_id = null where stage_id = ?";
        assertThatThrownBy(() -> jdbc.update(sql, STAGE))
                .rootCause()
                .isInstanceOf(java.sql.SQLException.class);
        assertThat(rewards.findByStageStageIdOrderByRating(STAGE))
                .allSatisfy(reward -> assertThat(reward.getRewardDefinition()).isNotNull());
        var receipt = service.claimRatingReward(userId, STAGE, "required-definition", 3);
        assertThat(receipt.gold()).isEqualTo(49);
        assertThat(receipt.items()).containsExactlyInAnyOrderEntriesOf(Map.of(A, 5, B, 7));
        assertState(49, 5, 7, 3);
    }

    private RewardDefinition fixedDefinition(int gold, Map<String, Integer> amounts) {
        var response =
                definitionService.create(
                        new RewardDefinitionRequest("fixture", gold, amounts, null));
        return definitionService.findDefinition(response.id());
    }

    private void replaceRating(int rating, int gold, Map<String, Integer> amounts) {
        Long definitionId =
                definitionService
                        .create(new RewardDefinitionRequest("replacement", gold, amounts, null))
                        .id();
        linkRating(rating, definitionId);
    }

    private void linkRating(int rating, Long definitionId) {
        var request = new StageRatingRewardRequest();
        ReflectionTestUtils.setField(request, "rewardDefinitionId", definitionId);
        ratingService.updateStageRatingReward(STAGE, rating, request);
    }

    private String auth(Long id, Role role) {
        return "Bearer "
                + tokens.createAccessToken(
                        id, role == Role.ADMIN ? ClientType.WEB : ClientType.GAME, role);
    }

    private void assertState(int gold, int a, int b, int claimed) {
        assertThat(wallets.findByUserId(userId).orElseThrow().getGold()).isEqualTo(gold);
        assertThat(inventory.findByUserIdAndItemId(userId, A).map(UserItem::getQuantity).orElse(0))
                .isEqualTo(a);
        assertThat(inventory.findByUserIdAndItemId(userId, B).map(UserItem::getQuantity).orElse(0))
                .isEqualTo(b);
        assertThat(progress.findByUserIdAndStageId(userId, STAGE).orElseThrow().getClaimedRating())
                .isEqualTo(claimed);
    }
}
