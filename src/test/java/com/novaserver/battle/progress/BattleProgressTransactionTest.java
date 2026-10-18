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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

@SpringBootTest
@AutoConfigureMockMvc
class BattleProgressTransactionTest {
    private static final Instant START = Instant.parse("2026-10-18T18:59:50Z");
    @Autowired BattleProgressService service;
    @Autowired MockMvc mvc;
    @Autowired JwtProvider jwt;
    @Autowired BattleProgressPolicyService policies;
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
        definitions.deleteAll();
        policies.save(901, policy(0));
        quests.saveQuest(
                "daily.fixture.kill",
                new QuestConfigRequest(QuestPeriod.DAILY, QuestKind.KILL_MONSTERS, 100, 17));
        battle = battles.createBattle(user, UUID.randomUUID().toString()).getBattleId();
        service.open(user, battle, 901);
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
        var changed = new ProgressReport(battle, request.reportId(), 0, 4, 1, 1, Set.of(1L, 2L));
        assertThatThrownBy(() -> service.report(user, changed))
                .isInstanceOf(ResponseStatusException.class);
        assertThat(service.snapshot(user, battle).progress().revision()).isEqualTo(1);
    }

    @Test
    void invalidCatalogTimeWaveAndRollbackReportsLeaveNoReceiptOrProgress() {
        var absent =
                new ProgressReport(battle, UUID.randomUUID().toString(), 0, 3, 1, 1, Set.of(999L));
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
        assertThatThrownBy(() -> service.open(user, next, 901))
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
                Set.of(1L, 2L));
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
