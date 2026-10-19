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
import com.novaserver.auth.repository.RefreshTokenRepository;
import com.novaserver.auth.repository.TokenBlocklistRepository;
import com.novaserver.auth.service.AuthService;
import com.novaserver.auth.social.SocialTokenVerifier;
import com.novaserver.battle.repository.BattleRepository;
import com.novaserver.battle.service.BattleService;
import com.novaserver.item.entity.Item;
import com.novaserver.item.repository.ItemRepository;
import com.novaserver.quest.dto.QuestChestRequest;
import com.novaserver.quest.dto.QuestConfigRequest;
import com.novaserver.quest.dto.QuestReceiptResponse;
import com.novaserver.quest.entity.QuestKind;
import com.novaserver.quest.entity.QuestPeriod;
import com.novaserver.quest.repository.QuestChestRepository;
import com.novaserver.quest.repository.QuestDefinitionRepository;
import com.novaserver.quest.repository.UserQuestClaimRepository;
import com.novaserver.quest.repository.UserQuestPeriodRepository;
import com.novaserver.quest.service.QuestConfigService;
import com.novaserver.quest.service.UserQuestService;
import com.novaserver.reward.dto.RewardDefinitionRequest;
import com.novaserver.reward.repository.RewardDefinitionRepository;
import com.novaserver.reward.service.RewardDefinitionService;
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
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
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
class QuestAccountTransactionTest {
    private static final String ITEM = "quest.fixture.item";
    private static final String LOGIN = "daily.fixture.login";
    private static final String ENERGY = "daily.fixture.energy";
    private static final String CHEST = "daily.fixture.chest";
    private static final String KEY = "20261019";
    @Autowired private UserQuestService service;
    @Autowired private QuestConfigService config;
    @Autowired private QuestDefinitionRepository quests;
    @Autowired private QuestChestRepository chests;
    @Autowired private UserQuestPeriodRepository periods;
    @MockitoSpyBean private UserQuestClaimRepository claims;
    @Autowired private UserService users;
    @Autowired private AuthService authService;
    @Autowired private BattleService battles;
    @Autowired private BattleRepository sessions;
    @Autowired private UserRepository accounts;
    @Autowired private WalletRepository wallets;
    @Autowired private UserEnergyRepository energies;
    @Autowired private ItemRepository items;
    @Autowired private UserItemRepository inventory;
    @Autowired private RewardDefinitionService rewards;
    @Autowired private RewardDefinitionRepository definitions;
    @Autowired private PlatformTransactionManager transactions;
    @Autowired private JwtProvider tokens;
    @Autowired private MockMvc mvc;
    @MockitoBean private Clock clock;
    @MockitoBean private TokenBlocklistRepository blocklist;
    @MockitoBean private RefreshTokenRepository refreshTokens;
    @MockitoBean private SocialTokenVerifier socialVerifier;
    @MockitoSpyBean private UserQuestPeriodRepository periodWrites;
    private TransactionTemplate tx;
    private Long userId;
    private Long rewardId;
    private final List<Long> userIds = new ArrayList<>();

    @BeforeEach
    void setUp() {
        tx = new TransactionTemplate(transactions);
        time("2026-10-19T01:02:03.123456Z");
        items.saveAndFlush(new Item(ITEM, null));
        rewardId =
                rewards.create(
                                new RewardDefinitionRequest(
                                        "quest fixture", 13, Map.of(ITEM, 2), null))
                        .id();
        config.saveQuest(LOGIN, new QuestConfigRequest(QuestPeriod.DAILY, QuestKind.LOGIN, 1, 17));
        config.saveQuest(
                ENERGY, new QuestConfigRequest(QuestPeriod.DAILY, QuestKind.SPEND_ENERGY, 10, 23));
        config.saveQuest(
                "weekly.fixture.login",
                new QuestConfigRequest(QuestPeriod.WEEKLY, QuestKind.LOGIN, 2, 31));
        config.saveQuest(
                "weekly.fixture.energy",
                new QuestConfigRequest(QuestPeriod.WEEKLY, QuestKind.SPEND_ENERGY, 15, 29));
        config.saveChest(CHEST, new QuestChestRequest(QuestPeriod.DAILY, 17, rewardId, 5, 7));
        config.saveChest(
                "weekly.fixture.chest",
                new QuestChestRequest(QuestPeriod.WEEKLY, 31, rewardId, 11, 19));
        userId = account();
    }

