package com.novaserver.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.novaserver.item.entity.Item;
import com.novaserver.item.repository.ItemRepository;
import com.novaserver.playerlevel.dto.PlayerLevelConfigRequest;
import com.novaserver.playerlevel.entity.PlayerLevelConfig;
import com.novaserver.playerlevel.repository.PlayerLevelConfigRepository;
import com.novaserver.playerlevel.service.PlayerLevelService;
import com.novaserver.skill.entity.Skill;
import com.novaserver.skill.entity.SkillCastType;
import com.novaserver.skill.entity.SkillProjectilePath;
import com.novaserver.upgrade.entity.Upgrade;
import com.novaserver.upgrade.repository.UpgradeRepository;
import com.novaserver.upgrade.service.UpgradeService;
import com.novaserver.user.Provider;
import com.novaserver.user.dto.UserUpgradeCreateRequest;
import com.novaserver.user.entity.User;
import com.novaserver.user.entity.UserItem;
import com.novaserver.user.repository.UserDataRepository;
import com.novaserver.user.repository.UserItemRepository;
import com.novaserver.user.repository.UserRepository;
import com.novaserver.user.repository.UserUpgradeRequestRepository;
import com.novaserver.user.repository.WalletRepository;
import com.novaserver.version.entity.DataTable;
import com.novaserver.version.entity.DataVersion;
import com.novaserver.version.repository.DataVersionRepository;
import jakarta.persistence.EntityManager;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

@SpringBootTest
class UserUpgradeTransactionTest {
    @Autowired private UserUpgradeService purchases;
    @Autowired private UserService users;
    @Autowired private UserRepository userRepository;
    @Autowired private UserDataRepository dataRepository;
    @Autowired private WalletRepository wallets;
    @Autowired private UserItemRepository items;
    @Autowired private UserUpgradeRequestRepository requests;
    @Autowired private UpgradeRepository upgrades;
    @Autowired private ItemRepository catalogItems;
    @Autowired private PlatformTransactionManager transactionManager;

    @Autowired private PlayerLevelService playerLevels;
    @Autowired private PlayerLevelConfigRepository levelConfigs;
    @Autowired private DataVersionRepository versions;

    @Autowired private EntityManager entityManager;
    @Autowired private UpgradeService upgradeCatalog;
    private final List<Long> skillIds = new ArrayList<>();
    private Long castTypeId;
    private Long pathId;

    private TransactionTemplate transaction;
    private Long userId;
    private String upgradeId;
    private String itemId;

    @BeforeEach
    void setUp() {
        transaction = new TransactionTemplate(transactionManager);
        upgradeId = "test." + UUID.randomUUID();
        itemId = "mat." + UUID.randomUUID();
        // 테스트 전용 곡선. 실제 운영 밸런스를 복사하거나 고정하지 않는다.
        playerLevels.saveConfig(new PlayerLevelConfigRequest(10, 5, 4));
        int initialExp = 9;
        transaction.executeWithoutResult(
                status -> {
                    User user = userRepository.saveAndFlush(new User(Provider.GUEST, upgradeId));
                    userId = user.getId();
                    users.createInitialState(user);
                    dataRepository.findByUserId(userId).orElseThrow().addExp(initialExp);
                    wallets.findByUserId(userId).orElseThrow().addGold(100);
                    Item material = catalogItems.saveAndFlush(new Item(itemId, null));
                    upgrades.saveAndFlush(
                            new Upgrade(
                                    upgradeId,
                                    2,
                                    2,
                                    material,
                                    900,
                                    900,
                                    900,
                                    900,
                                    null,
                                    List.of(11, 19),
                                    List.of(2, 3)));
                    items.saveAndFlush(new UserItem(user, material, 20));
                });
    }

    @AfterEach
    void tearDown() {
        if (userId != null) {
            transaction.executeWithoutResult(
                    status -> {
                        for (Long id : skillIds) {
                            entityManager.remove(entityManager.getReference(Skill.class, id));
                        }
                        entityManager.flush();
                        if (castTypeId != null) {
                            entityManager.remove(
                                    entityManager.getReference(SkillCastType.class, castTypeId));
                            entityManager.remove(
                                    entityManager.getReference(SkillProjectilePath.class, pathId));
                        }
                        users.deleteAccount(userId);
                        upgrades.deleteById(upgradeId);
                        catalogItems.deleteById(itemId);
                        levelConfigs.deleteAll();
                    });
        }
    }

