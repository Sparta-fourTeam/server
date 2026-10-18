package com.novaserver.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.novaserver.battle.repository.BattleRepository;
import com.novaserver.battle.repository.BattleResultRepository;
import com.novaserver.condex.entity.Codex;
import com.novaserver.global.error.ErrorCode;
import com.novaserver.item.entity.Item;
import com.novaserver.stage.entity.Stage;
import com.novaserver.tutorial.entity.Tutorials;
import com.novaserver.upgrade.entity.Upgrade;
import com.novaserver.user.dto.UserDataResponse;
import com.novaserver.user.entity.User;
import com.novaserver.user.entity.UserCodex;
import com.novaserver.user.entity.UserData;
import com.novaserver.user.entity.UserEnergy;
import com.novaserver.user.entity.UserItem;
import com.novaserver.user.entity.UserStageProgress;
import com.novaserver.user.entity.UserTutorial;
import com.novaserver.user.entity.UserUpgrade;
import com.novaserver.user.entity.Wallet;
import com.novaserver.user.repository.UserCodexRepository;
import com.novaserver.user.repository.UserDataRepository;
import com.novaserver.user.repository.UserEnergyPurchaseStateRepository;
import com.novaserver.user.repository.UserEnergyRecoveryReceiptRepository;
import com.novaserver.user.repository.UserEnergyRepository;
import com.novaserver.user.repository.UserItemRepository;
import com.novaserver.user.repository.UserRepository;
import com.novaserver.user.repository.UserStageProgressRepository;
import com.novaserver.user.repository.UserTutorialRepository;
import com.novaserver.user.repository.UserUpgradeRepository;
import com.novaserver.user.repository.UserUpgradeRequestRepository;
import com.novaserver.user.repository.WalletRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    private static final Long USER_ID = 1L;
    private static final Instant NOW = Instant.parse("2026-10-14T12:00:00Z");

    @Mock private UserRepository userRepository;
    @Mock private UserDataRepository userDataRepository;
    @Mock private WalletRepository walletRepository;
    @Mock private UserEnergyRepository userEnergyRepository;
    @Mock private UserEnergyRecoveryReceiptRepository userEnergyRecoveryReceiptRepository;
    @Mock private UserEnergyPurchaseStateRepository userEnergyPurchaseStateRepository;
    @Mock private UserStageProgressRepository userStageProgressRepository;
    @Mock private UserUpgradeRepository userUpgradeRepository;
    @Mock private UserUpgradeRequestRepository userUpgradeRequestRepository;
    @Mock private UserItemRepository userItemRepository;
    @Mock private UserCodexRepository userCodexRepository;
    @Mock private UserTutorialRepository userTutorialRepository;
    @Mock private BattleRepository battleRepository;
    @Mock private BattleResultRepository battleResultRepository;

    private UserService userService;

    @BeforeEach
    void setUp() {
        ZoneId zone = ZoneId.systemDefault();
        Clock clock = Clock.fixed(NOW, zone);

        userService =
                new UserService(
                        userRepository,
                        userDataRepository,
                        walletRepository,
                        userEnergyRepository,
                        userEnergyRecoveryReceiptRepository,
                        userEnergyPurchaseStateRepository,
                        userStageProgressRepository,
                        userUpgradeRepository,
                        userUpgradeRequestRepository,
                        userItemRepository,
                        userCodexRepository,
                        userTutorialRepository,
                        battleRepository,
                        battleResultRepository,
                        clock);
    }

    @Test
    @DisplayName("가입 직후 유저 데이터·지갑·에너지를 초기값으로 저장한다")
    void shouldSaveInitialState_onCreateInitialState() {
        // given
        User user = mock(User.class);

        // when
        userService.createInitialState(user);

        // then
        ArgumentCaptor<UserData> dataCaptor = ArgumentCaptor.forClass(UserData.class);
        ArgumentCaptor<Wallet> walletCaptor = ArgumentCaptor.forClass(Wallet.class);
        ArgumentCaptor<UserEnergy> energyCaptor = ArgumentCaptor.forClass(UserEnergy.class);
        verify(userDataRepository).save(dataCaptor.capture());
        verify(walletRepository).save(walletCaptor.capture());
        verify(userEnergyRepository).save(energyCaptor.capture());

        assertThat(dataCaptor.getValue().getUser()).isEqualTo(user);
        assertThat(dataCaptor.getValue().getExp()).isZero();
        assertThat(walletCaptor.getValue().getUser()).isEqualTo(user);
        assertThat(walletCaptor.getValue().getGold()).isZero();
        assertThat(walletCaptor.getValue().getGem()).isZero();
        assertThat(energyCaptor.getValue().getUser()).isEqualTo(user);
        assertThat(energyCaptor.getValue().getEnergyStored()).isEqualTo(UserEnergy.MAX_ENERGY);
    }

    @Test
    @DisplayName("가입 초기화는 스테이지·강화·아이템·도감·튜토리얼 저장소를 건드리지 않는다")
    void shouldNotTouchOtherRepositories_onCreateInitialState() {
        // when
        userService.createInitialState(mock(User.class));

        // then
        verifyNoInteractions(
                userStageProgressRepository,
                userUpgradeRepository,
                userUpgradeRequestRepository,
                userItemRepository,
                userCodexRepository,
                userTutorialRepository);
    }

    @Test
    @DisplayName("계정을 삭제하면 유저에 속한 모든 데이터와 유저 자신을 지운다")
    void shouldDeleteAllUserDataAndUser_onDeleteAccount() {
        // when
        userService.deleteAccount(USER_ID);

        // then
        verify(userDataRepository).deleteByUserId(USER_ID);
        verify(walletRepository).deleteByUserId(USER_ID);
        verify(userEnergyRepository).deleteByUserId(USER_ID);
        verify(userStageProgressRepository).deleteByUserId(USER_ID);
        verify(userUpgradeRequestRepository).deleteByUserId(USER_ID);
        verify(userUpgradeRepository).deleteByUserId(USER_ID);
        verify(userItemRepository).deleteByUserId(USER_ID);
        verify(userCodexRepository).deleteByUserId(USER_ID);
        verify(userTutorialRepository).deleteByUserId(USER_ID);
        verify(battleRepository).deleteById(USER_ID);
        verify(userRepository).deleteById(USER_ID);
    }

    @Test
    @DisplayName("유저 id로 상태를 조회하면 재화·경험치·에너지와 목록들을 모두 합쳐 반환한다")
    void shouldReturnAggregatedStatus_onGetMe() {
        // given
        UserEnergy energy = new UserEnergy(null, 25, NOW);
        Instant energyUpdatedAt = NOW;
        ReflectionTestUtils.setField(energy, "energyUpdatedAt", energyUpdatedAt);

        Stage stage = mock(Stage.class);
        when(stage.getStageId()).thenReturn(10);
        UserStageProgress stageProgress = new UserStageProgress(null, stage);
        stageProgress.updateClearRating(3);
        stageProgress.updateClaimedRating(2);

        Upgrade upgrade = mock(Upgrade.class);
        when(upgrade.getUpgradeId()).thenReturn("atk");
        UserUpgrade userUpgrade = mock(UserUpgrade.class);
        when(userUpgrade.getUpgrade()).thenReturn(upgrade);
        when(userUpgrade.getLevel()).thenReturn(3);

        Item item = mock(Item.class);
        when(item.getItemId()).thenReturn("book.hat");
        UserItem userItem = mock(UserItem.class);
        when(userItem.getItem()).thenReturn(item);
        when(userItem.getQuantity()).thenReturn(5);

        Codex codex = mock(Codex.class);
        when(codex.getId()).thenReturn(40L);
        UserCodex userCodex = mock(UserCodex.class);
        when(userCodex.getCodex()).thenReturn(codex);
        when(userCodex.isClaimed()).thenReturn(true);

        Tutorials tutorials = mock(Tutorials.class);
        when(tutorials.getId()).thenReturn(50L);
        UserTutorial userTutorial = mock(UserTutorial.class);
        when(userTutorial.getTutorials()).thenReturn(tutorials);

        UserData userData = new UserData(null, 120);
        when(userDataRepository.findByUserId(USER_ID)).thenReturn(Optional.of(userData));
        Wallet wallet = new Wallet(null, 500, 10);
        when(walletRepository.findByUserId(USER_ID)).thenReturn(Optional.of(wallet));
        when(userEnergyRepository.findByUserId(USER_ID)).thenReturn(Optional.of(energy));
        when(userStageProgressRepository.findByUserId(USER_ID)).thenReturn(List.of(stageProgress));
        when(userUpgradeRepository.findByUserId(USER_ID)).thenReturn(List.of(userUpgrade));
        when(userItemRepository.findByUserId(USER_ID)).thenReturn(List.of(userItem));
        when(userCodexRepository.findByUserId(USER_ID)).thenReturn(List.of(userCodex));
        when(userTutorialRepository.findByUserId(USER_ID)).thenReturn(List.of(userTutorial));

        // when
        UserDataResponse response = userService.getMe(USER_ID);

        // then
        assertThat(response.getGold()).isEqualTo(500);
        assertThat(response.getGem()).isEqualTo(10);
        assertThat(response.getExp()).isEqualTo(120);
        assertThat(response.getEnergyStored()).isEqualTo(25);
        assertThat(response.getEnergyUpdatedAt()).isEqualTo(energyUpdatedAt);

        assertThat(response.getStageProgress()).hasSize(1);
        assertThat(response.getStageProgress().get(0).getStageId()).isEqualTo(10);
        assertThat(response.getStageProgress().get(0).getClearRating()).isEqualTo(3);
        assertThat(response.getStageProgress().get(0).getClaimedRating()).isEqualTo(2);

        assertThat(response.getUpgrades()).hasSize(1);
        assertThat(response.getUpgrades().get(0).getUpgradeId()).isEqualTo("atk");
        assertThat(response.getUpgrades().get(0).getLevel()).isEqualTo(3);

        assertThat(response.getItems()).hasSize(1);
        assertThat(response.getItems().get(0).getItemId()).isEqualTo("book.hat");
        assertThat(response.getItems().get(0).getQuantity()).isEqualTo(5);

        assertThat(response.getCodex()).hasSize(1);
        assertThat(response.getCodex().get(0).getMonsterId()).isEqualTo(40L);
        assertThat(response.getCodex().get(0).isClaimed()).isTrue();

        assertThat(response.getTutorials()).containsExactly(50L);
    }

    @Test
    @DisplayName("유저 데이터가 없으면 404 USER_DATA_NOT_FOUND를 던지고 지갑·에너지는 조회하지 않는다")
    void shouldThrowNotFound_whenUserDataMissing() {
        // given
        when(userDataRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());

        // when & then
        assertStatus(
                () -> userService.getMe(USER_ID),
                HttpStatus.NOT_FOUND,
                ErrorCode.USER_DATA_NOT_FOUND,
                USER_ID);
        verifyNoInteractions(walletRepository, userEnergyRepository);
    }

    @Test
    @DisplayName("지갑이 없으면 404 WALLET_NOT_FOUND를 던지고 에너지는 조회하지 않는다")
    void shouldThrowNotFound_whenWalletMissing() {
        // given
        when(userDataRepository.findByUserId(USER_ID))
                .thenReturn(Optional.of(new UserData(null, 0)));
        when(walletRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());

        // when & then
        assertStatus(
                () -> userService.getMe(USER_ID),
                HttpStatus.NOT_FOUND,
                ErrorCode.WALLET_NOT_FOUND,
                USER_ID);
        verifyNoInteractions(userEnergyRepository);
    }

    @Test
    @DisplayName("에너지가 없으면 404 ENERGY_NOT_FOUND를 던지고 나머지 목록은 조회하지 않는다")
    void shouldThrowNotFound_whenEnergyMissing() {
        // given
        when(userDataRepository.findByUserId(USER_ID))
                .thenReturn(Optional.of(new UserData(null, 0)));
        when(walletRepository.findByUserId(USER_ID))
                .thenReturn(Optional.of(new Wallet(null, 0, 0)));
        when(userEnergyRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());

        // when & then
        assertStatus(
                () -> userService.getMe(USER_ID),
                HttpStatus.NOT_FOUND,
                ErrorCode.ENERGY_NOT_FOUND,
                USER_ID);
        verifyNoInteractions(
                userStageProgressRepository,
                userUpgradeRepository,
                userUpgradeRequestRepository,
                userItemRepository,
                userCodexRepository,
                userTutorialRepository);
    }

    @Test
    @DisplayName("상태를 조회하면 지난 시간만큼 회복된 에너지를 반영한다")
    void shouldReflectRecoveredEnergy_onGetMe() {
        // given
        UserEnergy energy = energyOf(10, NOW.minusSeconds(3000));
        when(userDataRepository.findByUserId(USER_ID))
                .thenReturn(Optional.of(new UserData(null, 0)));
        when(walletRepository.findByUserId(USER_ID))
                .thenReturn(Optional.of(new Wallet(null, 0, 0)));
        when(userEnergyRepository.findByUserId(USER_ID)).thenReturn(Optional.of(energy));

        // when
        UserDataResponse response = userService.getMe(USER_ID);

        // then
        assertThat(response.getEnergyStored()).isEqualTo(15);
        assertThat(response.getEnergyUpdatedAt()).isEqualTo(NOW);
    }

    @Test
    @DisplayName("에너지를 소모하면 회복분을 먼저 반영한 뒤 차감한다")
    void shouldRecoverThenConsume_onUseEnergy() {
        // given
        UserEnergy energy = energyOf(10, NOW.minusSeconds(3000));
        when(userEnergyRepository.findByUserIdForUpdate(USER_ID)).thenReturn(Optional.of(energy));

        // when
        userService.useEnergy(USER_ID, 5);

        // then: 10 + 회복 5 - 소모 5
        assertThat(energy.getEnergyStored()).isEqualTo(10);
        assertThat(energy.getEnergyUpdatedAt()).isEqualTo(NOW);
    }

    @Test
    @DisplayName("최대치에서 에너지를 소모하면 회복 타이머가 지금부터 시작된다")
    void shouldStartRecoveryTimerNow_whenConsumeFromFull() {
        // given
        UserEnergy energy = energyOf(UserEnergy.MAX_ENERGY, NOW.minusSeconds(259200));
        when(userEnergyRepository.findByUserIdForUpdate(USER_ID)).thenReturn(Optional.of(energy));

        // when
        userService.useEnergy(USER_ID, 5);

        // then
        assertThat(energy.getEnergyStored()).isEqualTo(UserEnergy.MAX_ENERGY - 5);
        assertThat(energy.getEnergyUpdatedAt()).isEqualTo(NOW);
    }

    @Test
    @DisplayName("에너지가 부족하면 409 INSUFFICIENT_ENERGY를 던지고 에너지는 그대로다")
    void shouldThrowConflict_whenEnergyInsufficient() {
        // given
        UserEnergy energy = energyOf(3, NOW);
        when(userEnergyRepository.findByUserIdForUpdate(USER_ID)).thenReturn(Optional.of(energy));

        // when & then
        assertStatus(
                () -> userService.useEnergy(USER_ID, 5),
                HttpStatus.CONFLICT,
                ErrorCode.INSUFFICIENT_ENERGY);
        assertThat(energy.getEnergyStored()).isEqualTo(3);
    }

    @Test
    @DisplayName("다른 트랜잭션이 에너지 행을 잠그고 있으면 409 ENERGY_LOCK_CONFLICT를 던진다")
    void shouldThrowConflict_whenEnergyRowLockedByAnotherTransaction() {
        // given
        when(userEnergyRepository.findByUserIdForUpdate(USER_ID))
                .thenThrow(new CannotAcquireLockException("lock not available"));

        // when & then
        assertStatus(
                () -> userService.useEnergy(USER_ID, 5),
                HttpStatus.CONFLICT,
                ErrorCode.ENERGY_LOCK_CONFLICT);
    }

    @Test
    @DisplayName("에너지 소모 시 에너지가 없으면 404 ENERGY_NOT_FOUND를 던진다")
    void shouldThrowNotFound_onUseEnergy_whenEnergyMissing() {
        // given
        when(userEnergyRepository.findByUserIdForUpdate(USER_ID)).thenReturn(Optional.empty());

        // when & then
        assertStatus(
                () -> userService.useEnergy(USER_ID, 5),
                HttpStatus.NOT_FOUND,
                ErrorCode.ENERGY_NOT_FOUND,
                USER_ID);
    }

    @Test
    @DisplayName("에너지 보상을 지급하면 에너지가 늘어난다")
    void shouldIncreaseEnergy_onGrantEnergy() {
        // given
        UserEnergy energy = energyOf(10, NOW);
        when(userEnergyRepository.findByUserIdForUpdate(USER_ID)).thenReturn(Optional.of(energy));

        // when
        userService.grantEnergy(USER_ID, 7);

        // then
        assertThat(energy.getEnergyStored()).isEqualTo(17);
    }

    @Test
    @DisplayName("최대치 미만이면 보상을 받아도 회복 타이머는 초기화되지 않는다")
    void shouldKeepRecoveryTimer_onGrantEnergy_whenBelowMax() {
        // given: 50분 30초 경과
        UserEnergy energy = energyOf(10, NOW.minusSeconds(3000).minusSeconds(30));
        when(userEnergyRepository.findByUserIdForUpdate(USER_ID)).thenReturn(Optional.of(energy));

        // when
        userService.grantEnergy(USER_ID, 7);

        // then: 10 + 회복 5 + 보상 7, 남은 30초는 그대로 보존
        assertThat(energy.getEnergyStored()).isEqualTo(22);
        assertThat(energy.getEnergyUpdatedAt()).isEqualTo(NOW.minusSeconds(30));
    }

    @Test
    @DisplayName("보상으로 최대치를 넘겨도 밀려 있던 회복분이 사라지지 않는다")
    void shouldKeepPendingRecovery_onGrantEnergy_whenExceedMax() {
        // given
        UserEnergy energy = energyOf(20, NOW.minusSeconds(3000));
        when(userEnergyRepository.findByUserIdForUpdate(USER_ID)).thenReturn(Optional.of(energy));

        // when
        userService.grantEnergy(USER_ID, 15);

        // then: 20 + 회복 5 + 보상 15
        assertThat(energy.getEnergyStored()).isEqualTo(40);
        assertThat(energy.getEnergyUpdatedAt()).isEqualTo(NOW);
    }

    @Test
    @DisplayName("보상 지급 시 에너지가 없으면 404 ENERGY_NOT_FOUND를 던진다")
    void shouldThrowNotFound_onGrantEnergy_whenEnergyMissing() {
        // given
        when(userEnergyRepository.findByUserIdForUpdate(USER_ID)).thenReturn(Optional.empty());

        // when & then
        assertStatus(
                () -> userService.grantEnergy(USER_ID, 5),
                HttpStatus.NOT_FOUND,
                ErrorCode.ENERGY_NOT_FOUND,
                USER_ID);
    }

    private static UserEnergy energyOf(int stored, Instant updatedAt) {
        UserEnergy energy = new UserEnergy(null, stored, NOW);
        ReflectionTestUtils.setField(energy, "energyUpdatedAt", updatedAt);
        return energy;
    }

    private static void assertStatus(
            ThrowingCallable call, HttpStatus status, ErrorCode errorCode, Object... args) {
        assertThatThrownBy(call)
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(
                        e -> {
                            ResponseStatusException ex = (ResponseStatusException) e;
                            assertThat(ex.getStatusCode()).isEqualTo(status);
                            assertThat(ex.getReason())
                                    .isEqualTo(errorCode.getMessage().formatted(args));
                        });
    }
}
