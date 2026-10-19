package com.novaserver.battle.progress;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.novaserver.auth.jwt.ClientType;
import com.novaserver.auth.jwt.JwtProvider;
import com.novaserver.auth.repository.TokenBlocklistRepository;
import com.novaserver.battle.service.BattleService;
import com.novaserver.condex.entity.Codex;
import com.novaserver.condex.entity.MonsterGrade;
import com.novaserver.condex.repository.CodexRepository;
import com.novaserver.quest.dto.QuestConfigRequest;
import com.novaserver.quest.entity.QuestKind;
import com.novaserver.quest.entity.QuestPeriod;
import com.novaserver.quest.repository.UserQuestPeriodRepository;
import com.novaserver.quest.service.QuestConfigService;
import com.novaserver.stage.entity.Stage;
import com.novaserver.stage.repository.StageRepository;
import com.novaserver.user.Provider;
import com.novaserver.user.Role;
import com.novaserver.user.entity.User;
import com.novaserver.user.entity.UserStageProgress;
import com.novaserver.user.repository.UserRepository;
import com.novaserver.user.repository.UserStageProgressRepository;
import com.novaserver.user.service.UserService;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.event.EventListener;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

@SpringBootTest
@AutoConfigureMockMvc
class BattleProgressTransactionTest {
    private static final Instant START = Instant.parse("2026-10-18T18:59:50Z");
    @Autowired BattleProgressService service;
    @Autowired com.novaserver.monster.service.MonsterCatalog monsterCatalog;
    @Autowired BattleSourceAdminService publisher;
    @Autowired com.novaserver.version.service.DataVersionService versions;
    @Autowired BattleSourceRepository sourceRows;
    @Autowired MockMvc mvc;
    @Autowired JwtProvider jwt;
    @Autowired BattleProgressPolicyService policies;
    @MockitoSpyBean BattlePolicySource source;
    @Autowired BattleProgressRepository ledger;
    @Autowired BattleProgressReceiptRepository receipts;
    @Autowired StageProgressPolicyRepository definitions;
    @Autowired BattleService battles;
    @Autowired UserRepository accounts;
    @Autowired UserService users;
    @Autowired StageRepository stages;
    @Autowired CodexRepository monsters;
    @Autowired UserStageProgressRepository unlocked;
    @Autowired UserQuestPeriodRepository periods;
    @Autowired QuestConfigService quests;
    @Autowired PlatformTransactionManager transactions;
    @Autowired EncounterFailure encounterFailure;
    @MockitoBean Clock clock;
    @MockitoBean TokenBlocklistRepository blocklist;
    private Long user;
    private String battle;

    @BeforeEach
    void prepare() {
        encounterFailure.fail.set(false);
        when(clock.instant()).thenReturn(START);
        var tx = new TransactionTemplate(transactions);
        user =
                tx.execute(
                        status -> {
                            stages.save(Stage.create(901));
                            monsters.save(new Codex(1L, MonsterGrade.NORMAL));
                            monsters.save(new Codex(2L, MonsterGrade.NORMAL));
                            var account =
                                    accounts.saveAndFlush(
                                            new User(Provider.GUEST, UUID.randomUUID().toString()));
                            users.createInitialState(account);
                            unlocked.saveAndFlush(
                                    new UserStageProgress(
                                            account, stages.findById(901).orElseThrow()));
                            return account.getId();
                        });
        sourceRows.deleteAll();
        publisher.publish(BattleSourceFixture.publication(null));
        definitions.deleteAll();
        policies.save(901, policy(0));
        quests.saveQuest(
                "daily.fixture.kill",
                new QuestConfigRequest(QuestPeriod.DAILY, QuestKind.KILL_MONSTERS, 100, 17));
        battle = battles.createBattle(user, UUID.randomUUID().toString()).getBattleId();
        service.open(user, battle, 901, policies.sourceRevision());
        when(clock.instant()).thenReturn(START.plusSeconds(5));
    }

    @AfterEach
    void cleanupAccount() {
        if (accounts.existsById(user)) {
            users.deleteAccount(user);
        }
    }