    @Test
    @DisplayName("해금 경계에서 구매·원래 결과 재전송과 새 상태 조회가 영구 저장 데이터와 일치한다")
    void shouldPersistPurchaseAndReplayOriginalResultAfterLaterPurchase() {
        // Given / When: 기준 데이터의 첫 레벨 경계 직전에는 차감하지 않는다.
        assertThatThrownBy(() -> purchases.levelUp(userId, request("first")))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("아직 해금되지 않은 강화");
        assertState(100, 20, 0);
        unlock();

        // Then: 새 트랜잭션에서 JSON 배열을 읽고 최초·마지막 구매 비용을 각각 차감한다.
        assertThat(purchases.levelUp(userId, request("first")).getLevel()).isEqualTo(1);
        assertState(89, 18, 1);
        assertThat(purchases.levelUp(userId, request("last")).getLevel()).isEqualTo(2);
        assertThat(purchases.levelUp(userId, request("first")).getLevel()).isEqualTo(1);
        assertState(70, 15, 2);
        assertThatThrownBy(() -> purchases.levelUp(userId, request("over-max")))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("이미 최대 레벨");
        assertState(70, 15, 2);
    }

    @ParameterizedTest
    @CsvSource({"10,20", "100,1"})
    @DisplayName("골드 또는 재료가 부족하면 실제 DB의 잔액·재료·강화·요청 기록을 유지한다")
    void shouldPreserveDatabaseOnInsufficientBalance(int gold, int material) {
        // Given
        unlock();
        transaction.executeWithoutResult(
                status -> {
                    wallets.findByUserId(userId).orElseThrow().useGold(100 - gold);
                    items.findByUserIdAndItemId(userId, itemId)
                            .orElseThrow()
                            .useQuantity(20 - material);
                });

        // When / Then
        assertThatThrownBy(() -> purchases.levelUp(userId, request("rejected")))
                .isInstanceOf(ResponseStatusException.class);
        assertState(gold, material, 0);
        assertThat(requests.findByUserIdAndUpgradeIdAndCreateId(userId, upgradeId, "rejected"))
                .isEmpty();
    }

