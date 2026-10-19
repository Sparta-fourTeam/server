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
import com.novaserver.reward.dto.RewardDefinitionRequest;
import com.novaserver.reward.repository.RewardDefinitionRepository;
import com.novaserver.reward.service.RewardDefinitionService;
import com.novaserver.shop.dto.GoldPackConfigurationRequest;
import com.novaserver.shop.dto.ShopConfigurationRequest;
import com.novaserver.shop.dto.ShopRerollRequest;
import com.novaserver.shop.repository.GoldPackConfigurationRepository;
import com.novaserver.shop.repository.ShopConfigurationRepository;
import com.novaserver.shop.repository.ShopOfferRepository;
import com.novaserver.shop.repository.UserGoldPackReceiptRepository;
import com.novaserver.shop.repository.UserShopReceiptRepository;
import com.novaserver.shop.service.GoldPackConfigurationService;
import com.novaserver.shop.service.GoldPackPrices;
import com.novaserver.shop.service.ShopConfigurationService;
import com.novaserver.shop.service.UserGoldPackService;
import com.novaserver.shop.service.UserShopService;
import com.novaserver.user.Provider;
import com.novaserver.user.Role;
import com.novaserver.user.entity.User;
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
import org.junit.jupiter.params.provider.CsvSource;
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
class GoldPackExchangeTransactionTest {
    @Autowired private UserGoldPackService service;
    @Autowired private GoldPackConfigurationService config;
    @Autowired private GoldPackConfigurationRepository configurations;
    @MockitoSpyBean private UserGoldPackReceiptRepository receipts;
    @Autowired private UserShopReceiptRepository dailyReceipts;
    @Autowired private UserShopService shops;
    @Autowired private ShopConfigurationService dailyConfig;
    @Autowired private ShopConfigurationRepository dailyConfigurations;
    @Autowired private ShopOfferRepository offers;
    @Autowired private RewardDefinitionService rewards;
    @Autowired private RewardDefinitionRepository definitions;
    @Autowired private UserService users;
    @Autowired private UserRepository accounts;
    @Autowired private WalletRepository wallets;
    @Autowired private PlatformTransactionManager transactions;
    @Autowired private JwtProvider tokens;
    @Autowired private MockMvc mvc;
    @MockitoBean private Clock clock;
    @MockitoBean private TokenBlocklistRepository blocklist;
    @MockitoBean private AccessTokenRepository accessTokenRepository;
    private TransactionTemplate tx;
    private Long userId;
    private final List<Long> userIds = new ArrayList<>();

    @BeforeEach
    void setup() {
        tx = new TransactionTemplate(transactions);
        time("2026-10-19T01:02:03.123456Z");
        config.replace(settings(0, 11, 101, 203));
        userId = account();
    }

    private GoldPackConfigurationRequest settings(int version, int rate, int small, int large) {
        return new GoldPackConfigurationRequest(
                version,
                rate,
                List.of(
                        new GoldPackConfigurationRequest.Pack(small, 0),
                        new GoldPackConfigurationRequest.Pack(large, 7)));
    }

