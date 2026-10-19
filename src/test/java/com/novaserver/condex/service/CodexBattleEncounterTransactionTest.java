package com.novaserver.condex.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.novaserver.battle.progress.AcceptedBattleEncounters;
import com.novaserver.battle.progress.BattleProgressPolicyService;
import com.novaserver.battle.progress.BattleProgressReceiptRepository;
import com.novaserver.battle.progress.BattleProgressService;
import com.novaserver.battle.progress.BattleSourceAdminService;
import com.novaserver.battle.progress.BattleSourceFixture;
import com.novaserver.battle.progress.BattleSourceRepository;
import com.novaserver.battle.progress.ProgressReport;
import com.novaserver.battle.progress.StageProgressPolicy;
import com.novaserver.battle.progress.StageProgressPolicyRepository;
import com.novaserver.battle.service.BattleService;
import com.novaserver.condex.entity.Codex;
import com.novaserver.condex.entity.CodexGradeReward;
import com.novaserver.condex.entity.MonsterGrade;
import com.novaserver.condex.repository.CodexGradeRewardRepository;
import com.novaserver.condex.repository.CodexRepository;
import com.novaserver.reward.entity.RewardDefinition;
import com.novaserver.reward.repository.RewardDefinitionRepository;
import com.novaserver.stage.entity.Stage;
import com.novaserver.stage.repository.StageRepository;
import com.novaserver.user.Provider;
import com.novaserver.user.entity.User;
import com.novaserver.user.entity.UserStageProgress;
import com.novaserver.user.repository.UserCodexRepository;
import com.novaserver.user.repository.UserRepository;
import com.novaserver.user.repository.UserStageProgressRepository;
import com.novaserver.user.repository.WalletRepository;
import com.novaserver.user.service.UserService;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

@SpringBootTest
class CodexBattleEncounterTransactionTest {
    private static final int FIRST_STAGE = 902;
    private static final int SECOND_STAGE = 903;
    private static final long FIRST_MONSTER = 88001L;
    private static final long SECOND_MONSTER = 88002L;
    private static final Instant START = Instant.parse("2026-10-19T01:00:00Z");

    @Autowired private BattleProgressService progress;
    @Autowired private BattleProgressPolicyService policies;
    @Autowired private BattleSourceAdminService publisher;
    @Autowired private BattleSourceRepository sourceRows;
    @Autowired private StageProgressPolicyRepository stagePolicies;
    @Autowired private BattleProgressReceiptRepository receipts;
    @Autowired private BattleService battles;
    @Autowired private UserCodexService codex;
    @Autowired private UserCodexRepository states;
    @Autowired private UserRepository accounts;
    @Autowired private UserService users;
    @Autowired private StageRepository stages;
    @Autowired private CodexRepository monsters;
    @Autowired private CodexGradeRewardRepository gradeRewards;
    @Autowired private RewardDefinitionRepository rewards;
    @Autowired private UserStageProgressRepository unlocked;
    @Autowired private WalletRepository wallets;
    @Autowired private PlatformTransactionManager transactions;
    @Autowired private ApplicationEventPublisher events;
    @MockitoBean private Clock clock;
    private Long user;
    private Long rewardId;
    private String battle;

    @BeforeEach
    void prepare() throws Exception {
        when(clock.instant()).thenReturn(START);
        user =
                new TransactionTemplate(transactions)
                        .execute(
                                status -> {
                                    final var first =
                                            stages.saveAndFlush(Stage.create(FIRST_STAGE));
                                    final var second =
                                            stages.saveAndFlush(Stage.create(SECOND_STAGE));
                                    monsters.saveAllAndFlush(
                                            List.of(
                                                    new Codex(FIRST_MONSTER, MonsterGrade.NORMAL),
                                                    new Codex(
                                                            SECOND_MONSTER, MonsterGrade.NORMAL)));
                                    var reward =
                                            rewards.saveAndFlush(
                                                    new RewardDefinition(
                                                            "codex encounter fixture",
                                                            500,
                                                            Map.of()));
                                    rewardId = reward.getId();
                                    gradeRewards.saveAndFlush(
                                            new CodexGradeReward(MonsterGrade.NORMAL, reward));
                                    var account =
                                            accounts.saveAndFlush(
                                                    new User(
                                                            Provider.GUEST,
                                                            UUID.randomUUID().toString()));
                                    users.createInitialState(account);
                                    unlocked.saveAllAndFlush(
                                            List.of(
                                                    new UserStageProgress(account, first),
                                                    new UserStageProgress(account, second)));
                                    return account.getId();
                                });
        sourceRows.deleteAll();
        var json = new ObjectMapper();
        var monsterRows = (ArrayNode) json.readTree(BattleSourceFixture.monsters());
        for (long id : List.of(FIRST_MONSTER, SECOND_MONSTER)) {
            var row = (ObjectNode) monsterRows.get(0).deepCopy();
            row.put("Id", id);
            monsterRows.add(row);
        }
        publisher.publish(
                new BattleSourceAdminService.Publication(
                        BattleSourceFixture.stages(),
                        json.writeValueAsString(monsterRows),
                        BattleSourceFixture.combat(),
                        null));
        var policy =
                new StageProgressPolicy(
                        BigDecimal.ONE,
                        BigDecimal.ONE,
                        List.of(Map.of(FIRST_MONSTER, 1, SECOND_MONSTER, 1)),
                        Map.of(FIRST_MONSTER, List.of(), SECOND_MONSTER, List.of()),
                        0);
        policies.save(FIRST_STAGE, policy);
        policies.save(SECOND_STAGE, policy);
        battle = battles.createBattle(user, UUID.randomUUID().toString()).getBattleId();
        progress.open(user, battle, FIRST_STAGE, policies.sourceRevision());
        when(clock.instant()).thenReturn(START.plusSeconds(5));
    }