    @ParameterizedTest
    @CsvSource({"same,same,89,18,1", "first,second,70,15,2"})
    @DisplayName("동시 동일 요청은 1회만, 서로 다른 요청은 직렬화하여 각각 1회씩 구매한다")
    void shouldSerializeConcurrentPurchases(
            String first, String second, int gold, int material, int level) throws Exception {
        // Given
        unlock();
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            Callable<Integer> one = () -> buyAfterStart(first, ready, start);
            Callable<Integer> two = () -> buyAfterStart(second, ready, start);
            var a = executor.submit(one);
            var b = executor.submit(two);
            try {
                assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();

                // When
                start.countDown();
                List<Integer> results =
                        List.of(a.get(15, TimeUnit.SECONDS), b.get(15, TimeUnit.SECONDS));

                // Then
                assertThat(results).containsExactlyInAnyOrder(1, level);
            } finally {
                start.countDown();
                executor.shutdownNow();
            }
        }
        assertState(gold, material, level);
    }

    @Test
    @DisplayName("DB 기준 교체는 조회 값과 PlayerLevels·전체 버전을 함께 변경한다")
    void shouldPublishStoredConfigWithIncrementedVersion() {
        int previous = playerLevels.getConfig().version();
        int revision = versions.findById(DataVersion.REVISION_KEY).orElseThrow().getVersion();

        var saved = playerLevels.saveConfig(new PlayerLevelConfigRequest(17, 3, 6));

        assertThat(saved.baseExp()).isEqualTo(17);
        assertThat(saved.stepExp()).isEqualTo(3);
        assertThat(saved.maxLevel()).isEqualTo(6);
        assertThat(saved.version()).isEqualTo(previous + 1);
        assertThat(versions.findById(DataVersion.REVISION_KEY).orElseThrow().getVersion())
                .isEqualTo(revision + 1);
        assertThat(versions.findById(DataTable.PLAYER_LEVELS.getKey()).orElseThrow().getVersion())
                .isEqualTo(saved.version());
        assertThat(playerLevels.getConfig()).isEqualTo(saved);
    }

    @Test
    @DisplayName("경험치 기준이 미등록이면 조회·구매를 거절하고 재화·진행을 유지한다")
    void shouldRejectPurchaseUntilLevelConfigIsInitialized() {
        transaction.executeWithoutResult(
                status -> levelConfigs.deleteById(PlayerLevelConfig.CONFIG_ID));

        assertThatThrownBy(() -> playerLevels.getConfig())
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("503");
        assertThatThrownBy(() -> purchases.levelUp(userId, request("no-config")))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("503");
        assertState(100, 20, 0);
        assertThat(requests.findByUserIdAndUpgradeIdAndCreateId(userId, upgradeId, "no-config"))
                .isEmpty();
    }

    @Test
    @DisplayName("DB 스킬 집계의 실제 해금 레벨과 구매 검증이 같은 누적 경험치 경계를 사용한다")
    void shouldUseEarliestPersistedSkillUnlockForCatalogAndPurchase() {
        transaction.executeWithoutResult(
                status -> {
                    var cast =
                            new SkillCastType(
                                    UUID.randomUUID().toString().substring(0, 12), "test");
                    var path =
                            new SkillProjectilePath(
                                    UUID.randomUUID().toString().substring(0, 12), "test");
                    entityManager.persist(cast);
                    entityManager.persist(path);
                    castTypeId = cast.getId();
                    pathId = path.getId();
                    for (int unlock : List.of(4, 3)) {
                        var skill =
                                new Skill(
                                        upgradeId + unlock,
                                        "test",
                                        cast,
                                        path,
                                        null,
                                        3,
                                        false,
                                        null,
                                        unlock,
                                        upgradeId);
                        entityManager.persist(skill);
                        skillIds.add(skill.getId());
                    }
                    dataRepository.findByUserId(userId).orElseThrow().addExp(15);
                });

        var quote =
                upgradeCatalog.getUpgradeList().stream()
                        .filter(row -> row.upgradeId().equals(upgradeId))
                        .findFirst()
                        .orElseThrow();
        assertThat(quote.unlockLevel()).isEqualTo(2);
        assertThat(quote.effectiveUnlockLevel()).isEqualTo(3);
        assertThatThrownBy(() -> purchases.levelUp(userId, request("skill")))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("아직 해금되지 않은 강화");
        assertState(100, 20, 0);
        unlock();
        assertThat(purchases.levelUp(userId, request("skill")).getLevel()).isEqualTo(1);
        assertState(89, 18, 1);
    }

    @Test
    @DisplayName("보상이 정수 상한을 넘어도 저장 경험치가 음수로 돌아가거나 해금이 다시 잠기지 않는다")
    void shouldKeepUnlockWhenAwardCrossesExperienceStorageLimit() {
        playerLevels.saveConfig(new PlayerLevelConfigRequest(Integer.MAX_VALUE, 0, 2));
        transaction.executeWithoutResult(
                status -> {
                    var data = dataRepository.findByUserId(userId).orElseThrow();
                    data.addExp(Integer.MAX_VALUE - 10);
                    data.addExp(2);
                });

        assertThat(users.getMe(userId).getExp()).isEqualTo(Integer.MAX_VALUE);
        assertThat(purchases.levelUp(userId, request("xp-cap")).getLevel()).isEqualTo(1);
        assertState(89, 18, 1);
    }

    private int buyAfterStart(String key, CountDownLatch ready, CountDownLatch start)
            throws InterruptedException {
        ready.countDown();
        if (!start.await(5, TimeUnit.SECONDS)) {
            throw new IllegalStateException("동시 요청 시작 시간 초과");
        }
        return purchases.levelUp(userId, request(key)).getLevel();
    }

    private void unlock() {
        transaction.executeWithoutResult(
                status -> dataRepository.findByUserId(userId).orElseThrow().addExp(1));
    }

    private UserUpgradeCreateRequest request(String key) {
        UserUpgradeCreateRequest request = new UserUpgradeCreateRequest();
        ReflectionTestUtils.setField(request, "upgradeId", upgradeId);
        ReflectionTestUtils.setField(request, "createId", key);
        return request;
    }

    private void assertState(int gold, int material, int level) {
        var snapshot = users.getMe(userId);
        assertThat(snapshot.getGold()).isEqualTo(gold);
        assertThat(snapshot.getItems())
                .singleElement()
                .satisfies(item -> assertThat(item.getQuantity()).isEqualTo(material));
        if (level == 0) {
            assertThat(snapshot.getUpgrades()).isEmpty();
        } else {
            assertThat(snapshot.getUpgrades())
                    .singleElement()
                    .satisfies(
                            upgrade -> {
                                assertThat(upgrade.getUpgradeId()).isEqualTo(upgradeId);
                                assertThat(upgrade.getLevel()).isEqualTo(level);
                            });
        }
    }
}
