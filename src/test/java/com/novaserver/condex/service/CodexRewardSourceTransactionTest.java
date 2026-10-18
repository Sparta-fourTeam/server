package com.novaserver.condex.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.novaserver.auth.jwt.ClientType;
import com.novaserver.auth.jwt.JwtProvider;
import com.novaserver.auth.repository.TokenBlocklistRepository;
import com.novaserver.condex.dto.CodexGradeRewardRequest;
import com.novaserver.condex.dto.CodexMonsterRequest;
import com.novaserver.condex.entity.CodexGradeReward;
import com.novaserver.condex.repository.CodexGradeRewardRepository;
import com.novaserver.condex.repository.CodexRepository;
import com.novaserver.item.entity.Item;
import com.novaserver.item.repository.ItemRepository;
import com.novaserver.reward.dto.RewardDefinitionRequest;
import com.novaserver.reward.repository.RewardDefinitionRepository;
import com.novaserver.reward.service.RewardDefinitionService;
import com.novaserver.user.Provider;
import com.novaserver.user.Role;
import com.novaserver.user.entity.User;
import com.novaserver.user.entity.UserCodex;
import com.novaserver.user.repository.UserCodexRepository;
import com.novaserver.user.repository.UserRepository;
import com.novaserver.user.repository.WalletRepository;
import com.novaserver.user.service.UserService;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

@SpringBootTest
@AutoConfigureMockMvc
class CodexRewardSourceTransactionTest {
    @Autowired private CodexConfigService config;
    @Autowired private CodexRewardSource source;
    @Autowired private UserCodexService codex;
    @Autowired private CodexRepository monsters;
    @Autowired private RewardDefinitionService rewards;
    @Autowired private RewardDefinitionRepository definitions;
    @Autowired private ItemRepository items;
    @Autowired private UserService users;
    @Autowired private UserRepository accounts;
    @Autowired private WalletRepository wallets;
    @Autowired private PlatformTransactionManager transactions;
    @Autowired private MockMvc mvc;
    @Autowired private JwtProvider tokens;
    @MockitoSpyBean private CodexGradeRewardRepository grades;
    @MockitoSpyBean private UserCodexRepository states;
    @MockitoBean private TokenBlocklistRepository blocklist;
    @MockitoBean private Clock clock;
    private TransactionTemplate tx;
    private final List<Long> userIds = new ArrayList<>();
    private final List<Long> rewardIds = new ArrayList<>();
    private Long normal;
    private Long elite;
    private Long boss;
    private Long user;

    @BeforeEach
    void setUp() {
        tx = new TransactionTemplate(transactions);
        when(clock.instant()).thenReturn(Instant.parse("2026-10-19T01:02:03.123456Z"));
        normal = reward(500);
        elite = reward(750);
        boss = reward(1000);
        user = account();
    }

    private Long reward(int gold) {
        Long id =
                rewards.create(new RewardDefinitionRequest("source fixture", gold, Map.of(), null))
                        .id();
        rewardIds.add(id);
        return id;
    }

    private Long account() {
        return tx.execute(
                s -> {
                    var row =
                            accounts.saveAndFlush(
                                    new User(Provider.GUEST, UUID.randomUUID().toString()));
                    users.createInitialState(row);
                    userIds.add(row.getId());
                    return row.getId();
                });
    }

    @AfterEach
    void cleanUp() {
        reset(grades, states);
        for (Long id : userIds) {
            if (accounts.existsById(id)) {
                users.deleteAccount(id);
            }
        }
        grades.deleteAll();
        grades.flush();
        monsters.deleteAllById(List.of(1L, 2L, 11L, 100L));
        for (Long id : rewardIds) {
            if (definitions.existsById(id)) {
                definitions.deleteById(id);
            }
        }
        items.deleteById("codex.source.fixture");
    }

    private void configure() {
        config.saveMonster(1L, new CodexMonsterRequest("Normal", null));
        config.saveMonster(2L, new CodexMonsterRequest("Normal", null));
        config.saveMonster(11L, new CodexMonsterRequest("Elite", null));
        config.saveMonster(100L, new CodexMonsterRequest("Boss", null));
        config.saveReward("Normal", new CodexGradeRewardRequest(normal, null));
        config.saveReward("Elite", new CodexGradeRewardRequest(elite, null));
        config.saveReward("Boss", new CodexGradeRewardRequest(boss, null));
    }

    @Test
    void noSeedAndUnconfiguredSourceReturnsNoReward() {
        assertThat(source).isInstanceOf(DatabaseCodexRewardSource.class);
        assertThat(config.monsters()).isEmpty();
        assertThat(config.rewards()).isEmpty();
        assertThat(source.findForMonster(1L)).isEmpty();
        config.saveMonster(1L, new CodexMonsterRequest("Normal", null));
        codex.recordVerifiedEncounter(user, 1L);
        assertThatThrownBy(() -> codex.claim(user, 1L)).hasMessageContaining("503");
        assertThat(wallets.findByUserId(user).orElseThrow().getGold()).isZero();
    }