    private Long account() {
        return tx.execute(
                s -> {
                    var u =
                            accounts.saveAndFlush(
                                    new User(Provider.GUEST, UUID.randomUUID().toString()));
                    userIds.add(u.getId());
                    users.createInitialState(u);
                    var wallet = wallets.findByUserId(u.getId()).orElseThrow();
                    wallet.addGold(40);
                    wallet.addGem(1000);
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
        configurations.deleteAll();
        offers.deleteAll();
        dailyConfigurations.deleteAll();
        definitions.deleteAll();
    }

    @ParameterizedTest
    @CsvSource({
        "101,0,11,9",
        "203,7,11,17",
        "2147483647,0,1,2147483647",
        "2147483647,99,1,21474836",
        "1,99,2147483647,0"
    })
    void integerFloorFormulaMatchesLocalAndUsesLongMath(
            int gold, int discount, int rate, int price) {
        assertThat(GoldPackPrices.price(gold, discount, rate)).isEqualTo(price);
    }

    @Test
    void serverCatalogAndPurchaseUseConfiguredGoldAndGemPrice() {
        var catalog = service.catalog(userId);
        assertThat(catalog.available()).isTrue();
        assertThat(catalog.version()).isEqualTo(1);
        assertThat(catalog.packs().get(0).gemPrice()).isEqualTo(9);
        assertThat(catalog.packs().get(1).gemPrice()).isEqualTo(17);
        var receipt = service.buy(userId, "buy", 1, 1);
        assertThat(receipt.gold()).isEqualTo(203);
        assertThat(receipt.gemsSpent()).isEqualTo(17);
        assertThat(receipt.gemGoldRate()).isEqualTo(11);
        assertThat(receipt.discountPercent()).isEqualTo(7);
        balances(userId, 243, 983);
    }

    @Test
    void responseLossRetryAfterBalanceChangesReturnsOriginalReceipt() {
        final var first = service.buy(userId, "replay", 1, 0);
        tx.executeWithoutResult(s -> wallets.findByUserId(userId).orElseThrow().addGold(3));
        assertThat(service.buy(userId, "replay", 1, 0)).isEqualTo(first);
        balances(userId, 144, 991);
        assertThat(receipts.count()).isEqualTo(1);
    }

    @Test
    void concurrentSameKeyDebitsAndGrantsOnce() throws Exception {
        var result =
                race(
                        () -> service.buy(userId, "same", 1, 0),
                        () -> service.buy(userId, "same", 1, 0));
        assertThat(result.getFirst()).isEqualTo(result.get(1));
        balances(userId, 141, 991);
        assertThat(receipts.count()).isEqualTo(1);
    }

    @Test
    void differentKeysAreSeparatePurchasesAndNoNewDailyLimitIsInvented() {
        for (int i = 0; i < 8; i++) {
            service.buy(userId, "order-" + i, 1, 0);
        }
        balances(userId, 848, 928);
        assertThat(receipts.count()).isEqualTo(8);
    }

    @Test
    void concurrentDifferentKeysSerializeAgainstActualGemBalance() throws Exception {
        setWallet("gem", 9);
        var result = race(() -> attempt("a", 0), () -> attempt("b", 0));
        assertThat(result).containsExactlyInAnyOrder(true, false);
        balances(userId, 141, 0);
        assertThat(receipts.count()).isEqualTo(1);
    }

    @Test
    void insufficientGemsAndInvalidPackDoNotCreateReceiptsOrChangeBalances() {
        setWallet("gem", 8);
        assertThatThrownBy(() -> service.buy(userId, "poor", 1, 0))
                .isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> service.buy(userId, "negative", 1, -1))
                .isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> service.buy(userId, "missing", 1, 2))
                .isInstanceOf(ResponseStatusException.class);
        balances(userId, 40, 8);
        assertThat(receipts.count()).isZero();
    }

    @Test
    void goldOverflowRollsBackGemDebitAndReceipt() {
        setWallet("gold", Integer.MAX_VALUE);
        assertThatThrownBy(() -> service.buy(userId, "overflow", 1, 0))
                .isInstanceOf(ResponseStatusException.class);
        balances(userId, Integer.MAX_VALUE, 1000);
        assertThat(receipts.count()).isZero();
    }

    @Test
    void receiptFailureRollsBackDebitAndCommonGoldGrant() {
        doThrow(new DataIntegrityViolationException("fixture receipt failure"))
                .when(receipts)
                .saveAndFlush(any());
        assertThatThrownBy(() -> service.buy(userId, "receipt-fail", 1, 0))
                .isInstanceOf(DataIntegrityViolationException.class);
        balances(userId, 40, 1000);
        assertThat(receipts.count()).isZero();
    }