    private Long account() {
        return tx.execute(
                s -> {
                    var u =
                            accounts.saveAndFlush(
                                    new User(Provider.GUEST, UUID.randomUUID().toString()));
                    userIds.add(u.getId());
                    users.createInitialState(u);
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
        chests.deleteAll();
        quests.deleteAll();
        definitions.deleteAll();
        items.deleteById(ITEM);
    }

    @Test
    void actualGuestAuthenticationCountsLoginOnceAcrossRepeatedTokens() {
        var signed = authService.guestSignUp();
        Long id = tokens.parse(signed.getTokens().getAccessToken()).userId();
        userIds.add(id);
        authService.guestLogIn(signed.getGuestId());
        authService.guestLogIn(signed.getGuestId());
        assertThat(service.snapshot(id).getFirst().loginCount()).isEqualTo(1);
        assertThat(service.snapshot(id).get(1).loginCount()).isEqualTo(1);
    }

    @Test
    void verifiedSocialGameLoginCountsButWebAuthenticationDoesNot() {
        when(socialVerifier.verify(Provider.GOOGLE, "fixture-token")).thenReturn("quest-social");
        var web = authService.socialLogIn("fixture-token", Provider.GOOGLE, ClientType.WEB);
        Long id = tokens.parse(web.getAccessToken()).userId();
        userIds.add(id);
        assertThat(service.snapshot(id).getFirst().loginCount()).isZero();
        authService.socialLogIn("fixture-token", Provider.GOOGLE, ClientType.GAME);
        assertThat(service.snapshot(id).getFirst().loginCount()).isEqualTo(1);
    }

    @Test
    void actualBattleSpendAndRetryCountOnlyActualEnergyDebit() {
        var first = battles.createBattle(userId, "fixture-battle-1");
        assertThat(battles.createBattle(userId, "fixture-battle-1").getBattleId())
                .isEqualTo(first.getBattleId());
        battles.cancelBattle(userId, first.getBattleId());
        assertThat(service.snapshot(userId).getFirst().spentEnergy()).isEqualTo(5);
        assertThat(service.snapshot(userId).get(1).spentEnergy()).isEqualTo(5);
        assertThat(energies.findByUserId(userId).orElseThrow().getEnergyStored()).isEqualTo(25);
    }

    @Test
    void concurrentBattleRetriesDebitAndCountOnce() throws Exception {
        var results =
                race(
                        () -> battles.createBattle(userId, "same-key").getBattleId(),
                        () -> battles.createBattle(userId, "same-key").getBattleId());
        assertThat(results.getFirst()).isEqualTo(results.get(1));
        assertThat(service.snapshot(userId).getFirst().spentEnergy()).isEqualTo(5);
    }

    @Test
    void failedBattleLeavesNoQuestProgress() {
        setEnergy(0);
        assertThatThrownBy(() -> battles.createBattle(userId, "no-energy"))
                .isInstanceOf(ResponseStatusException.class);
        assertThat(service.snapshot(userId).getFirst().spentEnergy()).isZero();
        assertThat(sessions.existsById(userId)).isFalse();
    }

    @Test
    void questStateWriteFailureRollsBackBattleIssuanceAndEnergy() {
        doThrow(new DataIntegrityViolationException("fixture period failure"))
                .when(periodWrites)
                .saveAndFlush(any());
        assertThatThrownBy(() -> battles.createBattle(userId, "failure"))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThat(energies.findByUserId(userId).orElseThrow().getEnergyStored()).isEqualTo(30);
        assertThat(sessions.existsById(userId)).isFalse();
    }

    @Test
    void energyRewardsAndProfileReadsDoNotCountAsBattleSpendOrLogin() {
        users.grantEnergy(userId, 3);
        users.getMe(userId);
        var board = service.snapshot(userId).getFirst();
        assertThat(board.loginCount()).isZero();
        assertThat(board.spentEnergy()).isZero();
    }

    @Test
    void dailyKst04BoundaryResetsOnlyDailyAndCountsWeeklyLoginOncePerDay() {
        time("2026-10-19T18:59:59.999999Z");
        service.recordLogin(userId);
        battles.createBattle(userId, "before-reset");
        assertThat(service.snapshot(userId).getFirst().periodKey()).isEqualTo(KEY);
        time("2026-10-19T19:00:00Z");
        var next = service.snapshot(userId);
        assertThat(next.getFirst().periodKey()).isEqualTo("20261020");
        assertThat(next.getFirst().spentEnergy()).isZero();
        assertThat(next.get(1).spentEnergy()).isEqualTo(5);
        service.recordLogin(userId);
        service.recordLogin(userId);
        assertThat(service.snapshot(userId).get(1).loginCount()).isEqualTo(2);
    }

    @Test
    void mondayKst04ResetsBothBoardsAndExposesUtcNextReset() {
        time("2026-10-18T18:59:59.999999Z");
        service.recordLogin(userId);
        var prior = service.snapshot(userId);
        assertThat(prior.getFirst().periodKey()).isEqualTo("20261018");
        assertThat(prior.get(1).periodKey()).isEqualTo("20261012");
        assertThat(prior.get(1).nextResetAt()).isEqualTo(Instant.parse("2026-10-18T19:00:00Z"));
        time("2026-10-18T19:00:00Z");
        var next = service.snapshot(userId);
        assertThat(next)
                .allSatisfy(
                        b -> {
                            assertThat(b.periodKey()).isEqualTo(KEY);
                            assertThat(b.loginCount()).isZero();
                            assertThat(b.points()).isZero();
                        });
        assertThat(next.get(1).nextResetAt()).isEqualTo(Instant.parse("2026-10-25T19:00:00Z"));
    }

    @Test
    void incompleteQuestAndCompletedButUnclaimedPointsCannotOpenChest() {
        assertThatThrownBy(() -> claim(LOGIN, false)).isInstanceOf(ResponseStatusException.class);
        service.recordLogin(userId);
        assertThatThrownBy(() -> claim(CHEST, true)).isInstanceOf(ResponseStatusException.class);
        assertThat(wallets.findByUserId(userId).orElseThrow().getGold()).isZero();
        assertThat(service.snapshot(userId).getFirst().points()).isZero();
    }

    @Test
    void firstQuestClaimAddsOnlyPointsAndConcurrentRetriesReturnSameReceipt() throws Exception {
        service.recordLogin(userId);
        var result = race(() -> claim(LOGIN, false), () -> claim(LOGIN, false));
        assertThat(result.getFirst()).isEqualTo(result.get(1));
        assertThat(service.snapshot(userId).getFirst().points()).isEqualTo(17);
        assertThat(wallets.findByUserId(userId).orElseThrow().getGold()).isZero();
        assertThat(claims.count()).isEqualTo(1);
    }

    @Test
    void concurrentChestClaimsPayCommonGoldItemsEnergyGemsOnlyOnce() throws Exception {
        unlock();
        var result = race(() -> claim(CHEST, true), () -> claim(CHEST, true));
        assertThat(result.getFirst()).isEqualTo(result.get(1));
        assertThat(result.getFirst().items()).containsEntry(ITEM, 2);
        assertBalances(13, 7, 35, 2);
        assertThat(service.snapshot(userId).getFirst().chests().getFirst().claimed()).isTrue();
    }

    @Test
    void battleEntryAndChestClaimConcurrentKeepBothChanges() throws Exception {
        unlock();
        race(
                () -> {
                    battles.createBattle(userId, "entry");
                    return true;
                },
                () -> {
                    claim(CHEST, true);
                    return true;
                });
        assertBalances(13, 7, 30, 2);
        assertThat(service.snapshot(userId).getFirst().spentEnergy()).isEqualTo(5);
    }

    @Test
    void boundaryRetryRestoresOldReceiptWithoutClaimingNewPeriod() {
        unlock();
        var receipt = claim(CHEST, true);
        time("2026-10-19T19:00:00Z");
        assertThat(claim(CHEST, true)).isEqualTo(receipt);
        assertThat(service.snapshot(userId).getFirst().points()).isZero();
        assertThat(service.snapshot(userId).getFirst().chests().getFirst().claimed()).isFalse();
        assertThatThrownBy(() -> service.claim(userId, CHEST, QuestPeriod.DAILY, "20261020", true))
                .isInstanceOf(ResponseStatusException.class);
        assertBalances(13, 7, 35, 2);
    }

    @Test
    void oldUnclaimedRequestAndFutureOrInvalidPeriodNeverPay() {
        unlock();
        time("2026-10-19T19:00:00Z");
        assertThatThrownBy(() -> claim(CHEST, true)).isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> service.claim(userId, CHEST, QuestPeriod.DAILY, "20261021", true))
                .isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> service.claim(userId, CHEST, QuestPeriod.DAILY, "20260230", true))
                .isInstanceOf(ResponseStatusException.class);
        assertBalances(0, 0, 30, 0);
    }

