package com.novaserver.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.novaserver.condex.entity.Codex;
import com.novaserver.global.error.ErrorCode;
import com.novaserver.item.entity.Item;
import com.novaserver.stage.entity.Stage;
import com.novaserver.tutorial.entity.Tutorials;
import com.novaserver.upgrade.entity.Upgrade;
import com.novaserver.user.dto.UserDataResponse;
import com.novaserver.user.entity.*;
import com.novaserver.user.repository.*;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    private static final Long USER_ID = 1L;

    @Mock private UserDataRepository userDataRepository;
    @Mock private WalletRepository walletRepository;
    @Mock private UserEnergyRepository userEnergyRepository;
    @Mock private UserStageProgressRepository userStageProgressRepository;
    @Mock private UserUpgradeRepository userUpgradeRepository;
    @Mock private UserItemRepository userItemRepository;
    @Mock private UserCodexRepository userCodexRepository;
    @Mock private UserTutorialRepository userTutorialRepository;

    @InjectMocks private UserService userService;

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
        assertThat(walletCaptor.getValue().getJem()).isZero();
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
                userItemRepository,
                userCodexRepository,
                userTutorialRepository);
    }

    @Test
    @DisplayName("유저 id로 상태를 조회하면 재화·경험치·에너지와 목록들을 모두 합쳐 반환한다")
    void shouldReturnAggregatedStatus_onGetMe() {
        // given
        UserData userData = new UserData(null, 120);
        Wallet wallet = new Wallet(null, 500, 10);
        UserEnergy energy = new UserEnergy(null, 25);
        LocalDateTime energyUpdatedAt = LocalDateTime.of(2026, 10, 14, 12, 0);
        ReflectionTestUtils.setField(energy, "energyUpdatedAt", energyUpdatedAt);

        Stage stage = mock(Stage.class);
        when(stage.getId()).thenReturn(10L);
        UserStageProgress stageProgress = new UserStageProgress(null, stage);
        stageProgress.updateClearRating(3);
        stageProgress.updateClaimedRating(2);

        Upgrade upgrade = mock(Upgrade.class);
        when(upgrade.getId()).thenReturn("atk");
        UserUpgrade userUpgrade = mock(UserUpgrade.class);
        when(userUpgrade.getUpgrade()).thenReturn(upgrade);
        when(userUpgrade.getLevel()).thenReturn(3);

        Item item = mock(Item.class);
        when(item.getId()).thenReturn(30L);
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

        when(userDataRepository.findByUser_Id(USER_ID)).thenReturn(Optional.of(userData));
        when(walletRepository.findByUser_Id(USER_ID)).thenReturn(Optional.of(wallet));
        when(userEnergyRepository.findByUser_Id(USER_ID)).thenReturn(Optional.of(energy));
        when(userStageProgressRepository.findByUser_Id(USER_ID)).thenReturn(List.of(stageProgress));
        when(userUpgradeRepository.findByUser_Id(USER_ID)).thenReturn(List.of(userUpgrade));
        when(userItemRepository.findByUser_Id(USER_ID)).thenReturn(List.of(userItem));
        when(userCodexRepository.findByUser_Id(USER_ID)).thenReturn(List.of(userCodex));
        when(userTutorialRepository.findByUser_Id(USER_ID)).thenReturn(List.of(userTutorial));

        // when
        UserDataResponse response = userService.getMe(USER_ID);

        // then
        assertThat(response.getGold()).isEqualTo(500);
        assertThat(response.getJem()).isEqualTo(10);
        assertThat(response.getExp()).isEqualTo(120);
        assertThat(response.getEnergyStored()).isEqualTo(25);
        assertThat(response.getEnergyUpdatedAt()).isEqualTo(energyUpdatedAt);

        assertThat(response.getStageProgress()).hasSize(1);
        assertThat(response.getStageProgress().get(0).getStageId()).isEqualTo(10L);
        assertThat(response.getStageProgress().get(0).getClearRating()).isEqualTo(3);
        assertThat(response.getStageProgress().get(0).getClaimedRating()).isEqualTo(2);

        assertThat(response.getUpgrades()).hasSize(1);
        assertThat(response.getUpgrades().get(0).getUpgradeId()).isEqualTo("atk");
        assertThat(response.getUpgrades().get(0).getLevel()).isEqualTo(3);

        assertThat(response.getItems()).hasSize(1);
        assertThat(response.getItems().get(0).getItemId()).isEqualTo(30L);
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
        when(userDataRepository.findByUser_Id(USER_ID)).thenReturn(Optional.empty());

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
        when(userDataRepository.findByUser_Id(USER_ID))
                .thenReturn(Optional.of(new UserData(null, 0)));
        when(walletRepository.findByUser_Id(USER_ID)).thenReturn(Optional.empty());

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
        when(userDataRepository.findByUser_Id(USER_ID))
                .thenReturn(Optional.of(new UserData(null, 0)));
        when(walletRepository.findByUser_Id(USER_ID))
                .thenReturn(Optional.of(new Wallet(null, 0, 0)));
        when(userEnergyRepository.findByUser_Id(USER_ID)).thenReturn(Optional.empty());

        // when & then
        assertStatus(
                () -> userService.getMe(USER_ID),
                HttpStatus.NOT_FOUND,
                ErrorCode.ENERGY_NOT_FOUND,
                USER_ID);
        verifyNoInteractions(
                userStageProgressRepository,
                userUpgradeRepository,
                userItemRepository,
                userCodexRepository,
                userTutorialRepository);
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
