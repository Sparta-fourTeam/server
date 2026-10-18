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
import com.novaserver.playerlevel.dto.PlayerLevelConfigRequest;
import com.novaserver.playerlevel.entity.PlayerLevelConfig;
import com.novaserver.playerlevel.repository.PlayerLevelConfigRepository;
import com.novaserver.playerlevel.service.PlayerLevelService;
import com.novaserver.reward.dto.RewardDefinitionRequest;
import com.novaserver.reward.repository.RewardDefinitionRepository;
import com.novaserver.reward.service.RewardDefinitionService;
import com.novaserver.stage.entity.Stage;
import com.novaserver.stage.repository.StageRepository;
import com.novaserver.stage.service.StageService;
import com.novaserver.tutorial.dto.TutorialConfigRequest;
import com.novaserver.tutorial.repository.TutorialRepository;
import com.novaserver.tutorial.service.TutorialConfigService;
import com.novaserver.tutorial.service.UserTutorialService;
import com.novaserver.upgrade.dto.UpgradeRequest;
import com.novaserver.upgrade.entity.Upgrade;
import com.novaserver.upgrade.repository.UpgradeRepository;
import com.novaserver.upgrade.service.UpgradeService;
import com.novaserver.user.Provider;
import com.novaserver.user.Role;
import com.novaserver.user.entity.User;
import com.novaserver.user.entity.UserItem;
import com.novaserver.user.entity.UserStageProgress;
import com.novaserver.user.entity.UserUpgrade;
import com.novaserver.user.repository.UserDataRepository;
import com.novaserver.user.repository.UserItemRepository;
import com.novaserver.user.repository.UserRepository;
import com.novaserver.user.repository.UserStageProgressRepository;
import com.novaserver.user.repository.UserTutorialRepository;
import com.novaserver.user.repository.UserUpgradeRepository;
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
class TutorialAccountTransactionTest {
    private static final int STAGE = 2861;
    private static final int START = 2862;
    private static final String ITEM = "tutorial.fixture.item";
    private static final String UPGRADE = "tutorial.fixture.upgrade";
    private static final String INTRO = "fixture_intro";
    @Autowired private UserTutorialService service;
    @Autowired private TutorialConfigService config;
    @Autowired private TutorialRepository tutorials;
    @Autowired private UserService users;
    @Autowired private UserRepository accounts;
    @Autowired private UserDataRepository data;
    @Autowired private WalletRepository wallets;
    @Autowired private UserStageProgressRepository progress;
    @Autowired private StageRepository stages;
    @Autowired private StageService stageService;
    @Autowired private UpgradeRepository upgrades;
    @Autowired private UpgradeService upgradeService;
    @Autowired private UserUpgradeRepository userUpgrades;
    @Autowired private UserItemRepository inventory;
    @Autowired private ItemRepository items;
    @Autowired private PlayerLevelConfigRepository levels;
    @Autowired private PlayerLevelService levelService;
    @Autowired private RewardDefinitionRepository definitions;
    @Autowired private RewardDefinitionService rewards;
    @Autowired private PlatformTransactionManager transactions;
    @Autowired private MockMvc mvc;
    @Autowired private JwtProvider tokens;
    @MockitoSpyBean private UserTutorialRepository states;
    @MockitoBean private Clock clock;
    @MockitoBean private TokenBlocklistRepository blocklist;
    private TransactionTemplate tx;
    private Long userId;
    private Long rewardId;
    private final List<Long> userIds = new ArrayList<>();

    @BeforeEach
    void setUp() {
        tx = new TransactionTemplate(transactions);
        when(clock.instant()).thenReturn(Instant.parse("2026-10-19T01:02:03.123456Z"));
        tx.executeWithoutResult(
                status -> {
                    stages.saveAll(List.of(Stage.create(STAGE), Stage.create(START)));
                    items.saveAndFlush(new Item(ITEM, null));
                    upgrades.saveAndFlush(
                            new Upgrade(UPGRADE, 4, 1, null, 1, 0, null, null, null, null, null));
                    levels.saveAndFlush(new PlayerLevelConfig(31, 7, 8));
                });
        rewardId =
                rewards.create(
                                new RewardDefinitionRequest(
                                        "tutorial fixture", 13, Map.of(ITEM, 2), null))
                        .id();
        config.save(INTRO, request(null, 1, null, STAGE, null, 0, 0, 2, rewardId));
        userId = account();
    }

