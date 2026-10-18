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
import com.novaserver.reward.dto.RewardDefinitionRequest;
import com.novaserver.reward.repository.RewardDefinitionRepository;
import com.novaserver.reward.service.RewardDefinitionService;
import com.novaserver.shop.dto.ShopConfigurationRequest;
import com.novaserver.shop.dto.ShopReceiptResponse;
import com.novaserver.shop.dto.ShopRerollRequest;
import com.novaserver.shop.repository.ShopConfigurationRepository;
import com.novaserver.shop.repository.ShopOfferRepository;
import com.novaserver.shop.repository.UserShopReceiptRepository;
import com.novaserver.shop.repository.UserShopStateRepository;
import com.novaserver.shop.service.ShopConfigurationService;
import com.novaserver.shop.service.ShopRoller;
import com.novaserver.shop.service.UserShopService;
import com.novaserver.user.Provider;
import com.novaserver.user.Role;
import com.novaserver.user.entity.User;
import com.novaserver.user.entity.UserItem;
import com.novaserver.user.repository.UserEnergyRepository;
import com.novaserver.user.repository.UserItemRepository;
import com.novaserver.user.repository.UserRepository;
import com.novaserver.user.repository.WalletRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
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
class ShopAccountTransactionTest {
    private static final String ITEM = "shop.fixture.item";
    private static final String DAY = "20261019";
    @Autowired private UserShopService service;
    @Autowired private ShopConfigurationService config;
    @Autowired private ShopConfigurationRepository configurations;
    @Autowired private ShopOfferRepository offers;
    @MockitoSpyBean private UserShopStateRepository states;
    @MockitoSpyBean private UserShopReceiptRepository receipts;
    @MockitoSpyBean private ShopRoller roller;
    @Autowired private RewardDefinitionService rewards;
    @Autowired private RewardDefinitionRepository definitions;
    @Autowired private ItemRepository items;
    @Autowired private ItemService itemService;
    @Autowired private UserItemRepository inventory;
    @Autowired private UserService users;
    @Autowired private UserRepository accounts;
    @Autowired private WalletRepository wallets;
    @Autowired private UserEnergyRepository energies;
    @Autowired private PlatformTransactionManager transactions;
    @Autowired private JwtProvider tokens;
    @Autowired private MockMvc mvc;
    @MockitoBean private Clock clock;
    @MockitoBean private TokenBlocklistRepository blocklist;
    private TransactionTemplate tx;
    private Long userId;
    private Long rewardId;
    private final List<Long> userIds = new ArrayList<>();

    @BeforeEach
    void setup() {
        tx = new TransactionTemplate(transactions);
        time("2026-10-19T01:02:03.123456Z");
        items.saveAndFlush(new Item(ITEM, null));
        rewardId =
                rewards.create(
                                new RewardDefinitionRequest(
                                        "shop fixture", 11, Map.of(ITEM, 2), null))
                        .id();
        config.replace(settings(0, rewardId, 17, 3, 7, 2));
        userId = account();
    }

    private ShopConfigurationRequest settings(
            int version, Long reward, int price, int energy, int cost, int limit) {
        return new ShopConfigurationRequest(
                version,
                2,
                limit,
                cost,
                2,
                List.of(10),
                List.of(
                        new ShopConfigurationRequest.Offer("fixture.a", price, energy, reward),
                        new ShopConfigurationRequest.Offer(
                                "fixture.b", price + 6, energy, reward)));
    }

    private Long account() {
        return tx.execute(
                status -> {
                    var u =
                            accounts.saveAndFlush(
                                    new User(Provider.GUEST, UUID.randomUUID().toString()));
                    userIds.add(u.getId());
                    users.createInitialState(u);
                    var wallet = wallets.findByUserId(u.getId()).orElseThrow();
                    wallet.addGold(50);
                    wallet.addGem(100);
                    wallets.saveAndFlush(wallet);
                    return u.getId();
                });
    }