    @AfterEach
    void cleanUp() {
        if (accounts.existsById(user)) {
            users.deleteAccount(user);
        }
        stagePolicies.deleteById(FIRST_STAGE);
        stagePolicies.deleteById(SECOND_STAGE);
        gradeRewards.deleteById(MonsterGrade.NORMAL);
        rewards.deleteById(rewardId);
        monsters.deleteAllById(List.of(FIRST_MONSTER, SECOND_MONSTER));
        stages.deleteById(FIRST_STAGE);
        stages.deleteById(SECOND_STAGE);
        sourceRows.deleteAll();
    }

    @Test
    void possibleMonsterReportCannotUnlockDiscoveryOrClaim() {
        var request = report(Set.of(FIRST_MONSTER));
        assertThat(progress.report(user, request).acceptedDelta()).isZero();
        assertThat(receipts.findById(request.reportId())).isPresent();
        assertThat(states.findByUserId(user)).isEmpty();
        assertThatThrownBy(() -> codex.claim(user, FIRST_MONSTER))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("409");
        assertThat(wallets.findByUserId(user).orElseThrow().getGold()).isZero();
    }

    @Test
    void missingCodexCatalogMonsterDoesNotRollBackBattleProgress() {
        monsters.deleteById(SECOND_MONSTER);
        var request = report(Set.of(FIRST_MONSTER, SECOND_MONSTER));
        assertThat(progress.report(user, request).revision()).isEqualTo(1);
        assertThat(receipts.findById(request.reportId())).isPresent();
        assertThat(states.findByUserId(user)).isEmpty();
    }

    @Test
    void sameMonsterAcrossStagesKeepsOneTrustedDiscoveryAndClaim() {
        final var original = codex.recordVerifiedEncounter(user, FIRST_MONSTER);
        final var receipt = codex.claim(user, FIRST_MONSTER);
        assertThat(progress.report(user, report(Set.of(FIRST_MONSTER))).revision()).isEqualTo(1);

        battle = battles.createBattle(user, UUID.randomUUID().toString()).getBattleId();
        progress.open(user, battle, SECOND_STAGE, policies.sourceRevision());
        when(clock.instant()).thenReturn(START.plusSeconds(10));
        assertThat(progress.report(user, report(Set.of(FIRST_MONSTER))).revision()).isEqualTo(1);

        assertThat(codex.recordVerifiedEncounter(user, FIRST_MONSTER).discoveredAt())
                .isEqualTo(original.discoveredAt());
        assertThat(codex.claim(user, FIRST_MONSTER)).isEqualTo(receipt);
        assertThat(states.findByUserId(user)).hasSize(1);
        assertThat(wallets.findByUserId(user).orElseThrow().getGold()).isEqualTo(500);
    }

    @Test
    void acceptedButUnverifiedEventCannotCreateDiscovery() {
        events.publishEvent(
                new AcceptedBattleEncounters(
                        user,
                        battle,
                        UUID.randomUUID().toString(),
                        Set.of(FIRST_MONSTER),
                        clock.instant()));
        assertThat(states.findByUserId(user)).isEmpty();
    }

    private ProgressReport report(Set<Long> ids) {
        return new ProgressReport(
                battle, UUID.randomUUID().toString(), 0, 0, 0, 1, ids, policies.sourceRevision());
    }
}