    private Long account() {
        return tx.execute(
                status -> {
                    var user =
                            accounts.saveAndFlush(
                                    new User(Provider.GUEST, UUID.randomUUID().toString()));
                    userIds.add(user.getId());
                    users.createInitialState(user);
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
                    var rows = tutorials.findAll();
                    rows.forEach(
                            t ->
                                    t.configure(
                                            null,
                                            t.getMinLevel(),
                                            t.getStartStage(),
                                            t.getClearStage(),
                                            t.getRequiredUpgrade(),
                                            t.getUpgradeLevel(),
                                            t.getPlayerLevel(),
                                            t.getSetLevel(),
                                            t.getRewardDefinition()));
                    tutorials.flush();
                    tutorials.deleteAll(rows);
                    tutorials.flush();
                    definitions.deleteAll(definitions.findAll());
                    definitions.flush();
                    upgrades.deleteById(UPGRADE);
                    items.deleteById(ITEM);
                    stages.deleteAllById(List.of(STAGE, START));
                    levels.deleteAll();
                });
    }

    @Test
    void dialogueHasNoRewardOrCompletionAndRestoresWithAnotherToken() throws Exception {
        var first = service.complete(userId, INTRO + ".dialogue");
        when(clock.instant()).thenReturn(Instant.parse("2026-10-19T02:00:00Z"));
        assertThat(service.complete(userId, INTRO + ".dialogue")).isEqualTo(first);
        assertThat(first.gold()).isZero();
        assertThat(first.items()).isEmpty();
        assertThat(first.dialogueOnly()).isTrue();
        assertThatThrownBy(() -> service.complete(userId, INTRO))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("완료 조건");
        assertThatThrownBy(() -> service.complete(userId, "unknown.dialogue"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("없습니다");
        mvc.perform(get("/api/user/me").header("Authorization", auth(userId, Role.USER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tutorials[0]").value(INTRO + ".dialogue"))
                .andExpect(jsonPath("$.tutorialStates[0].completed").value(false))
                .andExpect(jsonPath("$.tutorialStates[0].rewardClaimed").value(false));
        assertState(0, 0, 0);
    }

    @Test
    void completionPersistsExactReceiptAfterDefinitionAndBalanceChanges() throws Exception {
        clear(userId, STAGE);
        tx.executeWithoutResult(s -> data.findByUserIdForUpdate(userId).orElseThrow().addExp(90));
        var first = service.complete(userId, INTRO);
        assertThat(first.expBefore()).isEqualTo(90);
        assertThat(first.expAfter()).isEqualTo(31);
        assertThat(first.gold()).isEqualTo(13);
        assertThat(first.items()).containsExactlyEntriesOf(Map.of(ITEM, 2));
        assertState(13, 2, 31);
        var changed =
                rewards.update(
                        rewardId, new RewardDefinitionRequest("edited", 37, Map.of(ITEM, 7), 1));
        assertThat(changed.tutorialReferences()).isEqualTo(1);
        Long empty = rewards.create(new RewardDefinitionRequest("empty", 0, Map.of(), null)).id();
        config.save(INTRO, request(null, 1, null, STAGE, null, 0, 0, 0, empty));
        rewards.delete(rewardId);
        levels.deleteAll();
        tx.executeWithoutResult(s -> data.findByUserIdForUpdate(userId).orElseThrow().addExp(100));
        assertThat(service.complete(userId, INTRO)).isEqualTo(first);
        assertState(13, 2, 131);
        mvc.perform(get("/api/user/me").header("Authorization", auth(userId, Role.USER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tutorials[0]").value(INTRO))
                .andExpect(jsonPath("$.tutorialStates[0].completed").value(true))
                .andExpect(jsonPath("$.tutorialStates[0].rewardClaimed").value(true));
        Long other = account();
        assertThat(service.list(other)).isEmpty();
        assertThat(wallets.findByUserId(other).orElseThrow().getGold()).isZero();
    }

    @ParameterizedTest
    @ValueSource(strings = {"prerequisite", "start-stage", "min-level", "upgrade", "player-level"})
    void completionUsesStoredConditionsInsteadOfClientProgress(String condition) {
        Long empty = rewards.create(new RewardDefinitionRequest("empty", 0, Map.of(), null)).id();
        String next = "fixture_next";
        config.save(
                next,
                request(
                        condition.equals("prerequisite") ? INTRO : null,
                        condition.equals("min-level") ? 3 : 1,
                        condition.equals("start-stage") ? START : null,
                        condition.equals("upgrade") || condition.equals("player-level")
                                ? null
                                : STAGE,
                        condition.equals("upgrade") ? UPGRADE : null,
                        condition.equals("upgrade") ? 2 : 0,
                        condition.equals("player-level") ? 3 : 0,
                        0,
                        empty));
        if (!condition.equals("prerequisite")) {
            clear(userId, STAGE);
        }
        assertThatThrownBy(() -> service.complete(userId, next))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("완료 조건");
        assertThat(states.findByUserIdAndTutorialsId(userId, next)).isEmpty();
        switch (condition) {
            case "prerequisite" -> {
                clear(userId, STAGE);
                service.complete(userId, INTRO);
            }
            case "start-stage" -> clear(userId, START);
            case "upgrade" ->
                    tx.executeWithoutResult(
                            s ->
                                    userUpgrades.saveAndFlush(
                                            new UserUpgrade(
                                                    accounts.getReferenceById(userId),
                                                    upgrades.getReferenceById(UPGRADE),
                                                    2)));
            default ->
                    tx.executeWithoutResult(
                            s -> data.findByUserIdForUpdate(userId).orElseThrow().addExp(69));
        }
        assertThat(service.complete(userId, next).tutorialId()).isEqualTo(next);
    }

    @Test
    void concurrentDuplicateCompletionPaysOnceAndDialogueDoesNotLoseCompletion() throws Exception {
        clear(userId, STAGE);
        var duplicate =
                race(() -> service.complete(userId, INTRO), () -> service.complete(userId, INTRO));
        assertThat(duplicate.get(0)).isEqualTo(duplicate.get(1));
        var combined =
                race(
                        () -> service.complete(userId, INTRO + ".dialogue"),
                        () -> service.complete(userId, INTRO));
        assertThat(combined.get(1)).isEqualTo(duplicate.get(0));
        var state = service.list(userId).getFirst();
        assertThat(state.completed()).isTrue();
        assertThat(state.dialogueSeen()).isTrue();
        assertThat(states.findByUserId(userId)).hasSize(1);
        assertState(13, 2, 31);
    }

    @Test
    void firstDialogueAndCompletionRacePreservesBothRecordsAndOnePayout() throws Exception {
        clear(userId, STAGE);
        race(
                () -> service.complete(userId, INTRO + ".dialogue"),
                () -> service.complete(userId, INTRO));
        var state = service.list(userId).getFirst();
        assertThat(state.dialogueSeen()).isTrue();
        assertThat(state.completed()).isTrue();
        assertThat(state.rewardClaimed()).isTrue();
        assertThat(states.findByUserId(userId)).hasSize(1);
        assertState(13, 2, 31);
    }

    @Test
    void sharedRewardEditRacingWithCompletionUsesOneSnapshotAndKeepsTheReceipt() throws Exception {
        clear(userId, STAGE);
        service.complete(userId, INTRO + ".dialogue");
        var receipts =
                race(
                        () -> {
                            rewards.update(
                                    rewardId,
                                    new RewardDefinitionRequest("new", 37, Map.of(ITEM, 7), 1));
                            return service.complete(userId, INTRO);
                        },
                        () -> service.complete(userId, INTRO));
        assertThat(receipts.get(0)).isEqualTo(receipts.get(1));
        var receipt = receipts.getFirst();
        assertThat(receipt.gold()).isIn(13, 37);
        int amount = receipt.gold() == 13 ? 2 : 7;
        assertThat(receipt.items()).containsExactlyEntriesOf(Map.of(ITEM, amount));
        assertThat(service.complete(userId, INTRO)).isEqualTo(receipt);
        assertState(receipt.gold(), amount, 31);
    }

    @ParameterizedTest
    @ValueSource(strings = {"gold-overflow", "item-overflow", "receipt-failure"})
    void failureRollsBackExperienceGoldItemsAndCompletion(String reason) {
        clear(userId, STAGE);
        service.complete(userId, INTRO + ".dialogue");
        tx.executeWithoutResult(
                s -> {
                    data.findByUserIdForUpdate(userId).orElseThrow().addExp(90);
                    if (reason.equals("gold-overflow")) {
                        wallets.findByUserIdForUpdate(userId)
                                .orElseThrow()
                                .addGold(Integer.MAX_VALUE);
                    }
                    if (reason.equals("item-overflow")) {
                        inventory.saveAndFlush(
                                new UserItem(
                                        accounts.getReferenceById(userId),
                                        items.getReferenceById(ITEM),
                                        Integer.MAX_VALUE));
                    }
                });
        if (reason.equals("receipt-failure")) {
            doThrow(new DataIntegrityViolationException("fixture failure"))
                    .when(states)
                    .saveAndFlush(any());
        }
        assertThatThrownBy(() -> service.complete(userId, INTRO))
                .isInstanceOf(RuntimeException.class);
        assertState(
                reason.equals("gold-overflow") ? Integer.MAX_VALUE : 0,
                reason.equals("item-overflow") ? Integer.MAX_VALUE : 0,
                90);
        var state = service.list(userId).getFirst();
        assertThat(state.dialogueSeen()).isTrue();
        assertThat(state.completed()).isFalse();
        assertThat(state.rewardClaimed()).isFalse();
    }

    @Test
    void adminAcceptedSetLevelOneCompletesWithoutLevelConfiguration() throws Exception {
        clear(userId, STAGE);
        levels.deleteAll();
        tx.executeWithoutResult(s -> data.findByUserIdForUpdate(userId).orElseThrow().addExp(90));
        String body =
                "{\"minLevel\":1,\"clearStageId\":"
                        + STAGE
                        + ",\"setLevel\":1,\"rewardDefinitionId\":"
                        + rewardId
                        + "}";
        mvc.perform(
                        put("/api/admin/tutorials/" + INTRO)
                                .header("Authorization", auth(userId, Role.ADMIN))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body))
                .andExpect(status().isOk());
        var receipt = service.complete(userId, INTRO);
        assertThat(receipt.expBefore()).isEqualTo(90);
        assertThat(receipt.expAfter()).isZero();
        assertThat(receipt.gold()).isEqualTo(13);
        assertThat(service.complete(userId, INTRO)).isEqualTo(receipt);
        assertState(13, 2, 0);
    }

    @Test
    void missingLevelConfigFailsClosedAndAccountDeletionRemovesTutorialState() {
        clear(userId, STAGE);
        levels.deleteAll();
        assertThatThrownBy(() -> service.complete(userId, INTRO))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("503");
        assertThat(service.list(userId)).isEmpty();
        assertState(0, 0, 0);
        service.complete(userId, INTRO + ".dialogue");
        users.deleteAccount(userId);
        assertThat(states.findByUserId(userId)).isEmpty();
        userIds.remove(userId);
    }

    @Test
    void httpRejectsForgedRewardsAndConfigurationIsAdminOnly() throws Exception {
        String user = auth(userId, Role.USER);
        String admin = auth(userId, Role.ADMIN);
        for (String body :
                List.of(
                        "{}",
                        "{\"tutorialId\":\"unknown\"}",
                        "{\"tutorialId\":\"" + INTRO + "\",\"gold\":100000}",
                        "{\"tutorialId\":\"" + INTRO + "\",\"completed\":true}")) {
            mvc.perform(
                            post("/api/user/tutorials/complete")
                                    .header("Authorization", user)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(body))
                    .andExpect(status().is4xxClientError());
        }
        mvc.perform(
                        put("/api/admin/tutorials/unknown")
                                .header("Authorization", user)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{}"))
                .andExpect(status().isForbidden());
        mvc.perform(
                        put("/api/admin/tutorials/unknown")
                                .header("Authorization", admin)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{}"))
                .andExpect(status().isBadRequest());
        clear(userId, STAGE);
        String body = "{\"tutorialId\":\"" + INTRO + "\"}";
        mvc.perform(
                        post("/api/user/tutorials/complete")
                                .header("Authorization", user)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.gold").value(13));
        mvc.perform(
                        post("/api/user/tutorials/complete")
                                .header("Authorization", auth(userId, Role.USER))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.gold").value(13));
        assertState(13, 2, 31);
    }

    @Test
    void configurationRejectsCyclesAndProtectsDefinitionStageAndUpgradeReferences() {
        assertThatThrownBy(() -> rewards.delete(rewardId))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("참조 중인 보상 묶음");
        assertThat(rewards.get(rewardId).tutorialReferences()).isEqualTo(1);
        assertThat(rewards.list())
                .filteredOn(x -> x.id().equals(rewardId))
                .allSatisfy(x -> assertThat(x.tutorialReferences()).isEqualTo(1));
        config.save("fixture_guide", request(INTRO, 1, START, null, UPGRADE, 1, 0, 0, rewardId));
        assertThatThrownBy(
                        () ->
                                config.save(
                                        INTRO,
                                        request(
                                                "fixture_guide",
                                                1,
                                                null,
                                                STAGE,
                                                null,
                                                0,
                                                0,
                                                0,
                                                rewardId)))
                .isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> stageService.deleteStage(STAGE))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("참조 중");
        assertThatThrownBy(() -> upgradeService.deleteUpgrade(UPGRADE))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("참조 중");
        service.complete(userId, INTRO + ".dialogue");
        assertThatThrownBy(() -> config.delete(INTRO)).isInstanceOf(ResponseStatusException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"minimum", "completion", "reward"})
    void playerCapEditsPreserveEveryReferencedTutorialLevel(String kind) {
        config.save(
                "fixture_high",
                request(
                        null,
                        kind.equals("minimum") ? 5 : 1,
                        null,
                        kind.equals("completion") ? null : STAGE,
                        null,
                        0,
                        kind.equals("completion") ? 5 : 0,
                        kind.equals("reward") ? 5 : 0,
                        rewardId));
        var before = levelService.getConfig();
        assertThatThrownBy(() -> levelService.saveConfig(new PlayerLevelConfigRequest(31, 7, 4)))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("409");
        assertThat(levelService.getConfig()).isEqualTo(before);
        assertThat(levelService.saveConfig(new PlayerLevelConfigRequest(31, 7, 5)).maxLevel())
                .isEqualTo(5);
        clear(userId, STAGE);
        tx.executeWithoutResult(t -> data.findByUserIdForUpdate(userId).orElseThrow().addExp(1000));
        assertThat(service.complete(userId, "fixture_high").tutorialId()).isEqualTo("fixture_high");
    }

    @Test
    void upgradeCapEditCannotInvalidateExistingTutorialRequirements() {
        config.save("fixture_upgrade", request(null, 1, null, null, UPGRADE, 4, 0, 0, rewardId));
        assertThatThrownBy(() -> upgradeService.updateUpgrade(UPGRADE, upgradeRequest(3)))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("409");
        assertThat(upgrades.findById(UPGRADE).orElseThrow().getMaxLevel()).isEqualTo(4);
        assertThat(upgradeService.updateUpgrade(UPGRADE, upgradeRequest(4)).maxLevel())
                .isEqualTo(4);
        tx.executeWithoutResult(
                t ->
                        userUpgrades.saveAndFlush(
                                new UserUpgrade(
                                        accounts.getReferenceById(userId),
                                        upgrades.getReferenceById(UPGRADE),
                                        4)));
        assertThat(service.complete(userId, "fixture_upgrade").tutorialId())
                .isEqualTo("fixture_upgrade");
    }

    @ParameterizedTest
    @ValueSource(strings = {"player", "upgrade"})
    void concurrentCapReductionAndTutorialRegistrationKeepRequirementsReachable(String kind)
            throws Exception {
        boolean player = kind.equals("player");
        var results =
                race(
                        () -> {
                            try {
                                config.save(
                                        "fixture_race",
                                        request(
                                                null,
                                                player ? 5 : 1,
                                                null,
                                                player ? STAGE : null,
                                                player ? null : UPGRADE,
                                                player ? 0 : 4,
                                                0,
                                                0,
                                                rewardId));
                                return true;
                            } catch (ResponseStatusException error) {
                                assertThat(error.getStatusCode().value()).isEqualTo(400);
                                return false;
                            }
                        },
                        () -> {
                            try {
                                if (player) {
                                    levelService.saveConfig(new PlayerLevelConfigRequest(31, 7, 4));
                                } else {
                                    upgradeService.updateUpgrade(UPGRADE, upgradeRequest(3));
                                }
                                return true;
                            } catch (ResponseStatusException error) {
                                assertThat(error.getStatusCode().value()).isEqualTo(409);
                                return false;
                            }
                        });
        assertThat(results.get(0)).isNotEqualTo(results.get(1));
        assertThat(tutorials.existsById("fixture_race")).isEqualTo(results.get(0));
        int cap =
                player
                        ? levels.findById(PlayerLevelConfig.CONFIG_ID).orElseThrow().getMaxLevel()
                        : upgrades.findById(UPGRADE).orElseThrow().getMaxLevel();
        assertThat(cap).isEqualTo(results.get(0) ? (player ? 8 : 4) : (player ? 4 : 3));
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void firstStateCreationAndDefinitionDeletionHaveAnOrderedOutcome(boolean dialogue)
            throws Exception {
        String id = "fixture_delete";
        config.save(id, request(null, 1, null, STAGE, null, 0, 0, 0, rewardId));
        clear(userId, STAGE);
        String recordId = id + (dialogue ? ".dialogue" : "");
        var results =
                race(
                        () -> {
                            try {
                                service.complete(userId, recordId);
                                return true;
                            } catch (ResponseStatusException error) {
                                assertThat(error.getStatusCode().value()).isEqualTo(404);
                                return false;
                            }
                        },
                        () -> {
                            try {
                                config.delete(id);
                                return true;
                            } catch (ResponseStatusException error) {
                                assertThat(error.getStatusCode().value()).isEqualTo(409);
                                return false;
                            }
                        });
        boolean recorded = results.getFirst();
        assertThat(results.get(1)).isNotEqualTo(recorded);
        assertThat(tutorials.existsById(id)).isEqualTo(recorded);
        assertThat(states.findByUserIdAndTutorialsId(userId, id).isPresent()).isEqualTo(recorded);
        assertState(recorded && !dialogue ? 13 : 0, recorded && !dialogue ? 2 : 0, 0);
        if (recorded) {
            var receipt = service.complete(userId, recordId);
            assertThat(receipt.dialogueOnly()).isEqualTo(dialogue);
            assertThat(service.complete(userId, recordId)).isEqualTo(receipt);
        }
    }

    private UpgradeRequest upgradeRequest(int maxLevel) {
        var request = new UpgradeRequest();
        ReflectionTestUtils.setField(request, "maxLevel", maxLevel);
        ReflectionTestUtils.setField(request, "costBase", 1);
        ReflectionTestUtils.setField(request, "costStep", 0);
        return request;
    }

    private void clear(Long id, int stageId) {
        tx.executeWithoutResult(
                s -> {
                    var row =
                            progress.findByUserIdAndStageId(id, stageId)
                                    .orElseGet(
                                            () ->
                                                    new UserStageProgress(
                                                            accounts.getReferenceById(id),
                                                            stages.getReferenceById(stageId)));
                    row.updateClearRating(1);
                    progress.saveAndFlush(row);
                });
    }

    private void assertState(int gold, int amount, int exp) {
        assertThat(wallets.findByUserId(userId).orElseThrow().getGold()).isEqualTo(gold);
        assertThat(
                        inventory
                                .findByUserIdAndItemId(userId, ITEM)
                                .map(UserItem::getQuantity)
                                .orElse(0))
                .isEqualTo(amount);
        assertThat(data.findByUserId(userId).orElseThrow().getExp()).isEqualTo(exp);
    }

    private TutorialConfigRequest request(
            String prior,
            int min,
            Integer start,
            Integer clear,
            String upgrade,
            int upgradeLevel,
            int playerLevel,
            int setLevel,
            Long reward) {
        return new TutorialConfigRequest(
                prior, min, start, clear, upgrade, upgradeLevel, playerLevel, setLevel, reward);
    }

    private String auth(Long id, Role role) {
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