    @AfterEach
    void cleanup() {
        for (Long id : userIds) {
            if (accounts.existsById(id)) {
                users.deleteAccount(id);
            }
        }
        offers.deleteAll();
        configurations.deleteAll();
        definitions.deleteAll();
        items.deleteById(ITEM);
    }

    @Test
    void concurrentFirstReadsPersistExactlyOneIdenticalServerStock() throws Exception {
        var result = race(() -> service.snapshot(userId), () -> service.snapshot(userId));
        assertThat(result.getFirst()).isEqualTo(result.get(1));
        assertThat(states.count()).isEqualTo(1);
        var state = result.getFirst();
        assertThat(state.offers()).hasSize(2);
        assertThat(state.offers().stream().map(s -> s.offer().id()).distinct().count())
                .isEqualTo(2);
        assertThat(state.offers().getFirst().price()).isZero();
        assertThat(state.offers().getFirst().discountPercent()).isEqualTo(100);
        var paid = state.offers().get(1);
        assertThat(paid.price()).isEqualTo(paid.offer().price() * 90 / 100);
        assertThat(state.adAvailable()).isFalse();
    }

    @Test
    void paidPurchaseAndLostResponseRetryGrantExactlyOnce() {
        final var state = service.snapshot(userId);
        int price = state.offers().get(1).price();
        var first = buy("purchase", 1, 0);
        assertThat(first.gemsSpent()).isEqualTo(price);
        assertThat(first.items()).containsEntry(ITEM, 2);
        assertThat(buy("purchase", 1, 0)).isEqualTo(first);
        balances(61, 100 - price, 33, 2);
        assertThat(service.snapshot(userId).purchased()).containsExactly(1);
    }

    @Test
    void concurrentSameRequestReturnsSameReceipt() throws Exception {
        final var state = service.snapshot(userId);
        var result = race(() -> buy("same", 1, 0), () -> buy("same", 1, 0));
        assertThat(result.getFirst()).isEqualTo(result.get(1));
        balances(61, 100 - state.offers().get(1).price(), 33, 2);
        assertThat(receipts.count()).isEqualTo(1);
    }

    @Test
    void differentKeysCannotBuySameSlotTwice() throws Exception {
        final var state = service.snapshot(userId);
        var result = race(() -> attemptBuy("first", 1, 0), () -> attemptBuy("second", 1, 0));
        assertThat(result).containsExactlyInAnyOrder(true, false);
        balances(61, 100 - state.offers().get(1).price(), 33, 2);
        assertThat(receipts.count()).isEqualTo(1);
    }

    @Test
    void firstSlotIsFreeOncePerRollAndRerollResetsPurchasedSlots() {
        service.snapshot(userId);
        buy("free", 0, 0);
        balances(61, 100, 33, 2);
        assertThatThrownBy(() -> buy("free-again", 0, 0))
                .isInstanceOf(ResponseStatusException.class);
        var changed = reroll("roll", 0);
        assertThat(changed.goldSpent()).isEqualTo(7);
        assertThat(changed.stateAfter().purchased()).isEmpty();
        assertThat(changed.stateAfter().rerollIndex()).isEqualTo(1);
        buy("free-next", 0, 1);
        balances(65, 100, 36, 4);
    }

    @Test
    void concurrentRerollRetryChargesOnlyOnce() throws Exception {
        service.snapshot(userId);
        var result = race(() -> reroll("same-roll", 0), () -> reroll("same-roll", 0));
        assertThat(result.getFirst()).isEqualTo(result.get(1));
        assertThat(service.snapshot(userId)).isEqualTo(result.getFirst().stateAfter());
        balances(43, 100, 30, 0);
    }

    @Test
    void differentRerollKeysFromSameScreenCannotChargeTwice() throws Exception {
        service.snapshot(userId);
        var result = race(() -> attemptReroll("roll-a", 0), () -> attemptReroll("roll-b", 0));
        assertThat(result).containsExactlyInAnyOrder(true, false);
        assertThat(service.snapshot(userId).goldUsed()).isEqualTo(1);
        balances(43, 100, 30, 0);
    }