    @Test
    void sameKeyCannotChangeIndexOrCatalogVersion() {
        final var first = service.buy(userId, "identity", 1, 0);
        assertThatThrownBy(() -> service.buy(userId, "identity", 1, 1))
                .isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> service.buy(userId, "identity", 2, 0))
                .isInstanceOf(ResponseStatusException.class);
        assertThat(service.buy(userId, "identity", 1, 0)).isEqualTo(first);
        balances(userId, 141, 991);
    }

    @Test
    void changedCatalogRejectsOldScreenButRestoresOldSuccessWithoutRepricing() {
        final var first = service.buy(userId, "old", 1, 0);
        config.replace(settings(1, 13, 301, 503));
        assertThatThrownBy(() -> service.buy(userId, "stale-new", 1, 0))
                .isInstanceOf(ResponseStatusException.class);
        assertThat(service.buy(userId, "old", 1, 0)).isEqualTo(first);
        var next = service.buy(userId, "new", 2, 0);
        assertThat(next.gold()).isEqualTo(301);
        assertThat(next.gemsSpent()).isEqualTo(23);
        balances(userId, 442, 968);
    }

    @Test
    void concurrentCatalogUpdateEitherBuysOldQuoteOrRejectsWithoutPartialDebit() throws Exception {
        var result =
                race(
                        () -> attempt("config-race", 0),
                        () -> {
                            config.replace(settings(1, 13, 301, 503));
                            return true;
                        });
        assertThat(config.get().version()).isEqualTo(2);
        if (result.getFirst()) {
            balances(userId, 141, 991);
        } else {
            balances(userId, 40, 1000);
        }
    }

    @Test
    void kst04AndNextDayDoNotResetIdempotencyOrAddPurchaseLimits() {
        time("2026-10-19T18:59:59.999999Z");
        final var first = service.buy(userId, "before-reset", 1, 0);
        time("2026-10-19T19:00:00Z");
        assertThat(service.buy(userId, "before-reset", 1, 0)).isEqualTo(first);
        service.buy(userId, "after-reset", 1, 0);
        balances(userId, 242, 982);
        assertThat(service.catalog(userId).version()).isEqualTo(1);
    }

    @Test
    void utcReceiptPrecisionIsStableOnReplay() {
        time("2026-10-19T01:02:03.123456789Z");
        final var first = service.buy(userId, "precision", 1, 0);
        assertThat(first.completedAt()).isEqualTo(Instant.parse("2026-10-19T01:02:03.123456Z"));
        assertThat(service.buy(userId, "precision", 1, 0)).isEqualTo(first);
    }

    @Test
    void noConfiguredCatalogDoesNotInventPriceOrBreakAccountRestoration() throws Exception {
        configurations.deleteAll();
        assertThat(service.catalog(userId).available()).isFalse();
        assertThatThrownBy(() -> service.buy(userId, "not-ready", 1, 0))
                .isInstanceOf(ResponseStatusException.class);
        mvc.perform(get("/api/user/me").header("Authorization", token(userId, Role.USER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.gold").value(40));
        balances(userId, 40, 1000);
    }

    @Test
    void successfulReceiptReplaysEvenIfCatalogIsRemoved() {
        final var first = service.buy(userId, "removed", 1, 0);
        configurations.deleteAll();
        assertThat(service.buy(userId, "removed", 1, 0)).isEqualTo(first);
        balances(userId, 141, 991);
    }

    @ParameterizedTest
    @ValueSource(
            strings = {"rate", "empty", "gold", "discount", "order", "discount-order", "missing"})
    void invalidPolicyDoesNotReplaceCatalog(String kind) {
        var before = config.get();
        var rows =
                List.of(
                        new GoldPackConfigurationRequest.Pack(101, 0),
                        new GoldPackConfigurationRequest.Pack(203, 7));
        int rate = 11;
        switch (kind) {
            case "rate" -> rate = 0;
            case "empty" -> rows = List.of();
            case "gold" -> rows = List.of(new GoldPackConfigurationRequest.Pack(0, 0));
            case "discount" -> rows = List.of(new GoldPackConfigurationRequest.Pack(101, 100));
            case "order" ->
                    rows =
                            List.of(
                                    new GoldPackConfigurationRequest.Pack(101, 0),
                                    new GoldPackConfigurationRequest.Pack(101, 7));
            case "discount-order" ->
                    rows =
                            List.of(
                                    new GoldPackConfigurationRequest.Pack(101, 7),
                                    new GoldPackConfigurationRequest.Pack(203, 0));
            case "missing" -> rows = List.of(new GoldPackConfigurationRequest.Pack(null, 0));
            default -> throw new IllegalArgumentException(kind);
        }
        final var request = new GoldPackConfigurationRequest(1, rate, rows);
        assertThatThrownBy(() -> config.replace(request))
                .isInstanceOf(ResponseStatusException.class);
        assertThat(config.get()).isEqualTo(before);
    }

    @Test
    void staleAdministratorVersionAndWrongClientVersionAreRejected() {
        assertThatThrownBy(() -> config.replace(settings(0, 11, 101, 203)))
                .isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> service.buy(userId, "wrong-version", 99, 0))
                .isInstanceOf(ResponseStatusException.class);
        balances(userId, 40, 1000);
    }

    @Test
    void newTokenRestoresWalletAndOtherAccountCanUseOwnSameKey() throws Exception {
        final var first = service.buy(userId, "account-key", 1, 0);
        mvc.perform(get("/api/user/me").header("Authorization", token(userId, Role.USER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.gold").value(141))
                .andExpect(jsonPath("$.gem").value(991));
        Long other = account();
        balances(other, 40, 1000);
        service.buy(other, "account-key", 1, 0);
        balances(other, 141, 991);
        assertThat(service.buy(userId, "account-key", 1, 0)).isEqualTo(first);
        assertThat(receipts.count()).isEqualTo(2);
    }

    @Test
    void actualHttpRejectsForgedPriceGoldUserAndMissingTarget() throws Exception {
        for (String extra : List.of("\"gemPrice\":0", "\"gold\":9999", "\"userId\":1")) {
            mvc.perform(
                            post("/api/user/shop/gold-packs/buy")
                                    .header("Authorization", token(userId, Role.USER))
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(
                                            "{\"requestId\":\"forged\",\"configVersion\":1,"
                                                    + "\"index\":0,"
                                                    + extra
                                                    + "}"))
                    .andExpect(status().isBadRequest());
        }
        mvc.perform(
                        post("/api/user/shop/gold-packs/buy")
                                .header("Authorization", token(userId, Role.USER))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"requestId\":\"missing\"}"))
                .andExpect(status().isBadRequest());
        balances(userId, 40, 1000);
    }

    @Test
    void actualHttpCatalogPurchaseAndRetryUseServerPriceAndOriginalReceipt() throws Exception {
        mvc.perform(
                        get("/api/user/shop/gold-packs")
                                .header("Authorization", token(userId, Role.USER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.version").value(1))
                .andExpect(jsonPath("$.packs[0].gemPrice").value(9));
        String body = "{\"requestId\":\"http\",\"configVersion\":1,\"index\":0}";
        for (int i = 0; i < 2; i++) {
            mvc.perform(
                            post("/api/user/shop/gold-packs/buy")
                                    .header("Authorization", token(userId, Role.USER))
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(body))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.gold").value(101))
                    .andExpect(jsonPath("$.gemsSpent").value(9))
                    .andExpect(jsonPath("$.completedAt").value("2026-10-19T01:02:03.123456Z"));
        }
        balances(userId, 141, 991);
    }

    @Test
    void adminOnlyHttpCanReplaceServerCatalog() throws Exception {
        String body =
                "{\"expectedVersion\":1,\"gemGoldRate\":13,"
                        + "\"packs\":[{\"gold\":301,\"discountPercent\":0}]}";
        mvc.perform(
                        put("/api/admin/shop/gold-packs")
                                .header("Authorization", token(userId, Role.USER))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body))
                .andExpect(status().isForbidden());
        mvc.perform(
                        put("/api/admin/shop/gold-packs")
                                .header("Authorization", token(userId, Role.ADMIN))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.version").value(2))
                .andExpect(jsonPath("$.packs[0].gemPrice").value(23));
    }

    @Test
    void goldPackKeyCannotBeReusedByDailyBuyOrReroll() {
        daily();
        service.buy(userId, "cross-type", 1, 0);
        assertThatThrownBy(() -> shops.buy(userId, "cross-type", "20261019", 0, 0))
                .isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(
                        () ->
                                shops.reroll(
                                        userId,
                                        "cross-type",
                                        "20261019",
                                        0,
                                        ShopRerollRequest.Kind.GOLD))
                .isInstanceOf(ResponseStatusException.class);
        balances(userId, 141, 991);
        assertThat(shops.snapshot(userId).purchased()).isEmpty();
        assertThat(shops.snapshot(userId).goldUsed()).isZero();
    }

    @Test
    void dailyBuyKeyCannotBeReusedByGoldPack() {
        daily();
        shops.buy(userId, "daily-key", "20261019", 0, 0);
        assertThatThrownBy(() -> service.buy(userId, "daily-key", 1, 0))
                .isInstanceOf(ResponseStatusException.class);
        balances(userId, 51, 1000);
        assertThat(receipts.count()).isZero();
    }

    @Test
    void concurrentDifferentPurchaseKindsShareOneAccountKeyNamespace() throws Exception {
        daily();
        var result =
                race(
                        () -> attempt("shared", 0),
                        () -> {
                            try {
                                shops.buy(userId, "shared", "20261019", 0, 0);
                                return true;
                            } catch (ResponseStatusException e) {
                                assertThat(e.getStatusCode().value()).isEqualTo(409);
                                return false;
                            }
                        });
        assertThat(result).containsExactlyInAnyOrder(true, false);
        if (result.getFirst()) {
            balances(userId, 141, 991);
        } else {
            balances(userId, 51, 1000);
        }
        assertThat(receipts.count() + dailyReceipts.count()).isEqualTo(1);
    }

    @Test
    void maximumGoldAndIntegerPriceDoNotOverflow() {
        config.replace(
                new GoldPackConfigurationRequest(
                        1,
                        1,
                        List.of(new GoldPackConfigurationRequest.Pack(Integer.MAX_VALUE, 0))));
        setWallet("gold", 0);
        setWallet("gem", Integer.MAX_VALUE);
        var receipt = service.buy(userId, "max", 2, 0);
        assertThat(receipt.gemsSpent()).isEqualTo(Integer.MAX_VALUE);
        balances(userId, Integer.MAX_VALUE, 0);
    }

    @Test
    void accountDeletionCleansOnlyItsReceiptAndPreservesOtherAccount() {
        service.buy(userId, "delete", 1, 0);
        Long other = account();
        service.buy(other, "delete", 1, 0);
        users.deleteAccount(userId);
        assertThat(receipts.count()).isEqualTo(1);
        assertThatThrownBy(() -> service.buy(userId, "delete", 1, 0))
                .isInstanceOf(ResponseStatusException.class);
        balances(other, 141, 991);
    }

    private void daily() {
        Long reward =
                rewards.create(
                                new RewardDefinitionRequest(
                                        "goldpack daily fixture", 11, Map.of(), null))
                        .id();
        dailyConfig.replace(
                new ShopConfigurationRequest(
                        0,
                        1,
                        1,
                        7,
                        0,
                        List.of(0),
                        List.of(
                                new ShopConfigurationRequest.Offer(
                                        "goldpack.daily", 3, 0, reward))));
        shops.snapshot(userId);
    }

    private boolean attempt(String key, int index) {
        try {
            service.buy(userId, key, 1, index);
            return true;
        } catch (ResponseStatusException e) {
            assertThat(e.getStatusCode().value()).isEqualTo(409);
            return false;
        }
    }

    private void time(String text) {
        when(clock.instant()).thenReturn(Instant.parse(text));
    }

    private void setWallet(String field, int amount) {
        tx.executeWithoutResult(
                s -> {
                    var w = wallets.findByUserId(userId).orElseThrow();
                    ReflectionTestUtils.setField(w, field, amount);
                    wallets.saveAndFlush(w);
                });
    }

    private void balances(Long id, int gold, int gems) {
        var w = wallets.findByUserId(id).orElseThrow();
        assertThat(w.getGold()).isEqualTo(gold);
        assertThat(w.getGem()).isEqualTo(gems);
    }

    private String token(Long id, Role role) {
        return "Bearer "
                + tokens.createAccessToken(
                        id, role == Role.ADMIN ? ClientType.WEB : ClientType.GAME, role);
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