    @Test
    void actualDatabaseGradeMappingSuppliesGoldAndPaysEveryMonsterOnce() {
        configure();
        assertThat(source.findForMonster(1L).orElseThrow().getGold()).isEqualTo(500);
        assertThat(source.findForMonster(11L).orElseThrow().getGold()).isEqualTo(750);
        assertThat(source.findForMonster(100L).orElseThrow().getGold()).isEqualTo(1000);
        for (Long id : List.of(1L, 2L, 11L, 100L)) {
            codex.recordVerifiedEncounter(user, id);
            var first = codex.claim(user, id);
            assertThat(codex.claim(user, id)).isEqualTo(first);
        }
        assertThat(wallets.findByUserId(user).orElseThrow().getGold()).isEqualTo(2750);
        assertThat(codex.list(user)).hasSize(4).allMatch(s -> s.claimed());
        assertThat(config.rewards()).hasSize(3);
    }

    @Test
    void explicitIdsRoundTripAndCatalogDoesNotDiscoverAnything() {
        configure();
        assertThat(config.monsters())
                .extracting(s -> s.monsterId())
                .containsExactly(1L, 2L, 11L, 100L);
        assertThat(codex.list(user)).isEmpty();
        assertThatThrownBy(() -> codex.claim(user, 1L)).hasMessageContaining("409");
        assertThat(source.findForMonster(999L)).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"normal", "NORMAL", " Elite", "Boss ", "", "Unknown"})
    void gradeAliasesAndUnknownNamesAreRejected(String name) {
        assertThatThrownBy(() -> config.saveMonster(1L, new CodexMonsterRequest(name, null)))
                .hasMessageContaining("400");
        assertThatThrownBy(() -> config.saveReward(name, new CodexGradeRewardRequest(normal, null)))
                .hasMessageContaining("400");
        assertThat(config.monsters()).isEmpty();
        assertThat(config.rewards()).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(longs = {0, -1, 2147483648L})
    void monsterIdsMustUsePositiveClientIntRange(long id) {
        assertThatThrownBy(() -> config.saveMonster(id, new CodexMonsterRequest("Normal", null)))
                .hasMessageContaining("400");
    }

    @Test
    void exactExpectedVersionsProtectMonsterAndGradeConfiguration() {
        configure();
        assertThatThrownBy(() -> config.saveMonster(1L, new CodexMonsterRequest("Elite", null)))
                .hasMessageContaining("409");
        assertThatThrownBy(() -> config.saveMonster(1L, new CodexMonsterRequest("Elite", 0)))
                .hasMessageContaining("409");
        var edited = config.saveMonster(1L, new CodexMonsterRequest("Elite", 1));
        assertThat(edited.version()).isEqualTo(2);
        assertThat(source.findForMonster(1L).orElseThrow().getGold()).isEqualTo(750);
        assertThatThrownBy(
                        () -> config.saveReward("Normal", new CodexGradeRewardRequest(boss, null)))
                .hasMessageContaining("409");
        assertThat(config.saveReward("Normal", new CodexGradeRewardRequest(boss, 1)).version())
                .isEqualTo(2);
        assertThat(source.findForMonster(2L).orElseThrow().getGold()).isEqualTo(1000);
        assertThatThrownBy(
                        () -> config.saveReward("Normal", new CodexGradeRewardRequest(normal, 1)))
                .hasMessageContaining("409");
    }

    @Test
    void newConfigurationRejectsUnexpectedVersionAndMissingReward() {
        assertThatThrownBy(() -> config.saveMonster(1L, new CodexMonsterRequest("Normal", 1)))
                .hasMessageContaining("409");
        assertThatThrownBy(
                        () -> config.saveReward("Normal", new CodexGradeRewardRequest(normal, 1)))
                .hasMessageContaining("409");
        assertThatThrownBy(
                        () ->
                                config.saveReward(
                                        "Normal",
                                        new CodexGradeRewardRequest(Long.MAX_VALUE, null)))
                .hasMessageContaining("404");
    }

    @Test
    void onlyPositiveGoldOnlyDefinitionsCanBeLinkedAndSharedEditsAreGuarded() {
        Long empty = reward(0);
        assertThatThrownBy(
                        () -> config.saveReward("Normal", new CodexGradeRewardRequest(empty, null)))
                .hasMessageContaining("400");
        items.saveAndFlush(new Item("codex.source.fixture", null));
        Long itemReward =
                rewards.create(
                                new RewardDefinitionRequest(
                                        "item fixture",
                                        500,
                                        Map.of("codex.source.fixture", 1),
                                        null))
                        .id();
        rewardIds.add(itemReward);
        assertThatThrownBy(
                        () ->
                                config.saveReward(
                                        "Normal", new CodexGradeRewardRequest(itemReward, null)))
                .hasMessageContaining("400");
        configure();
        assertThatThrownBy(
                        () ->
                                rewards.update(
                                        normal,
                                        new RewardDefinitionRequest("zero", 0, Map.of(), 1)))
                .hasMessageContaining("400");
        assertThatThrownBy(
                        () ->
                                rewards.update(
                                        normal,
                                        new RewardDefinitionRequest(
                                                "item", 500, Map.of("codex.source.fixture", 1), 1)))
                .hasMessageContaining("400");
        assertThat(rewards.get(normal).version()).isEqualTo(1);
        assertThat(source.findForMonster(1L).orElseThrow().getGold()).isEqualTo(500);
    }

    @Test
    void configuredGradesAreCountedAndPreventSharedDefinitionDeletion() {
        configure();
        config.saveReward("Elite", new CodexGradeRewardRequest(normal, 1));
        assertThat(rewards.get(normal).codexGradeReferences()).isEqualTo(2);
        assertThat(rewards.list())
                .filteredOn(r -> r.id().equals(normal))
                .singleElement()
                .satisfies(r -> assertThat(r.codexGradeReferences()).isEqualTo(2));
        assertThatThrownBy(() -> rewards.delete(normal)).hasMessageContaining("409");
        config.deleteReward("Normal");
        config.deleteReward("Elite");
        rewards.delete(normal);
        assertThat(definitions.existsById(normal)).isFalse();
    }

    @Test
    void immutableReceiptSurvivesGradeDefinitionChangesAndSupplyRemoval() {
        configure();
        codex.recordVerifiedEncounter(user, 1L);
        final var first = codex.claim(user, 1L);
        rewards.update(normal, new RewardDefinitionRequest("edit", 621, Map.of(), 1));
        assertThat(config.rewards())
                .filteredOn(r -> r.grade().equals("Normal"))
                .singleElement()
                .satisfies(
                        r -> {
                            assertThat(r.gold()).isEqualTo(621);
                            assertThat(r.rewardVersion()).isEqualTo(2);
                        });
        codex.recordVerifiedEncounter(user, 2L);
        assertThat(codex.claim(user, 2L).gold()).isEqualTo(621);
        config.saveMonster(1L, new CodexMonsterRequest("Boss", 1));
        config.deleteReward("Normal");
        config.deleteReward("Boss");
        rewards.delete(normal);
        assertThat(codex.claim(user, 1L)).isEqualTo(first);
        assertThat(codex.list(user))
                .filteredOn(s -> s.monsterId().equals(1L))
                .singleElement()
                .satisfies(s -> assertThat(s.receipt()).isEqualTo(first));
        Long other = account();
        codex.recordVerifiedEncounter(other, 1L);
        assertThatThrownBy(() -> codex.claim(other, 1L)).hasMessageContaining("503");
    }

    @Test
    void realSourceConcurrentClaimsPreserveOnePayout() throws Exception {
        configure();
        codex.recordVerifiedEncounter(user, 1L);
        var result = race(() -> codex.claim(user, 1L), () -> codex.claim(user, 1L));
        assertThat(result.getFirst()).isEqualTo(result.getLast());
        assertThat(wallets.findByUserId(user).orElseThrow().getGold()).isEqualTo(500);
    }

    @Test
    void realSourceReceiptFailureRollsBackGold() {
        configure();
        codex.recordVerifiedEncounter(user, 1L);
        doThrow(new IllegalStateException("receipt failed"))
                .when(states)
                .saveAndFlush(any(UserCodex.class));
        assertThatThrownBy(() -> codex.claim(user, 1L)).hasMessageContaining("receipt failed");
        assertThat(wallets.findByUserId(user).orElseThrow().getGold()).isZero();
        assertThat(codex.list(user).getFirst().claimed()).isFalse();
        reset(states);
        assertThat(codex.claim(user, 1L).gold()).isEqualTo(500);
    }

    @Test
    void configurationFlushFailureKeepsOriginalDefinitionAndVersion() {
        configure();
        doThrow(new IllegalStateException("config failed"))
                .when(grades)
                .saveAndFlush(any(CodexGradeReward.class));
        assertThatThrownBy(() -> config.saveReward("Normal", new CodexGradeRewardRequest(boss, 1)))
                .hasMessageContaining("config failed");
        assertThat(source.findForMonster(1L).orElseThrow().getGold()).isEqualTo(500);
        assertThat(config.rewards())
                .filteredOn(r -> r.grade().equals("Normal"))
                .singleElement()
                .satisfies(r -> assertThat(r.version()).isEqualTo(1));
    }

    @Test
    void corruptedSharedDefinitionFailsClosed() {
        configure();
        codex.recordVerifiedEncounter(user, 1L);
        tx.executeWithoutResult(
                s -> definitions.findById(normal).orElseThrow().replace("corrupt", 0, Map.of()));
        assertThatThrownBy(() -> codex.claim(user, 1L)).hasMessageContaining("503");
        assertThat(wallets.findByUserId(user).orElseThrow().getGold()).isZero();
    }

    @Test
    void concurrentLinkAndDefinitionDeletionCannotCreateDanglingReward() throws Exception {
        var result =
                race(
                        () ->
                                outcome(
                                        () ->
                                                config.saveReward(
                                                        "Normal",
                                                        new CodexGradeRewardRequest(normal, null))),
                        () ->
                                outcome(
                                        () -> {
                                            rewards.delete(normal);
                                            return true;
                                        }));
        assertThat(result).contains(200);
        if (definitions.existsById(normal)) {
            assertThat(config.rewards()).hasSize(1);
            assertThat(result).contains(409);
        } else {
            assertThat(config.rewards()).isEmpty();
            assertThat(result).contains(404);
        }
    }

    @Test
    void concurrentSameVersionConfigEditsAcceptOnlyOne() throws Exception {
        configure();
        var result =
                race(
                        () ->
                                outcome(
                                        () ->
                                                config.saveReward(
                                                        "Normal",
                                                        new CodexGradeRewardRequest(elite, 1))),
                        () ->
                                outcome(
                                        () ->
                                                config.saveReward(
                                                        "Normal",
                                                        new CodexGradeRewardRequest(boss, 1))));
        assertThat(result).containsExactlyInAnyOrder(200, 409);
    }

    @Test
    void adminGameAndAnonymousAuthorizationAndActualHttpConfiguration() throws Exception {
        mvc.perform(get("/api/admin/codex/rewards")).andExpect(status().isUnauthorized());
        mvc.perform(
                        get("/api/admin/codex/rewards")
                                .header("Authorization", auth(Role.USER, ClientType.GAME)))
                .andExpect(status().isForbidden());
        mvc.perform(
                        put("/api/admin/codex/monsters/1")
                                .header("Authorization", auth(Role.ADMIN, ClientType.WEB))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"grade\":\"Normal\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.monsterId").value(1))
                .andExpect(jsonPath("$.grade").value("Normal"));
        mvc.perform(
                        put("/api/admin/codex/rewards/Normal")
                                .header("Authorization", auth(Role.ADMIN, ClientType.WEB))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"rewardDefinitionId\":" + normal + "}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.gold").value(500));
        mvc.perform(
                        get("/api/admin/codex/rewards")
                                .header("Authorization", auth(Role.ADMIN, ClientType.WEB)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].grade").value("Normal"));
        assertThat(codex.list(user)).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"{}", "{\"grade\":null}", "{\"grade\":\"\"}", "{\"grade\":\"normal\"}"})
    void invalidHttpGradeDoesNotCreateMonster(String body) throws Exception {
        mvc.perform(
                        put("/api/admin/codex/monsters/1")
                                .header("Authorization", auth(Role.ADMIN, ClientType.WEB))
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body))
                .andExpect(status().isBadRequest());
        assertThat(config.monsters()).isEmpty();
    }

    @Test
    void deletingReferencedDefinitionDirectlyIsBlockedByDatabaseForeignKey() {
        configure();
        assertThatThrownBy(
                        () ->
                                tx.executeWithoutResult(
                                        s -> {
                                            definitions.deleteById(normal);
                                            definitions.flush();
                                        }))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThat(source.findForMonster(1L).orElseThrow().getGold()).isEqualTo(500);
    }

    @ParameterizedTest
    @ValueSource(strings = {"GET", "PUT", "DELETE"})
    void adminGameCannotReadOrWriteCodexConfiguration(String method) throws Exception {
        var request =
                switch (method) {
                    case "GET" -> get("/api/admin/codex/rewards");
                    case "PUT" ->
                            put("/api/admin/codex/monsters/1")
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content("{\"grade\":\"Normal\"}");
                    case "DELETE" -> delete("/api/admin/codex/rewards/Normal");
                    default -> throw new IllegalArgumentException(method);
                };
        mvc.perform(request.header("Authorization", auth(Role.ADMIN, ClientType.GAME)))
                .andExpect(status().isForbidden());
        assertThat(config.monsters()).isEmpty();
        assertThat(config.rewards()).isEmpty();
    }

    private String auth(Role role, ClientType type) {
        return "Bearer " + tokens.createAccessToken(user, type, role);
    }

    private int outcome(Callable<?> action) throws Exception {
        try {
            action.call();
            return 200;
        } catch (ResponseStatusException e) {
            return e.getStatusCode().value();
        }
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