    @Test
    void cumulativeDeltaUsesFirstAcceptedPeriodAndReplaysOriginalAckAfterResetAndCancel() {
        var first = report(0, 3, 1, 1);
        var ack = service.report(user, first);
        assertThat(ack.acceptedDelta()).isEqualTo(3);
        assertThat(ack.dailyKey()).isEqualTo("20261018");
        when(clock.instant()).thenReturn(START.plusSeconds(15));
        var next = service.report(user, report(1, 7, 1, 2));
        assertThat(next.acceptedDelta()).isEqualTo(4);
        assertThat(next.dailyKey()).isEqualTo("20261019");
        assertThat(
                        periods.findByUserIdAndPeriodAndPeriodDay(
                                        user, QuestPeriod.DAILY, LocalDate.of(2026, 10, 18))
                                .orElseThrow()
                                .getKilledMonsters())
                .isEqualTo(3);
        assertThat(
                        periods.findByUserIdAndPeriodAndPeriodDay(
                                        user, QuestPeriod.DAILY, LocalDate.of(2026, 10, 19))
                                .orElseThrow()
                                .getKilledMonsters())
                .isEqualTo(4);
        battles.cancelBattle(user, battle);
        assertThat(service.report(user, first)).isEqualTo(ack);
        assertThat(service.snapshot(user, battle).closed()).isTrue();
        assertThatThrownBy(() -> service.report(user, report(2, 8, 1, 2)))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void reversedDuplicateAndChangedPayloadCannotIncreaseProgress() throws Exception {
        var request = report(0, 3, 1, 1);
        try (var pool = Executors.newFixedThreadPool(2)) {
            var a = pool.submit(() -> service.report(user, request));
            var b = pool.submit(() -> service.report(user, request));
            assertThat(a.get()).isEqualTo(b.get());
        }
        assertThat(service.snapshot(user, battle).progress().killedMonsters()).isEqualTo(3);
        assertThatThrownBy(() -> service.report(user, report(0, 6, 1, 2)))
                .isInstanceOf(ResponseStatusException.class);
        var changed =
                new ProgressReport(
                        battle,
                        request.reportId(),
                        0,
                        4,
                        1,
                        1,
                        Set.of(1L, 2L),
                        policies.sourceRevision());
        assertThatThrownBy(() -> service.report(user, changed))
                .isInstanceOf(ResponseStatusException.class);
        assertThat(service.snapshot(user, battle).progress().revision()).isEqualTo(1);
    }

    @Test
    void invalidCatalogTimeWaveAndRollbackReportsLeaveNoReceiptOrProgress() {
        var absent =
                new ProgressReport(
                        battle,
                        UUID.randomUUID().toString(),
                        0,
                        3,
                        1,
                        1,
                        Set.of(999L),
                        policies.sourceRevision());
        assertThatThrownBy(() -> service.report(user, absent))
                .isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> service.report(user, report(0, 16, 1, 1)))
                .isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> service.report(user, report(0, 1, 1, 1)))
                .isInstanceOf(ResponseStatusException.class);
        assertThatThrownBy(() -> service.report(user, report(0, 3, 0, 2)))
                .isInstanceOf(ResponseStatusException.class);
        assertThat(service.snapshot(user, battle).progress().revision()).isZero();
        service.report(user, report(0, 3, 1, 1));
        assertThatThrownBy(() -> service.report(user, report(1, 2, 1, 1)))
                .isInstanceOf(ResponseStatusException.class);
        assertThat(service.snapshot(user, battle).progress().killedMonsters()).isEqualTo(3);
    }

    @Test
    void policyChangesDoNotRewriteActiveBattleAndMissingPolicyFailsClosed() {
        policies.save(
                901,
                new StageProgressPolicy(
                        BigDecimal.ONE,
                        BigDecimal.TEN,
                        List.of(Map.of(1L, 2)),
                        Map.of(1L, List.of()),
                        1));
        assertThat(service.report(user, report(0, 3, 1, 1)).policyVersion()).isEqualTo(1);
        definitions.deleteAll();
        var next = battles.createBattle(user, UUID.randomUUID().toString()).getBattleId();
        assertThatThrownBy(() -> service.open(user, next, 901, policies.sourceRevision()))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("503");
        assertThat(service.snapshot(user, battle).closed()).isTrue();
        assertThatThrownBy(() -> service.report(user, report(1, 4, 1, 1)))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void resumedBattleHasNoNewTimeExpiryOrLifetimeKillCap() {
        when(clock.instant()).thenReturn(START.plus(java.time.Duration.ofDays(30)));
        var ack = service.report(user, report(0, 1000, 1, 2));
        assertThat(ack.acceptedDelta()).isEqualTo(1000);
        assertThat(service.snapshot(user, battle).closed()).isFalse();
    }

    @Test
    void currentReleasePolicyRejectsEditorSpeedAllowance() {
        assertThatThrownBy(
                        () ->
                                policies.save(
                                        901,
                                        new StageProgressPolicy(
                                                BigDecimal.valueOf(8),
                                                BigDecimal.ONE,
                                                List.of(Map.of(1L, 2)),
                                                Map.of(1L, List.of()),
                                                1)))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("400");
        assertThat(service.report(user, report(0, 3, 1, 1)).policyVersion()).isEqualTo(1);
    }

    @Test
    void realStagePolicyIsGeneratedWithoutAdminInputAndWrongSourceCannotBind() {
        stages.saveAndFlush(Stage.create(1));
        new TransactionTemplate(transactions)
                .execute(
                        status -> {
                            if (unlocked.findByUserIdAndStageId(user, 1).isEmpty()) {
                                unlocked.saveAndFlush(
                                        new UserStageProgress(
                                                accounts.findById(user).orElseThrow(),
                                                stages.findById(1).orElseThrow()));
                            }
                            return null;
                        });
        definitions.deleteAll();
        var next = battles.createBattle(user, UUID.randomUUID().toString()).getBattleId();
        assertThatThrownBy(() -> service.open(user, next, 1, "0".repeat(64)))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("409");
        assertThat(ledger.findById(next)).isEmpty();
        var state = service.open(user, next, 1, policies.sourceRevision());
        assertThat(state.progress().sourceRevision()).isEqualTo(policies.sourceRevision());
        assertThat(definitions.findById(1)).isPresent();
        when(clock.instant()).thenReturn(START.plusSeconds(15));
        var report =
                new ProgressReport(
                        next,
                        UUID.randomUUID().toString(),
                        0,
                        2,
                        1,
                        1,
                        Set.of(2L),
                        policies.sourceRevision());
        assertThat(service.report(user, report).acceptedDelta()).isEqualTo(2);
        var changedSource =
                new ProgressReport(
                        next, UUID.randomUUID().toString(), 1, 3, 1, 1, Set.of(2L), "0".repeat(64));
        assertThatThrownBy(() -> service.report(user, changedSource))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("409");
        assertThat(service.snapshot(user, next).progress().killedMonsters()).isEqualTo(2);
    }

    @Test
    void newSourceRegeneratesPolicyButKeepsOldBattleAndReceiptsOnFrozenSource() throws Exception {
        stages.saveAndFlush(Stage.create(1));
        new TransactionTemplate(transactions)
                .execute(
                        status -> {
                            if (unlocked.findByUserIdAndStageId(user, 1).isEmpty()) {
                                unlocked.saveAndFlush(
                                        new UserStageProgress(
                                                accounts.findById(user).orElseThrow(),
                                                stages.findById(1).orElseThrow()));
                            }
                            return null;
                        });
        String originalRevision = policies.sourceRevision();
        var firstBattle = battles.createBattle(user, UUID.randomUUID().toString()).getBattleId();
        service.open(user, firstBattle, 1, originalRevision);
        when(clock.instant()).thenReturn(START.plusSeconds(15));
        var firstReport =
                new ProgressReport(
                        firstBattle,
                        UUID.randomUUID().toString(),
                        0,
                        2,
                        1,
                        1,
                        Set.of(2L),
                        originalRevision);
        final var originalAck = service.report(user, firstReport);
        var replacement =
                new BattlePolicySource(
                        source.stagesJson() + "\n", source.monstersJson(), source.combatJson());
        publisher.publish(
                new BattleSourceAdminService.Publication(
                        replacement.stagesJson(),
                        replacement.monstersJson(),
                        replacement.combatJson(),
                        originalRevision));
        assertThat(policies.require(1).getVersion()).isEqualTo(2);
        var laterOldReport =
                new ProgressReport(
                        firstBattle,
                        UUID.randomUUID().toString(),
                        1,
                        3,
                        1,
                        1,
                        Set.of(2L),
                        originalRevision);
        assertThat(service.report(user, laterOldReport).sourceRevision())
                .isEqualTo(originalRevision);
        assertThat(service.open(user, firstBattle, 1, originalRevision).progress().policyVersion())
                .isEqualTo(1);
        var next = battles.createBattle(user, UUID.randomUUID().toString()).getBattleId();
        var current = service.open(user, next, 1, replacement.revision());
        assertThat(current.progress().sourceRevision()).isEqualTo(replacement.revision());
        assertThat(current.progress().policyVersion()).isEqualTo(2);
        assertThat(service.report(user, firstReport)).isEqualTo(originalAck);
    }

    @Test
    void accountDeletionCascadesLedgerAndReportReceipt() {
        var request = report(0, 3, 1, 1);
        service.report(user, request);
        users.deleteAccount(user);
        assertThat(ledger.findById(battle)).isEmpty();
        assertThat(receipts.findById(request.reportId())).isEmpty();
    }

    @Test
    void authenticatedHttpAbandonCommitsProgressOnceWithoutFinalRewardAndReplaysAfterClose()
            throws Exception {
        var request = report(0, 3, 1, 1);
        String body = BattleProgressPolicyService.encode(request);
        String token = "Bearer " + jwt.createAccessToken(user, ClientType.GAME, Role.USER);
        for (int i = 0; i < 2; i++) {
            mvc.perform(
                            post("/api/battle/progress/abandon")
                                    .header("Authorization", token)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(body))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.revision").value(1))
                    .andExpect(jsonPath("$.acceptedDelta").value(3));
        }
        assertThat(service.snapshot(user, battle).closed()).isTrue();
        assertThat(
                        periods.findByUserIdAndPeriodAndPeriodDay(
                                        user, QuestPeriod.DAILY, LocalDate.of(2026, 10, 18))
                                .orElseThrow()
                                .getKilledMonsters())
                .isEqualTo(3);
        mvc.perform(
                        post("/api/battle/progress")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void encounterListenerFailureRollsBackQuestLedgerAndReceiptTogether() {
        var request = report(0, 3, 1, 1);
        encounterFailure.fail.set(true);
        assertThatThrownBy(() -> service.report(user, request))
                .isInstanceOf(IllegalStateException.class);
        assertThat(receipts.findById(request.reportId())).isEmpty();
        assertThat(service.snapshot(user, battle).progress().revision()).isZero();
        assertThat(
                        periods.findByUserIdAndPeriodAndPeriodDay(
                                        user, QuestPeriod.DAILY, LocalDate.of(2026, 10, 18))
                                .orElseThrow()
                                .getKilledMonsters())
                .isZero();
        encounterFailure.fail.set(false);
        assertThat(service.report(user, request).acceptedDelta()).isEqualTo(3);
    }

    @Test
    void anotherAccountCannotReadOrSubmitOwnedBattle() {
        var other = accounts.saveAndFlush(new User(Provider.GUEST, UUID.randomUUID().toString()));
        try {
            assertThatThrownBy(() -> service.snapshot(other.getId(), battle))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("404");
            assertThatThrownBy(() -> service.report(other.getId(), report(0, 3, 1, 1)))
                    .isInstanceOf(ResponseStatusException.class)
                    .hasMessageContaining("404");
        } finally {
            users.deleteAccount(other.getId());
        }
    }

    private ProgressReport report(long revision, int kills, int completed, int reached) {
        return new ProgressReport(
                battle,
                UUID.randomUUID().toString(),
                revision,
                kills,
                completed,
                reached,
                Set.of(1L, 2L),
                policies.sourceRevision());
    }

    @Test
    void firstAttemptAndNewlyReachedStageOpenWithoutExistingClearRows() throws Exception {
        var tx = new TransactionTemplate(transactions);
        tx.execute(
                status -> {
                    unlocked.deleteByUserId(user);
                    return null;
                });
        var json = new com.fasterxml.jackson.databind.ObjectMapper();
        var template = json.readTree(BattleSourceFixture.stages()).get(0);
        var rows = json.createArrayNode();
        for (int id = 1; id <= 3; id++) {
            var copy = (com.fasterxml.jackson.databind.node.ObjectNode) template.deepCopy();
            copy.put("Id", id);
            rows.add(copy);
            stages.saveAndFlush(Stage.create(id));
        }
        publisher.publish(
                new BattleSourceAdminService.Publication(
                        rows.toString(),
                        source.monstersJson(),
                        source.combatJson(),
                        source.revision()));
        String first = battles.createBattle(user, UUID.randomUUID().toString()).getBattleId();
        assertThat(service.open(user, first, 1, source.revision()).progress().stageId())
                .isEqualTo(1);
        assertThat(unlocked.findByUserId(user)).isEmpty();
        tx.execute(
                status -> {
                    var clear =
                            new UserStageProgress(
                                    accounts.findById(user).orElseThrow(),
                                    stages.findById(1).orElseThrow());
                    clear.updateClearRating(1);
                    unlocked.saveAndFlush(clear);
                    return null;
                });
        String next = battles.createBattle(user, UUID.randomUUID().toString()).getBattleId();
        assertThat(service.open(user, next, 2, source.revision()).progress().stageId())
                .isEqualTo(2);
        assertThat(unlocked.findByUserIdAndStageId(user, 2)).isEmpty();
        String locked = battles.createBattle(user, UUID.randomUUID().toString()).getBattleId();
        assertThatThrownBy(() -> service.open(user, locked, 3, source.revision()))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void monsterDownloadBlocksPublicationUntilBodyAndNumericVersionAreRead() throws Exception {
        var oldSource = source.snapshot();
        final int oldVersion = versions.getVersions().getTables().get("Monsters");
        var json = new com.fasterxml.jackson.databind.ObjectMapper();
        var changed = json.readTree(oldSource.monstersJson());
        ((com.fasterxml.jackson.databind.node.ObjectNode) changed.get(0)).put("Hp", 999);
        var request =
                new BattleSourceAdminService.Publication(
                        oldSource.stagesJson(),
                        changed.toString(),
                        oldSource.combatJson(),
                        oldSource.revision());
        var captured = new java.util.concurrent.CountDownLatch(1);
        final var release = new java.util.concurrent.CountDownLatch(1);
        org.mockito.Mockito.doAnswer(
                        call -> {
                            var snapshot = call.callRealMethod();
                            captured.countDown();
                            if (!release.await(10, java.util.concurrent.TimeUnit.SECONDS)) {
                                throw new IllegalStateException("reader timeout");
                            }
                            return snapshot;
                        })
                .when(source)
                .snapshot();
        try (var pool = java.util.concurrent.Executors.newFixedThreadPool(2)) {
            var read = pool.submit(() -> monsterCatalog.download());
            try {
                assertThat(captured.await(10, java.util.concurrent.TimeUnit.SECONDS)).isTrue();
                var write = pool.submit(() -> publisher.publish(request));
                assertThatThrownBy(() -> write.get(200, java.util.concurrent.TimeUnit.MILLISECONDS))
                        .isInstanceOf(java.util.concurrent.TimeoutException.class);
                release.countDown();
                var download = read.get(10, java.util.concurrent.TimeUnit.SECONDS);
                assertThat(download.json()).isEqualTo(oldSource.monstersJson());
                assertThat(download.version()).isEqualTo(oldVersion);
                write.get(10, java.util.concurrent.TimeUnit.SECONDS);
                assertThat(monsterCatalog.download().json()).isEqualTo(changed.toString());
                assertThat(monsterCatalog.download().version()).isEqualTo(oldVersion + 1);
            } finally {
                release.countDown();
            }
        }
    }

    @Test
    void eachRawEndpointUsesOneSnapshotForBodyAndRevisionDuringPublication() throws Exception {
        var previous = source.snapshot();
        org.mockito.Mockito.doReturn(previous).when(source).snapshot();
        org.mockito.Mockito.doReturn("b".repeat(64)).when(source).revision();
        String token = "Bearer " + jwt.createAccessToken(user, ClientType.GAME, Role.USER);
        for (var table :
                Map.of("stages", previous.stagesJson(), "combat", previous.combatJson())
                        .entrySet()) {
            mvc.perform(
                            org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(
                                            "/api/data/" + table.getKey())
                                    .header("Authorization", token))
                    .andExpect(
                            org.springframework.test.web.servlet.result.MockMvcResultMatchers
                                    .header()
                                    .string("X-Battle-Source-Revision", previous.revision()))
                    .andExpect(
                            org.springframework.test.web.servlet.result.MockMvcResultMatchers
                                    .content()
                                    .string(table.getValue()));
        }
    }

    @Test
    void publicationIsAdminOnlyAtomicVersionedAndDoesNotFallbackToFiles() throws Exception {
        String token = "Bearer " + jwt.createAccessToken(user, ClientType.GAME, Role.USER);
        String admin = "Bearer " + jwt.createAccessToken(user, ClientType.WEB, Role.ADMIN);
        String original = source.revision();
        final var before = versions.getVersions().getTables();
        var invalid =
                new BattleSourceAdminService.Publication(
                        "[]", source.monstersJson(), source.combatJson(), original);
        mvc.perform(
                        org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put(
                                        "/api/admin/data/battle-source")
                                .header("Authorization", token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(BattleProgressPolicyService.encode(invalid)))
                .andExpect(
                        org.springframework.test.web.servlet.result.MockMvcResultMatchers.status()
                                .isForbidden());
        mvc.perform(
                        org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put(
                                        "/api/admin/data/battle-source")
                                .header("Authorization", admin)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(BattleProgressPolicyService.encode(invalid)))
                .andExpect(
                        org.springframework.test.web.servlet.result.MockMvcResultMatchers.status()
                                .isBadRequest());
        assertThat(source.revision()).isEqualTo(original);
        assertThat(versions.getVersions().getTables()).isEqualTo(before);
        publisher.publish(
                new BattleSourceAdminService.Publication(
                        source.stagesJson(),
                        source.monstersJson(),
                        "{\"BaseTimeScale\":0.75}",
                        original));
        assertThat(source.manifest().maxTimeScale()).isEqualByComparingTo("1.50");
        assertThat(versions.getVersions().getTables().get("Combat"))
                .isEqualTo(before.get("Combat") + 1);
        assertThat(versions.getVersions().getTables().get("Stages"))
                .isEqualTo(before.get("Stages"));
        assertThatThrownBy(() -> publisher.publish(BattleSourceFixture.publication(original)))
                .isInstanceOf(ResponseStatusException.class);
        sourceRows.deleteAll();
        mvc.perform(
                        org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(
                                        "/api/data/battle-source/snapshot")
                                .header("Authorization", token))
                .andExpect(
                        org.springframework.test.web.servlet.result.MockMvcResultMatchers.status()
                                .isServiceUnavailable());
    }

    private StageProgressPolicy policy(int version) {
        return new StageProgressPolicy(
                BigDecimal.ONE,
                BigDecimal.ONE,
                List.of(Map.of(1L, 2), Map.of(1L, 2)),
                Map.of(1L, List.of(new StageProgressPolicy.SpawnLimit(2L, 2)), 2L, List.of()),
                version);
    }

    @TestConfiguration
    static class ListenerConfiguration {
        @Bean
        EncounterFailure encounterFailure() {
            return new EncounterFailure();
        }
    }

    static class EncounterFailure {
        final AtomicBoolean fail = new AtomicBoolean();

        @EventListener
        public void accept(AcceptedBattleEncounters event) {
            if (fail.get()) {
                throw new IllegalStateException("fixture encounter failure");
            }
        }
    }
}
