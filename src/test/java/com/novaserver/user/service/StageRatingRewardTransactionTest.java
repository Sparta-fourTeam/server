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
import com.novaserver.auth.repository.TokenBlocklistRepository;
import com.novaserver.item.entity.Item;
import com.novaserver.item.repository.ItemRepository;
import com.novaserver.item.service.ItemService;
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
    @Autowired private StageRatingRewardService ratingService;
    @Autowired private PlatformTransactionManager transactions;
    @Autowired private MockMvc mvc;
    @Autowired private JwtProvider tokens;
    @Autowired private JdbcTemplate jdbc;
    @MockitoSpyBean private StageRatingClaimRepository receipts;
    @MockitoBean private Clock clock;
    @MockitoBean private TokenBlocklistRepository blocklist;
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
                                    new StageRatingReward(stage, 1, 13, Map.of(A, 2)),
                                    new StageRatingReward(stage, 2, 17, Map.of(A, 3)),
                                    new StageRatingReward(stage, 3, 19, Map.of(B, 7))));
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
                        rewards.findByStageStageIdAndRating(STAGE, 1)
                                .orElseThrow()
                                .update(13, Map.of(A, Integer.MAX_VALUE));
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
    void adminItemConfigurationRejectsBadDataWithoutReplacingTheExistingReward() throws Exception {
        String url = "/api/admin/stage-rating-reward/1901/1";
        String body = "{\"gold\":23,\"items\":{\"" + A + "\":4}}";
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
                        "{\"gold\":23,\"items\":{\"unknown\":4}}",
                        "{\"gold\":23,\"items\":{\"" + A + "\":0}}")) {
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

    @Test
    void unownedRatingRewardItemCannotBeDeletedUntilAllTierReferencesAreRemoved() {
        assertThat(inventory.existsByItemId(A)).isFalse();
        assertThatThrownBy(() -> itemService.deleteItem(A))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("별등급 보상에서 참조");
        assertThat(items.existsById(A)).isTrue();
        tx.executeWithoutResult(
                status -> {
                    rewards.findByStageStageIdAndRating(STAGE, 1)
                            .orElseThrow()
                            .update(13, Map.of());
                    rewards.findByStageStageIdAndRating(STAGE, 2)
                            .orElseThrow()
                            .update(17, Map.of());
                });
        itemService.deleteItem(A);
        assertThat(items.existsById(A)).isFalse();
        service.claimRatingReward(userId, STAGE, "after-unlink", 3);
        assertState(49, 0, 7, 3);
    }

    @Test
    void concurrentRatingEditAndItemDeletionPreserveReferences() throws Exception {
        items.saveAndFlush(new Item(C, null));
        var request = new StageRatingRewardRequest();
        ReflectionTestUtils.setField(request, "gold", 13);
        ReflectionTestUtils.setField(request, "items", Map.of(C, 5));
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
                                    ratingService.updateStageRatingReward(STAGE, 1, request);
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
    void goldOnlyRowsFromBeforeTheItemsColumnStillPayTheirGold() {
        jdbc.update("update stage_rating_reward set items = null where stage_id = ?", STAGE);
        var receipt = service.claimRatingReward(userId, STAGE, "legacy", 3);
        assertThat(receipt.items()).isEmpty();
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