    @Test
    void purchaseAndRerollRaceNeverBuysReplacementAtOldRevision() throws Exception {
        final var state = service.snapshot(userId);
        var result = race(() -> attemptBuy("buy-race", 1, 0), () -> attemptReroll("roll-race", 0));
        assertThat(result.get(1)).isTrue();
        assertThat(service.snapshot(userId).rerollIndex()).isEqualTo(1);
        assertThat(service.snapshot(userId).purchased()).isEmpty();
        balances(
                result.getFirst() ? 54 : 43,
                result.getFirst() ? 100 - state.offers().get(1).price() : 100,
                result.getFirst() ? 33 : 30,
                result.getFirst() ? 2 : 0);
    }

    @Test
    void rerollLimitAndInsufficientGoldLeaveStateUnchanged() {
        service.snapshot(userId);
        reroll("r1", 0);
        reroll("r2", 1);
        final var before = service.snapshot(userId);
        assertThatThrownBy(() -> reroll("r3", 2)).isInstanceOf(ResponseStatusException.class);
        assertThat(service.snapshot(userId)).isEqualTo(before);
        balances(36, 100, 30, 0);
    }

    @Test
    void insufficientGoldDoesNotClearPurchaseOrCount() {
        service.snapshot(userId);
        buy("free", 0, 0);
        setWallet("gold", 0);
        final var before = service.snapshot(userId);
        assertThatThrownBy(() -> reroll("no-gold", 0)).isInstanceOf(ResponseStatusException.class);
        assertThat(service.snapshot(userId)).isEqualTo(before);
        balances(0, 100, 33, 2);
    }