    @Test
    void weeklyLoginTargetAndChestPersistUntilMonday() {
        service.recordLogin(userId);
        assertThatThrownBy(
                        () ->
                                service.claim(
                                        userId,
                                        "weekly.fixture.login",
                                        QuestPeriod.WEEKLY,
                                        KEY,
                                        false))
                .isInstanceOf(ResponseStatusException.class);
        time("2026-10-19T19:00:00Z");
        service.recordLogin(userId);
        service.claim(userId, "weekly.fixture.login", QuestPeriod.WEEKLY, KEY, false);
        var receipt = service.claim(userId, "weekly.fixture.chest", QuestPeriod.WEEKLY, KEY, true);
        assertThat(receipt.points()).isZero();
        assertBalances(13, 19, 41, 2);
        assertThat(service.snapshot(userId).get(1).points()).isEqualTo(31);
    }

    @ParameterizedTest
    @ValueSource(strings = {"gold", "gems", "energy", "items"})
    void rewardOverflowRollsBackAllPayoutAndClaimState(String kind) {
        unlock();
        tx.executeWithoutResult(
                s -> {
                    if (kind.equals("gold") || kind.equals("gems")) {
                        var wallet = wallets.findByUserId(userId).orElseThrow();
                        ReflectionTestUtils.setField(
                                wallet, kind.equals("gems") ? "gem" : "gold", Integer.MAX_VALUE);
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
        assertThatThrownBy(() -> claim(CHEST, true)).isInstanceOf(ResponseStatusException.class);
        assertThat(service.snapshot(userId).getFirst().chests().getFirst().claimed()).isFalse();
        assertBalances(
                kind.equals("gold") ? Integer.MAX_VALUE : 0,
                kind.equals("gems") ? Integer.MAX_VALUE : 0,
                kind.equals("energy") ? Integer.MAX_VALUE : 30,
                kind.equals("items") ? Integer.MAX_VALUE : 0);
    }

    @Test
    void receiptWriteFailureRollsBackEveryRewardAndClaimState() {
        unlock();
        doThrow(new DataIntegrityViolationException("fixture receipt failure"))
                .when(claims)
                .saveAndFlush(any());
        assertThatThrownBy(() -> claim(CHEST, true))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertBalances(0, 0, 30, 0);
        assertThat(service.snapshot(userId).getFirst().chests().getFirst().claimed()).isFalse();
    }

    @Test
    void originalReceiptSurvivesCommonRewardEditAndLaterWalletChanges() {
        unlock();
        var receipt = claim(CHEST, true);
        rewards.update(
                rewardId,
                new RewardDefinitionRequest(
                        "edited fixture", 29, Map.of(ITEM, 4), rewards.get(rewardId).version()));
        tx.executeWithoutResult(s -> wallets.findByUserId(userId).orElseThrow().addGold(3));
        assertThat(claim(CHEST, true)).isEqualTo(receipt);
        assertBalances(16, 7, 35, 2);
        time("2026-10-19T19:00:00Z");
        service.recordLogin(userId);
        service.claim(userId, LOGIN, QuestPeriod.DAILY, "20261020", false);
        assertThat(service.claim(userId, CHEST, QuestPeriod.DAILY, "20261020", true).gold())
                .isEqualTo(29);
        assertBalances(45, 14, 40, 6);
    }

    @Test
    void sharedBundleCannotBeDeletedAndUsedDefinitionsRequireNewId() {
        unlock();
        claim(CHEST, true);
        assertThat(rewards.get(rewardId).questChestReferences()).isEqualTo(2);
        assertThatThrownBy(() -> rewards.delete(rewardId))
                .isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> config.deleteQuest(LOGIN))
                .isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(
                        () ->
                                config.saveQuest(
                                        LOGIN,
                                        new QuestConfigRequest(
                                                QuestPeriod.DAILY, QuestKind.LOGIN, 1, 99)))
                .isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> config.deleteChest(CHEST))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void newAuthenticatedTokenRestoresMeAndOtherAccountIsIsolated() throws Exception {
        unlock();
        claim(CHEST, true);
        mvc.perform(get("/api/user/me").header("Authorization", token(userId, Role.USER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.gold").value(13))
                .andExpect(jsonPath("$.quests[0].points").value(17))
                .andExpect(jsonPath("$.quests[0].chests[0].claimed").value(true));
        Long other = account();
        mvc.perform(get("/api/user/quests").header("Authorization", token(other, Role.USER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].points").value(0))
                .andExpect(jsonPath("$[0].loginCount").value(0))
                .andExpect(jsonPath("$[0].chests[0].claimed").value(false));
        assertThatThrownBy(() -> service.claim(other, CHEST, QuestPeriod.DAILY, KEY, true))
                .isInstanceOf(ResponseStatusException.class);
        assertThat(wallets.findByUserId(other).orElseThrow().getGold()).isZero();
    }

    @Test
    void clientForgedCountsRewardsPeriodAndAdminActionsAreRejected() throws Exception {
        for (String extra : List.of("\"count\":100", "\"gold\":99", "\"claimed\":true")) {
            mvc.perform(
                            post("/api/user/quests/claim")
                                    .header("Authorization", token(userId, Role.USER))
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(
                                            "{\"id\":\""
                                                    + LOGIN
                                                    + "\",\"period\":\"DAILY\",\"periodKey\":\""
                                                    + KEY
                                                    + "\","
                                                    + extra
                                                    + "}"))
                    .andExpect(status().isBadRequest());
        }
        mvc.perform(
                        put("/api/admin/quests/forged")
                                .header("Authorization", token(userId, Role.USER))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        "{\"period\":\"DAILY\",\"kind\":\"LOGIN\","
                                                + "\"target\":1,\"points\":1}"))
                .andExpect(status().isForbidden());
        mvc.perform(
                        put("/api/admin/quests/kill")
                                .header("Authorization", token(userId, Role.ADMIN))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        "{\"period\":\"DAILY\",\"kind\":\"KILL_MONSTERS\","
                                                + "\"target\":1,\"points\":1}"))
                .andExpect(status().isOk());
    }

    @Test
    void actualHttpClaimRequiresPeriodAndPreservesUtcReceiptOnRetry() throws Exception {
        unlock();
        String body =
                "{\"id\":\"" + CHEST + "\",\"period\":\"DAILY\",\"periodKey\":\"" + KEY + "\"}";
        for (int i = 0; i < 2; i++) {
            mvc.perform(
                            post("/api/user/quests/chests/claim")
                                    .header("Authorization", token(userId, Role.USER))
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(body))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.gold").value(13))
                    .andExpect(jsonPath("$.claimedAt").value("2026-10-19T01:02:03.123456Z"));
        }
        assertBalances(13, 7, 35, 2);
    }

    @Test
    void largePointTotalsDoNotOverflowAndProgressIsClampedToTarget() {
        config.saveQuest(
                "daily.large",
                new QuestConfigRequest(QuestPeriod.DAILY, QuestKind.LOGIN, 1, Integer.MAX_VALUE));
        service.recordLogin(userId);
        claim(LOGIN, false);
        claim("daily.large", false);
        assertThat(service.snapshot(userId).getFirst().points()).isEqualTo(2147483664L);
        battles.createBattle(userId, "energy-1");
        battles.createBattle(userId, "energy-2");
        battles.createBattle(userId, "energy-3");
        assertThat(
                        service.snapshot(userId).getFirst().quests().stream()
                                .filter(q -> q.id().equals(ENERGY))
                                .findFirst()
                                .orElseThrow()
                                .count())
                .isEqualTo(10);
    }

    @Test
    void accountDeletionRemovesAllQuestPeriodsAndReceipts() {
        unlock();
        claim(CHEST, true);
        users.deleteAccount(userId);
        assertThat(periods.count()).isZero();
        assertThat(claims.count()).isZero();
        assertThatThrownBy(() -> service.snapshot(userId))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void concurrentAuthenticatedLoginsAfterResetCountNewDayOnce() throws Exception {
        var signed = authService.guestSignUp();
        Long id = tokens.parse(signed.getTokens().getAccessToken()).userId();
        userIds.add(id);
        time("2026-10-19T19:00:00Z");
        race(
                () -> authService.guestLogIn(signed.getGuestId()).getAccessToken(),
                () -> authService.guestLogIn(signed.getGuestId()).getAccessToken());
        assertThat(service.snapshot(id).getFirst().loginCount()).isEqualTo(1);
        assertThat(service.snapshot(id).get(1).loginCount()).isEqualTo(2);
    }

    @Test
    void claimedPointsAndChestDeleteRaceHasOnlySuccessfulClaimOrNotFound() throws Exception {
        unlock();
        var results =
                race(
                        () -> {
                            try {
                                claim(CHEST, true);
                                return true;
                            } catch (ResponseStatusException e) {
                                assertThat(e.getStatusCode().value()).isEqualTo(404);
                                return false;
                            }
                        },
                        () -> {
                            try {
                                config.deleteChest(CHEST);
                                return true;
                            } catch (ResponseStatusException e) {
                                assertThat(e.getStatusCode().value()).isEqualTo(409);
                                return false;
                            }
                        });
        assertThat(results.getFirst()).isNotEqualTo(results.get(1));
        assertBalances(
                results.getFirst() ? 13 : 0,
                results.getFirst() ? 7 : 0,
                results.getFirst() ? 35 : 30,
                results.getFirst() ? 2 : 0);
    }

    @Test
    void rewardEditConcurrentWithClaimProducesOneConsistentSnapshot() throws Exception {
        unlock();
        var results =
                race(
                        () -> claim(CHEST, true),
                        () -> {
                            rewards.update(
                                    rewardId,
                                    new RewardDefinitionRequest(
                                            "concurrent fixture",
                                            29,
                                            Map.of(ITEM, 4),
                                            rewards.get(rewardId).version()));
                            return claim(CHEST, true);
                        });
        var receipt = results.getFirst();
        assertThat(receipt.gold()).isIn(13, 29);
        assertThat(receipt.items()).containsEntry(ITEM, receipt.gold() == 13 ? 2 : 4);
        assertThat(claim(CHEST, true)).isEqualTo(receipt);
    }

    @Test
    void adminHttpRegistersExplicitDefinitionsAndRejectsUnreachableLoginTarget() throws Exception {
        mvc.perform(
                        put("/api/admin/quests/daily.fixture.http")
                                .header("Authorization", token(userId, Role.ADMIN))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        "{\"period\":\"DAILY\",\"kind\":\"SPEND_ENERGY\","
                                                + "\"target\":5,\"points\":7}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.target").value(5));
        assertThatThrownBy(
                        () ->
                                config.saveQuest(
                                        "daily.unreachable",
                                        new QuestConfigRequest(
                                                QuestPeriod.DAILY, QuestKind.LOGIN, 2, 7)))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void meProjectsNaturalRecoveryWithoutWritingUnlockedEnergyState() {
        tx.executeWithoutResult(
                status -> {
                    var energy = energies.findByUserId(userId).orElseThrow();
                    ReflectionTestUtils.setField(energy, "energyStored", 5);
                    ReflectionTestUtils.setField(
                            energy,
                            "energyUpdatedAt",
                            Instant.parse("2026-10-19T00:42:03.123456Z"));
                    energies.saveAndFlush(energy);
                });
        assertThat(users.getMe(userId).getEnergyStored()).isEqualTo(7);
        var stored = energies.findByUserId(userId).orElseThrow();
        assertThat(stored.getEnergyStored()).isEqualTo(5);
        assertThat(stored.getEnergyUpdatedAt())
                .isEqualTo(Instant.parse("2026-10-19T00:42:03.123456Z"));
    }

    @Test
    void nonCanonicalIdCannotCreateUnrestorableCaseVariantReceipt() {
        service.recordLogin(userId);
        assertThatThrownBy(() -> claim(LOGIN.toUpperCase(java.util.Locale.ROOT), false))
                .isInstanceOf(ResponseStatusException.class);
        claim(LOGIN, false);
        assertThatThrownBy(() -> claim(CHEST.toUpperCase(java.util.Locale.ROOT), true))
                .isInstanceOf(ResponseStatusException.class);
        assertBalances(0, 0, 30, 0);
        assertThat(service.snapshot(userId).getFirst().chests().getFirst().claimed()).isFalse();
    }

    @ParameterizedTest
    @CsvSource({
        "2026-10-19T18:59:59.999999Z,20261019,20261019",
        "2026-10-18T18:59:59.999999Z,20261018,20261012"
    })
    void battleIssueUsesOneDebitTimestampAcrossDailyAndWeeklyReset(
            String beforeText, String dailyKey, String weeklyKey) {
        Instant before = Instant.parse(beforeText);
        Instant after = before.plusSeconds(1);
        when(clock.instant()).thenReturn(before, after);
        final var issued = battles.createBattle(userId, "reset-crossing");
        time(after.toString());
        assertThat(energies.findByUserId(userId).orElseThrow().getEnergyUpdatedAt())
                .isEqualTo(before);
        assertThat(sessions.findById(userId).orElseThrow().getCreatedAt()).isEqualTo(before);
        assertThat(
                        periods.findByUserIdAndPeriodAndPeriodDay(
                                        userId,
                                        QuestPeriod.DAILY,
                                        LocalDate.parse(dailyKey, DateTimeFormatter.BASIC_ISO_DATE))
                                .orElseThrow()
                                .getSpentEnergy())
                .isEqualTo(5);
        assertThat(
                        periods.findByUserIdAndPeriodAndPeriodDay(
                                        userId,
                                        QuestPeriod.WEEKLY,
                                        LocalDate.parse(
                                                weeklyKey, DateTimeFormatter.BASIC_ISO_DATE))
                                .orElseThrow()
                                .getSpentEnergy())
                .isEqualTo(5);
        assertThat(service.snapshot(userId).getFirst().spentEnergy()).isZero();
        assertThat(battles.createBattle(userId, "reset-crossing").getBattleId())
                .isEqualTo(issued.getBattleId());
        assertThat(service.snapshot(userId).getFirst().spentEnergy()).isZero();
    }

    private void unlock() {
        service.recordLogin(userId);
        claim(LOGIN, false);
    }

    private QuestReceiptResponse claim(String id, boolean chest) {
        return service.claim(userId, id, QuestPeriod.DAILY, KEY, chest);
    }

    private void time(String now) {
        when(clock.instant()).thenReturn(Instant.parse(now));
    }

    private String token(Long id, Role role) {
        return "Bearer "
                + tokens.createAccessToken(
                        id, role == Role.ADMIN ? ClientType.WEB : ClientType.GAME, role);
    }

    private void setEnergy(int amount) {
        tx.executeWithoutResult(
                s -> {
                    var e = energies.findByUserId(userId).orElseThrow();
                    ReflectionTestUtils.setField(e, "energyStored", amount);
                    energies.saveAndFlush(e);
                });
    }

    private void assertBalances(int gold, int gems, int energy, int amount) {
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