    @Test
    void insufficientGemsInvalidSlotAndStaleRevisionDoNotChangeAnything() {
        service.snapshot(userId);
        setWallet("gem", 0);
        assertThatThrownBy(() -> buy("no-gems", 1, 0)).isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> buy("bad-slot", 2, 0)).isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> buy("old-revision", 0, 1))
                .isInstanceOf(ResponseStatusException.class);
        balances(50, 0, 30, 0);
        assertThat(receipts.count()).isZero();
        assertThat(service.snapshot(userId).purchased()).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"gold", "energy", "items"})
    void payoutOverflowRollsBackGemsStockAndEveryOtherReward(String kind) {
        service.snapshot(userId);
        tx.executeWithoutResult(
                s -> {
                    if (kind.equals("gold")) {
                        var wallet = wallets.findByUserId(userId).orElseThrow();
                        ReflectionTestUtils.setField(wallet, "gold", Integer.MAX_VALUE);
                        wallets.saveAndFlush(wallet);
                    } else if (kind.equals("energy")) {
                        var energy = energies.findByUserId(userId).orElseThrow();
                        ReflectionTestUtils.setField(energy, "energyStored", Integer.MAX_VALUE);
                        energies.saveAndFlush(energy);
                    } else {
                        inventory.saveAndFlush(
                                new UserItem(
                                        accounts.getReferenceById(userId),
                                        items.getReferenceById(ITEM),
                                        Integer.MAX_VALUE));
                    }
                });
        assertThatThrownBy(() -> buy("overflow", 1, 0)).isInstanceOf(ResponseStatusException.class);
        balances(
                kind.equals("gold") ? Integer.MAX_VALUE : 50,
                100,
                kind.equals("energy") ? Integer.MAX_VALUE : 30,
                kind.equals("items") ? Integer.MAX_VALUE : 0);
        assertThat(service.snapshot(userId).purchased()).isEmpty();
        assertThat(receipts.count()).isZero();
    }

    @Test
    void receiptFailureRollsBackMoneyRewardsAndStock() {
        service.snapshot(userId);
        doThrow(new DataIntegrityViolationException("fixture receipt failure"))
                .when(receipts)
                .saveAndFlush(any());
        assertThatThrownBy(() -> buy("receipt-fail", 1, 0))
                .isInstanceOf(DataIntegrityViolationException.class);
        balances(50, 100, 30, 0);
        assertThat(service.snapshot(userId).purchased()).isEmpty();
    }

    @Test
    void stateWriteFailureRollsBackAllPurchaseEffects() {
        service.snapshot(userId);
        doThrow(new DataIntegrityViolationException("fixture state failure"))
                .when(states)
                .saveAndFlush(any());
        assertThatThrownBy(() -> buy("state-fail", 1, 0))
                .isInstanceOf(DataIntegrityViolationException.class);
        balances(50, 100, 30, 0);
        assertThat(service.snapshot(userId).purchased()).isEmpty();
    }

    @Test
    void rerollGenerationFailurePreservesCostCountAndPurchasedSlots() {
        service.snapshot(userId);
        buy("free", 0, 0);
        final var before = service.snapshot(userId);
        doThrow(new IllegalStateException("fixture generator failure")).when(roller).roll(any());
        assertThatThrownBy(() -> reroll("generation-fail", 0))
                .isInstanceOf(IllegalStateException.class);
        assertThat(service.snapshot(userId)).isEqualTo(before);
        balances(61, 100, 33, 2);
    }

    @Test
    void requestIdReuseForDifferentTargetOrOperationIsRejected() {
        service.snapshot(userId);
        var first = buy("identity", 0, 0);
        assertThatThrownBy(() -> buy("identity", 1, 0)).isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> reroll("identity", 0)).isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> service.buy(userId, "identity", "20261020", 0, 0))
                .isInstanceOf(ResponseStatusException.class);
        assertThat(buy("identity", 0, 0)).isEqualTo(first);
        balances(61, 100, 33, 2);
    }

    @Test
    void oldPurchaseRetryAfterRerollReturnsOriginalReceiptWithoutRestoringOldStock() {
        service.snapshot(userId);
        var first = buy("original", 0, 0);
        reroll("next-stock", 0);
        var current = service.snapshot(userId);
        assertThat(buy("original", 0, 0)).isEqualTo(first);
        assertThat(first.stateAfter().rerollIndex()).isZero();
        assertThat(service.snapshot(userId)).isEqualTo(current);
    }

    @Test
    void kst04ResetsOnlyDailyStateAndOldRequestReplaysWithoutNewPayout() {
        time("2026-10-19T18:59:59.999999Z");
        service.snapshot(userId);
        var first = buy("before-day", 0, 0);
        var rollReceipt = reroll("before-roll", 0);
        time("2026-10-19T19:00:00Z");
        assertThat(buy("before-day", 0, 0)).isEqualTo(first);
        assertThat(reroll("before-roll", 0)).isEqualTo(rollReceipt);
        assertThatThrownBy(() -> buy("late-new-request", 1, 1))
                .isInstanceOf(ResponseStatusException.class);
        var next = service.snapshot(userId);
        assertThat(next.dayKey()).isEqualTo("20261020");
        assertThat(next.goldUsed()).isZero();
        assertThat(next.purchased()).isEmpty();
        assertThat(next.nextResetAt()).isEqualTo(Instant.parse("2026-10-20T19:00:00Z"));
        balances(54, 100, 33, 2);
    }

    @Test
    void debitReceiptAndStockUseOneTimestampWhenClockCrossesBoundary() {
        service.snapshot(userId);
        Instant before = Instant.parse("2026-10-19T18:59:59.999999Z");
        when(clock.instant()).thenReturn(before, Instant.parse("2026-10-19T19:00:00Z"));
        var receipt = buy("crossing", 1, 0);
        assertThat(receipt.completedAt()).isEqualTo(before);
        assertThat(receipt.dayKey()).isEqualTo(DAY);
        assertThat(receipt.stateAfter().dayKey()).isEqualTo(DAY);
        time("2026-10-19T19:00:00Z");
        assertThat(service.snapshot(userId).purchased()).isEmpty();
    }

    @Test
    void currentDayPolicyAndActualRewardsStayFrozenAcrossAdminAndBundleChanges() {
        var frozen = service.snapshot(userId);
        rewards.update(rewardId, new RewardDefinitionRequest("changed", 19, Map.of(ITEM, 4), 1));
        config.replace(settings(1, rewardId, 99, 1, 31, 1));
        assertThat(service.snapshot(userId)).isEqualTo(frozen);
        var receipt = buy("frozen", 1, 0);
        assertThat(receipt.gold()).isEqualTo(11);
        assertThat(receipt.items()).containsEntry(ITEM, 2);
        reroll("old-price", 0);
        assertThat(service.snapshot(userId).goldRerollCost()).isEqualTo(7);
        time("2026-10-19T19:00:00Z");
        var next = service.snapshot(userId);
        assertThat(next.configVersion()).isEqualTo(2);
        assertThat(next.goldRerollCost()).isEqualTo(31);
        assertThat(next.offers()).allSatisfy(s -> assertThat(s.offer().gold()).isEqualTo(19));
        assertThat(buy("frozen", 1, 0)).isEqualTo(receipt);
    }

    @Test
    void frozenItemReferencesSurviveUnlinkAndCommonBundleDelete() {
        service.snapshot(userId);
        assertThat(rewards.get(rewardId).shopReferences()).isEqualTo(2);
        assertThatThrownBy(() -> rewards.delete(rewardId))
                .isInstanceOf(ResponseStatusException.class);
        Long replacement =
                rewards.create(new RewardDefinitionRequest("replacement", 5, Map.of(), null)).id();
        config.replace(settings(1, replacement, 9, 0, 7, 2));
        rewards.delete(rewardId);
        assertThatThrownBy(() -> itemService.deleteItem(ITEM))
                .isInstanceOf(ResponseStatusException.class);
        var receipt = buy("after-unlink", 1, 0);
        assertThat(receipt.gold()).isEqualTo(11);
        assertThat(receipt.items()).containsEntry(ITEM, 2);
    }

    @Test
    void newTokenRestoresExactStockAndOtherAccountRemainsIsolated() throws Exception {
        var original = service.snapshot(userId);
        buy("account-key", 0, 0);
        mvc.perform(get("/api/user/me").header("Authorization", token(userId, Role.USER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.shop.dayKey").value(DAY))
                .andExpect(jsonPath("$.shop.purchased[0]").value(0));
        assertThat(service.snapshot(userId).offers()).isEqualTo(original.offers());
        Long other = account();
        assertThat(service.snapshot(other).purchased()).isEmpty();
        service.buy(other, "account-key", DAY, 0, 0);
        assertThat(wallets.findByUserId(other).orElseThrow().getGold()).isEqualTo(61);
        assertThat(service.snapshot(other).goldUsed()).isZero();
        assertThat(service.snapshot(userId).goldUsed()).isZero();
    }

    @Test
    void unconfiguredShopIsUnavailableWithoutBreakingMeOrInventingRewards() throws Exception {
        offers.deleteAll();
        configurations.deleteAll();
        assertThat(service.snapshot(userId).available()).isFalse();
        assertThat(states.count()).isZero();
        assertThatThrownBy(() -> buy("not-ready", 0, 0))
                .isInstanceOf(ResponseStatusException.class);
        mvc.perform(get("/api/user/me").header("Authorization", token(userId, Role.USER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.shop.available").value(false));
        balances(50, 100, 30, 0);
    }

    @Test
    void adRerollIsUnavailableAndDoesNotChangeAnyState() throws Exception {
        final var before = service.snapshot(userId);
        mvc.perform(
                        post("/api/user/shop/reroll")
                                .header("Authorization", token(userId, Role.USER))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        "{\"requestId\":\"ad\",\"dayKey\":\""
                                                + DAY
                                                + "\",\"rerollIndex\":0,\"kind\":\"AD\"}"))
                .andExpect(status().isServiceUnavailable());
        assertThat(service.snapshot(userId)).isEqualTo(before);
        balances(50, 100, 30, 0);
        assertThat(receipts.count()).isZero();
    }

    @Test
    void clientPricesRewardsAndAdminPrivilegesCannotBeForged() throws Exception {
        service.snapshot(userId);
        for (String extra : List.of("\"price\":0", "\"gold\":100000", "\"items\":{}")) {
            mvc.perform(
                            post("/api/user/shop/buy")
                                    .header("Authorization", token(userId, Role.USER))
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(
                                            "{\"requestId\":\"forged\",\"dayKey\":\""
                                                    + DAY
                                                    + "\",\"rerollIndex\":0,\"slot\":1,"
                                                    + extra
                                                    + "}"))
                    .andExpect(status().isBadRequest());
        }
        mvc.perform(
                        put("/api/admin/shop")
                                .header("Authorization", token(userId, Role.USER))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{}"))
                .andExpect(status().isForbidden());
        balances(50, 100, 30, 0);
    }

    @Test
    void actualHttpPurchaseAndRetryReturnOriginalUtcReceipt() throws Exception {
        service.snapshot(userId);
        String body =
                "{\"requestId\":\"http\",\"dayKey\":\"" + DAY + "\",\"rerollIndex\":0,\"slot\":0}";
        for (int i = 0; i < 2; i++) {
            mvc.perform(
                            post("/api/user/shop/buy")
                                    .header("Authorization", token(userId, Role.USER))
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(body))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.gold").value(11))
                    .andExpect(jsonPath("$.completedAt").value("2026-10-19T01:02:03.123456Z"));
        }
        balances(61, 100, 33, 2);
    }

    @Test
    void accountDeletionRemovesReceiptsStateAndFrozenItemLinks() {
        service.snapshot(userId);
        buy("delete-key", 0, 0);
        users.deleteAccount(userId);
        assertThat(receipts.count()).isZero();
        assertThat(states.count()).isZero();
        assertThat(states.existsByCatalogItem(ITEM)).isFalse();
    }

    @Test
    void staleOrInvalidAdminConfigurationDoesNotPartiallyReplaceCatalog() {
        var before = config.get();
        assertThatThrownBy(() -> config.replace(settings(0, rewardId, 9, 3, 7, 2)))
                .isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> config.replace(settings(1, Long.MAX_VALUE, 9, 3, 7, 2)))
                .isInstanceOf(ResponseStatusException.class);
        assertThat(config.get()).isEqualTo(before);
    }

    @ParameterizedTest
    @ValueSource(strings = {"receipt", "state"})
    void rerollPersistenceFailureRollsBackCostCountStockAndSoldOutSlots(String target) {
        service.snapshot(userId);
        buy("before-failure", 0, 0);
        final var before = service.snapshot(userId);
        if (target.equals("receipt")) {
            doThrow(new DataIntegrityViolationException("fixture roll receipt failure"))
                    .when(receipts)
                    .saveAndFlush(any());
        } else {
            doThrow(new DataIntegrityViolationException("fixture roll state failure"))
                    .when(states)
                    .saveAndFlush(any());
        }
        assertThatThrownBy(() -> reroll("persist-failure", 0))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThat(service.snapshot(userId)).isEqualTo(before);
        balances(61, 100, 33, 2);
    }

    @Test
    void newDayGenerationFailureDoesNotPartiallyResetPersistedState() {
        final var before = service.snapshot(userId);
        time("2026-10-19T19:00:00Z");
        doThrow(new IllegalStateException("fixture new day roll failure")).when(roller).roll(any());
        assertThatThrownBy(() -> service.snapshot(userId))
                .isInstanceOf(IllegalStateException.class);
        assertThat(states.findByUserId(userId).orElseThrow().getShopDay().toString())
                .isEqualTo("2026-10-19");
        assertThat(states.findByUserId(userId).orElseThrow().getOffers())
                .isEqualTo(before.offers());
        balances(50, 100, 30, 0);
    }

    @Test
    void concurrentNewDayQueriesPersistOneSameResetAndPreserveReceipt() throws Exception {
        service.snapshot(userId);
        buy("old-day", 0, 0);
        time("2026-10-19T19:00:00Z");
        var results = race(() -> service.snapshot(userId), () -> service.snapshot(userId));
        assertThat(results.getFirst()).isEqualTo(results.get(1));
        assertThat(results.getFirst().dayKey()).isEqualTo("20261020");
        assertThat(results.getFirst().purchased()).isEmpty();
        assertThat(states.count()).isEqualTo(1);
        assertThat(receipts.count()).isEqualTo(1);
    }

    @Test
    void maximumGemPriceDiscountUsesLongMathAndCannotIncreaseBalance() {
        var rows =
                List.of(
                        new ShopConfigurationRequest.Offer(
                                "large.a", Integer.MAX_VALUE, 0, rewardId),
                        new ShopConfigurationRequest.Offer(
                                "large.b", Integer.MAX_VALUE, 0, rewardId));
        config.replace(new ShopConfigurationRequest(1, 2, 2, 7, 2, List.of(10), rows));
        setWallet("gem", Integer.MAX_VALUE);
        final var state = service.snapshot(userId);
        int price = (int) ((long) Integer.MAX_VALUE * 90 / 100);
        assertThat(state.offers().get(1).price()).isEqualTo(price);
        buy("large-price", 1, 0);
        balances(61, Integer.MAX_VALUE - price, 30, 2);
    }

    @Test
    void actualAdminHttpReplacesPolicyWithVersionAndRejectsMissingVersion() throws Exception {
        String body =
                "{\"expectedVersion\":1,\"slots\":1,\"goldRerollsPerDay\":2,"
                        + "\"goldRerollCost\":7,\"adRerollsPerDay\":2,\"discounts\":[10],"
                        + "\"offers\":[{\"id\":\"http.offer\",\"price\":17,\"energy\":3,"
                        + "\"rewardDefinitionId\":"
                        + rewardId
                        + "}]}";
        mvc.perform(
                        put("/api/admin/shop")
                                .header("Authorization", token(userId, Role.ADMIN))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.version").value(2));
        mvc.perform(
                        put("/api/admin/shop")
                                .header("Authorization", token(userId, Role.ADMIN))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{}"))
                .andExpect(status().isBadRequest());
        assertThat(service.snapshot(userId).configVersion()).isEqualTo(2);
    }

    private void time(String now) {
        when(clock.instant()).thenReturn(Instant.parse(now));
    }

    private ShopReceiptResponse buy(String key, int slot, int revision) {
        return service.buy(userId, key, DAY, revision, slot);
    }

    private ShopReceiptResponse reroll(String key, int revision) {
        return service.reroll(userId, key, DAY, revision, ShopRerollRequest.Kind.GOLD);
    }

    private boolean attemptBuy(String key, int slot, int revision) {
        try {
            buy(key, slot, revision);
            return true;
        } catch (ResponseStatusException e) {
            assertThat(e.getStatusCode().value()).isEqualTo(409);
            return false;
        }
    }

    private boolean attemptReroll(String key, int revision) {
        try {
            reroll(key, revision);
            return true;
        } catch (ResponseStatusException e) {
            assertThat(e.getStatusCode().value()).isEqualTo(409);
            return false;
        }
    }

    private String token(Long id, Role role) {
        return "Bearer "
                + tokens.createAccessToken(
                        id, role == Role.ADMIN ? ClientType.WEB : ClientType.GAME, role);
    }

    private void setWallet(String field, int amount) {
        tx.executeWithoutResult(
                s -> {
                    var w = wallets.findByUserId(userId).orElseThrow();
                    ReflectionTestUtils.setField(w, field, amount);
                    wallets.saveAndFlush(w);
                });
    }

    private void balances(int gold, int gems, int energy, int amount) {
        var wallet = wallets.findByUserId(userId).orElseThrow();
        assertThat(wallet.getGold()).isEqualTo(gold);
        assertThat(wallet.getGem()).isEqualTo(gems);
        assertThat(energies.findByUserId(userId).orElseThrow().getEnergyStored()).isEqualTo(energy);
        assertThat(
                        inventory
                                .findByUserIdAndItemId(userId, ITEM)
                                .map(UserItem::getQuantity)
                                .orElse(0))
                .isEqualTo(amount);
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
