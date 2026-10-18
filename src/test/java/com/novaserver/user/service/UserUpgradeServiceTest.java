package com.novaserver.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.novaserver.global.error.ErrorCode;
import com.novaserver.item.entity.Item;
import com.novaserver.skill.entity.Skill;
import com.novaserver.skill.repository.SkillRepository;
import com.novaserver.upgrade.entity.Upgrade;
import com.novaserver.upgrade.service.UpgradeService;
import com.novaserver.user.dto.UserUpgradeCreateRequest;
import com.novaserver.user.dto.UserUpgradeResponse;
import com.novaserver.user.entity.User;
import com.novaserver.user.entity.UserData;
import com.novaserver.user.entity.UserItem;
import com.novaserver.user.entity.UserUpgrade;
import com.novaserver.user.entity.UserUpgradeRequest;
import com.novaserver.user.entity.Wallet;
import com.novaserver.user.repository.UserDataRepository;
import com.novaserver.user.repository.UserItemRepository;
import com.novaserver.user.repository.UserRepository;
import com.novaserver.user.repository.UserUpgradeRepository;
import com.novaserver.user.repository.UserUpgradeRequestRepository;
import com.novaserver.user.repository.WalletRepository;
import java.util.List;
import java.util.Optional;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class UserUpgradeServiceTest {

    private static final Long USER_ID = 1L;
    private static final String UPGRADE_ID = "atk";
    private static final String CREATE_ID = "create-1";

    @Mock private UserRepository userRepository;
    @Mock private WalletRepository walletRepository;
    @Mock private UserItemRepository userItemRepository;
    @Mock private UserUpgradeRepository userUpgradeRepository;
    @Mock private UserUpgradeRequestRepository userUpgradeRequestRepository;
    @Mock private UpgradeService upgradeService;
    @Mock private UserDataRepository userDataRepository;
    @Mock private SkillRepository skillRepository;
    @Mock private PlayerLevelPolicy playerLevelPolicy;

    @InjectMocks private UserUpgradeService userUpgradeService;

    private UserUpgradeCreateRequest request(String upgradeId, String createId) {
        UserUpgradeCreateRequest request = new UserUpgradeCreateRequest();
        ReflectionTestUtils.setField(request, "upgradeId", upgradeId);
        ReflectionTestUtils.setField(request, "createId", createId);
        return request;
    }

    @Test
    @DisplayName("같은 createId로 다시 요청하면 이전 결과를 그대로 돌려주고 아무것도 건드리지 않는다")
    void shouldReturnPreviousResult_whenRequestAlreadyProcessed() {
        UserUpgradeRequest existing = mock(UserUpgradeRequest.class);
        when(existing.getResultLevel()).thenReturn(5);
        when(userUpgradeRequestRepository.findByUserIdAndUpgradeIdAndCreateId(
                        USER_ID, UPGRADE_ID, CREATE_ID))
                .thenReturn(Optional.of(existing));

        UserUpgradeResponse response =
                userUpgradeService.levelUp(USER_ID, request(UPGRADE_ID, CREATE_ID));

        assertThat(response.getUpgradeId()).isEqualTo(UPGRADE_ID);
        assertThat(response.getLevel()).isEqualTo(5);
        verifyNoInteractions(
                upgradeService,
                userRepository,
                walletRepository,
                userItemRepository,
                userUpgradeRepository);
    }

    @Test
    @DisplayName("존재하지 않는 강화면 404를 던지고 재료는 조회하지 않는다")
    void shouldThrowNotFound_whenUpgradeMissing() {
        when(userUpgradeRequestRepository.findByUserIdAndUpgradeIdAndCreateId(
                        USER_ID, UPGRADE_ID, CREATE_ID))
                .thenReturn(Optional.empty());
        when(walletRepository.findByUserIdForUpdate(USER_ID))
                .thenReturn(Optional.of(new Wallet(mock(User.class), 0, 0)));
        when(upgradeService.findUpgrade(UPGRADE_ID))
                .thenThrow(ErrorCode.UPGRADE_NOT_FOUND.exception(UPGRADE_ID));

        assertStatus(
                () -> userUpgradeService.levelUp(USER_ID, request(UPGRADE_ID, CREATE_ID)),
                HttpStatus.NOT_FOUND,
                ErrorCode.UPGRADE_NOT_FOUND,
                UPGRADE_ID);
        verifyNoInteractions(userItemRepository);
    }

    @Test
    @DisplayName("이미 최대 레벨이면 409 UPGRADE_MAX_LEVEL을 던지고 재료는 건드리지 않는다")
    void shouldThrowConflict_whenAlreadyMaxLevel() {
        Upgrade upgrade =
                new Upgrade(UPGRADE_ID, 3, 1, null, 100, 10, null, null, null, null, null);
        when(userUpgradeRequestRepository.findByUserIdAndUpgradeIdAndCreateId(
                        USER_ID, UPGRADE_ID, CREATE_ID))
                .thenReturn(Optional.empty());
        when(walletRepository.findByUserIdForUpdate(USER_ID))
                .thenReturn(Optional.of(new Wallet(mock(User.class), 0, 0)));
        when(upgradeService.findUpgrade(UPGRADE_ID)).thenReturn(upgrade);
        User user = mock(User.class);
        UserUpgrade userUpgrade = new UserUpgrade(user, upgrade, 3);
        when(userUpgradeRepository.findByUserIdAndUpgradeId(USER_ID, UPGRADE_ID))
                .thenReturn(Optional.of(userUpgrade));

        assertStatus(
                () -> userUpgradeService.levelUp(USER_ID, request(UPGRADE_ID, CREATE_ID)),
                HttpStatus.CONFLICT,
                ErrorCode.UPGRADE_MAX_LEVEL,
                UPGRADE_ID);
        verifyNoInteractions(userItemRepository);
    }

    @Test
    @DisplayName("지갑이 없으면 404 WALLET_NOT_FOUND를 던지고 강화 정보는 조회하지 않는다")
    void shouldThrowNotFound_whenWalletMissing() {
        when(userUpgradeRequestRepository.findByUserIdAndUpgradeIdAndCreateId(
                        USER_ID, UPGRADE_ID, CREATE_ID))
                .thenReturn(Optional.empty());
        when(walletRepository.findByUserIdForUpdate(USER_ID)).thenReturn(Optional.empty());

        assertStatus(
                () -> userUpgradeService.levelUp(USER_ID, request(UPGRADE_ID, CREATE_ID)),
                HttpStatus.NOT_FOUND,
                ErrorCode.WALLET_NOT_FOUND,
                USER_ID);
        verifyNoInteractions(upgradeService, userItemRepository);
    }

    @Test
    @DisplayName("골드가 부족하면 409 WALLET_GOLD_NOT_ENOUGH를 던진다")
    void shouldThrowConflict_whenGoldNotEnough() {
        Upgrade upgrade =
                new Upgrade(UPGRADE_ID, 5, 1, null, 100, 10, null, null, null, null, null);
        givenNoExistingRequestOrProgress(upgrade);
        User walletOwner = mock(User.class);
        when(walletRepository.findByUserIdForUpdate(USER_ID))
                .thenReturn(Optional.of(new Wallet(walletOwner, 50, 0)));

        assertStatus(
                () -> userUpgradeService.levelUp(USER_ID, request(UPGRADE_ID, CREATE_ID)),
                HttpStatus.CONFLICT,
                ErrorCode.WALLET_GOLD_NOT_ENOUGH,
                UPGRADE_ID);
        verifyNoInteractions(userItemRepository);
    }

    @Test
    @DisplayName("재료가 없으면 409 ITEM_QUANTITY_NOT_ENOUGH를 던진다")
    void shouldThrowConflict_whenMaterialMissing() {
        Item material = new Item("mat.wood", null);
        Upgrade upgrade =
                new Upgrade(UPGRADE_ID, 5, 1, material, null, null, 3, 1, null, null, null);
        givenNoExistingRequestOrProgress(upgrade);
        when(walletRepository.findByUserIdForUpdate(USER_ID))
                .thenReturn(Optional.of(new Wallet(mock(User.class), 0, 0)));
        when(userItemRepository.findByUserIdAndItemIdForUpdate(USER_ID, "mat.wood"))
                .thenReturn(Optional.empty());

        assertStatus(
                () -> userUpgradeService.levelUp(USER_ID, request(UPGRADE_ID, CREATE_ID)),
                HttpStatus.CONFLICT,
                ErrorCode.ITEM_QUANTITY_NOT_ENOUGH,
                "mat.wood");
    }

    @Test
    @DisplayName("재료 수량이 부족하면 409 ITEM_QUANTITY_NOT_ENOUGH를 던진다")
    void shouldThrowConflict_whenMaterialQuantityNotEnough() {
        Item material = new Item("mat.wood", null);
        Upgrade upgrade =
                new Upgrade(UPGRADE_ID, 5, 1, material, null, null, 3, 1, null, null, null);
        givenNoExistingRequestOrProgress(upgrade);
        when(walletRepository.findByUserIdForUpdate(USER_ID))
                .thenReturn(Optional.of(new Wallet(mock(User.class), 0, 0)));
        User itemOwner = mock(User.class);
        when(userItemRepository.findByUserIdAndItemIdForUpdate(USER_ID, "mat.wood"))
                .thenReturn(Optional.of(new UserItem(itemOwner, material, 2)));

        assertStatus(
                () -> userUpgradeService.levelUp(USER_ID, request(UPGRADE_ID, CREATE_ID)),
                HttpStatus.CONFLICT,
                ErrorCode.ITEM_QUANTITY_NOT_ENOUGH,
                "mat.wood");
    }

    @Test
    @DisplayName("costBase가 없으면 골드를 차감하지 않는다")
    void shouldNotDeductGold_whenCostBaseNull() {
        Upgrade upgrade =
                new Upgrade(UPGRADE_ID, 5, 1, null, null, null, null, null, null, null, null);
        givenNoExistingRequestOrProgress(upgrade);
        Wallet wallet = new Wallet(mock(User.class), 500, 0);
        when(walletRepository.findByUserIdForUpdate(USER_ID)).thenReturn(Optional.of(wallet));
        when(userRepository.getReferenceById(USER_ID)).thenReturn(mock(User.class));
        when(userUpgradeRepository.save(any())).thenReturn(mock(UserUpgrade.class));

        userUpgradeService.levelUp(USER_ID, request(UPGRADE_ID, CREATE_ID));

        assertThat(wallet.getGold()).isEqualTo(500);
        verifyNoInteractions(userItemRepository);
    }

    @Test
    @DisplayName("재료 아이템이 없으면 재료 저장소를 조회하지 않는다")
    void shouldSkipMaterialLookup_whenMaterialItemNull() {
        Upgrade upgrade =
                new Upgrade(UPGRADE_ID, 5, 1, null, 100, 10, null, null, null, null, null);
        givenNoExistingRequestOrProgress(upgrade);
        User walletOwner = mock(User.class);
        when(walletRepository.findByUserIdForUpdate(USER_ID))
                .thenReturn(Optional.of(new Wallet(walletOwner, 1000, 0)));
        when(userRepository.getReferenceById(USER_ID)).thenReturn(mock(User.class));
        when(userUpgradeRepository.save(any())).thenReturn(mock(UserUpgrade.class));

        userUpgradeService.levelUp(USER_ID, request(UPGRADE_ID, CREATE_ID));

        verifyNoInteractions(userItemRepository);
    }

    @Test
    @DisplayName("costStep이 없으면 0으로 취급해 costBase만큼만 차감한다")
    void shouldTreatNullCostStepAsZero() {
        Upgrade upgrade =
                new Upgrade(UPGRADE_ID, 5, 1, null, 100, null, null, null, null, null, null);
        givenNoExistingRequestOrProgress(upgrade);
        Wallet wallet = new Wallet(mock(User.class), 150, 0);
        when(walletRepository.findByUserIdForUpdate(USER_ID)).thenReturn(Optional.of(wallet));
        when(userRepository.getReferenceById(USER_ID)).thenReturn(mock(User.class));
        when(userUpgradeRepository.save(any())).thenReturn(mock(UserUpgrade.class));

        userUpgradeService.levelUp(USER_ID, request(UPGRADE_ID, CREATE_ID));

        assertThat(wallet.getGold()).isEqualTo(50);
    }

    @Test
    @DisplayName("처음 강화하면 골드·재료를 차감하고 레벨 1의 유저 강화를 새로 만든다")
    void shouldDeductGoldAndMaterialAndCreateUserUpgrade_onFirstLevelUp() {
        Item material = new Item("mat.wood", null);
        Upgrade upgrade = new Upgrade(UPGRADE_ID, 5, 1, material, 100, 20, 3, 1, null, null, null);
        givenNoExistingRequestOrProgress(upgrade);

        Wallet wallet = new Wallet(mock(User.class), 200, 0);
        when(walletRepository.findByUserIdForUpdate(USER_ID)).thenReturn(Optional.of(wallet));
        UserItem userItem = new UserItem(mock(User.class), material, 10);
        when(userItemRepository.findByUserIdAndItemIdForUpdate(USER_ID, "mat.wood"))
                .thenReturn(Optional.of(userItem));

        User userRef = mock(User.class);
        when(userRepository.getReferenceById(USER_ID)).thenReturn(userRef);
        UserUpgrade created = new UserUpgrade(userRef, upgrade, 1);
        when(userUpgradeRepository.save(any())).thenReturn(created);

        UserUpgradeResponse response =
                userUpgradeService.levelUp(USER_ID, request(UPGRADE_ID, CREATE_ID));

        assertThat(response.getUpgradeId()).isEqualTo(UPGRADE_ID);
        assertThat(response.getLevel()).isEqualTo(1);
        assertThat(wallet.getGold()).isEqualTo(100);
        assertThat(userItem.getQuantity()).isEqualTo(7);

        ArgumentCaptor<UserUpgradeRequest> captor =
                ArgumentCaptor.forClass(UserUpgradeRequest.class);
        verify(userUpgradeRequestRepository).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getResultLevel()).isEqualTo(1);
        assertThat(captor.getValue().getGoldSpent()).isEqualTo(100);
        assertThat(captor.getValue().getMaterialSpent()).isEqualTo(3);
    }

    @Test
    @DisplayName("이미 진행 중인 강화는 레벨을 올리기만 하고 새로 저장하지 않는다")
    void shouldLevelUpExistingUserUpgrade_onSubsequentLevelUp() {
        givenUnlocked();
        Item material = new Item("mat.wood", null);
        Upgrade upgrade = new Upgrade(UPGRADE_ID, 5, 1, material, 100, 20, 3, 1, null, null, null);
        when(userUpgradeRequestRepository.findByUserIdAndUpgradeIdAndCreateId(
                        USER_ID, UPGRADE_ID, CREATE_ID))
                .thenReturn(Optional.empty());
        when(upgradeService.findUpgrade(UPGRADE_ID)).thenReturn(upgrade);

        UserUpgrade existing = new UserUpgrade(mock(User.class), upgrade, 2);
        when(userUpgradeRepository.findByUserIdAndUpgradeId(USER_ID, UPGRADE_ID))
                .thenReturn(Optional.of(existing));

        Wallet wallet = new Wallet(mock(User.class), 1000, 0);
        when(walletRepository.findByUserIdForUpdate(USER_ID)).thenReturn(Optional.of(wallet));
        UserItem userItem = new UserItem(mock(User.class), material, 10);
        when(userItemRepository.findByUserIdAndItemIdForUpdate(USER_ID, "mat.wood"))
                .thenReturn(Optional.of(userItem));

        UserUpgradeResponse response =
                userUpgradeService.levelUp(USER_ID, request(UPGRADE_ID, CREATE_ID));

        // cost = 100 + 20*2 = 140, material = 3 + 1*2 = 5
        assertThat(response.getLevel()).isEqualTo(3);
        assertThat(wallet.getGold()).isEqualTo(860);
        assertThat(userItem.getQuantity()).isEqualTo(5);
        verify(userUpgradeRepository, never()).save(any());
    }

    @Test
    @DisplayName("동시에 같은 요청이 처리되어 유니크 제약이 깨지면 409 UPGRADE_REQUEST_DUPLICATED를 던진다")
    void shouldThrowConflict_whenRequestSaveRacesWithDuplicate() {
        Upgrade upgrade =
                new Upgrade(UPGRADE_ID, 5, 1, null, null, null, null, null, null, null, null);
        givenNoExistingRequestOrProgress(upgrade);
        when(walletRepository.findByUserIdForUpdate(USER_ID))
                .thenReturn(Optional.of(new Wallet(mock(User.class), 0, 0)));
        when(userRepository.getReferenceById(USER_ID)).thenReturn(mock(User.class));
        when(userUpgradeRepository.save(any())).thenReturn(mock(UserUpgrade.class));
        when(userUpgradeRequestRepository.saveAndFlush(any()))
                .thenThrow(new DataIntegrityViolationException(""));

        assertStatus(
                () -> userUpgradeService.levelUp(USER_ID, request(UPGRADE_ID, CREATE_ID)),
                HttpStatus.CONFLICT,
                ErrorCode.UPGRADE_REQUEST_DUPLICATED,
                CREATE_ID);
    }

    @ParameterizedTest
    @CsvSource({
        "0,true,true,7,2",
        "1,true,true,13,5",
        "2,true,true,29,9",
        "1,true,false,13,4",
        "1,false,true,18,5",
        "1,false,false,18,4"
    })
    @DisplayName("현재 레벨 인덱스의 배열 비용이 선형 비용보다 우선하며 골드·재료를 각각 선택한다")
    void shouldUseIndependentCostArrays(
            int level,
            boolean goldArray,
            boolean materialArray,
            int expectedGold,
            int expectedMaterial) {
        // Given
        Item material = new Item("mat.wood", null);
        Upgrade upgrade =
                new Upgrade(
                        UPGRADE_ID,
                        3,
                        1,
                        material,
                        11,
                        7,
                        2,
                        2,
                        null,
                        goldArray ? List.of(7, 13, 29) : null,
                        materialArray ? List.of(2, 5, 9) : null);
        givenNoExistingRequestOrProgress(upgrade);
        User user = mock(User.class);
        UserUpgrade progress = new UserUpgrade(user, upgrade, level);
        when(userUpgradeRepository.findByUserIdAndUpgradeId(USER_ID, UPGRADE_ID))
                .thenReturn(Optional.of(progress));
        Wallet wallet = new Wallet(user, 100, 0);
        UserItem item = new UserItem(user, material, 100);
        when(walletRepository.findByUserIdForUpdate(USER_ID)).thenReturn(Optional.of(wallet));
        when(userItemRepository.findByUserIdAndItemIdForUpdate(USER_ID, "mat.wood"))
                .thenReturn(Optional.of(item));

        // When
        UserUpgradeResponse response =
                userUpgradeService.levelUp(USER_ID, request(UPGRADE_ID, CREATE_ID));

        // Then
        assertThat(response.getLevel()).isEqualTo(level + 1);
        assertThat(wallet.getGold()).isEqualTo(100 - expectedGold);
        assertThat(item.getQuantity()).isEqualTo(100 - expectedMaterial);
        ArgumentCaptor<UserUpgradeRequest> ledger =
                ArgumentCaptor.forClass(UserUpgradeRequest.class);
        verify(userUpgradeRequestRepository).saveAndFlush(ledger.capture());
        assertThat(ledger.getValue().getGoldSpent()).isEqualTo(expectedGold);
        assertThat(ledger.getValue().getMaterialSpent()).isEqualTo(expectedMaterial);
    }

    @Test
    @DisplayName("해금 전 구매는 재화·재료·강화 레벨을 변경하지 않는다")
    void shouldRejectLockedUpgradeBeforeSpending() {
        // Given
        Upgrade upgrade = new Upgrade(UPGRADE_ID, 3, 4, null, 11, 7, null, null, null, null, null);
        when(upgradeService.findUpgrade(UPGRADE_ID)).thenReturn(upgrade);
        when(userDataRepository.findByUserId(USER_ID))
                .thenReturn(Optional.of(new UserData(mock(User.class), 35)));
        when(playerLevelPolicy.hasReached(35, 4)).thenReturn(false);
        Wallet wallet = new Wallet(mock(User.class), 100, 0);
        when(walletRepository.findByUserIdForUpdate(USER_ID)).thenReturn(Optional.of(wallet));

        // When / Then
        assertStatus(
                () -> userUpgradeService.levelUp(USER_ID, request(UPGRADE_ID, CREATE_ID)),
                HttpStatus.CONFLICT,
                ErrorCode.UPGRADE_LOCKED,
                UPGRADE_ID);
        assertThat(wallet.getGold()).isEqualTo(100);
        verifyNoInteractions(userItemRepository);
        verify(userUpgradeRepository, never()).save(any());
        verify(userUpgradeRequestRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("지갑 잠금을 기다리던 중 완료된 동일 요청도 저장된 최초 결과를 반환한다")
    void shouldReturnCommittedDuplicateAfterWalletLock() {
        // Given
        UserUpgradeRequest prior = mock(UserUpgradeRequest.class);
        when(prior.getResultLevel()).thenReturn(3);
        when(userUpgradeRequestRepository.findByUserIdAndUpgradeIdAndCreateId(
                        USER_ID, UPGRADE_ID, CREATE_ID))
                .thenReturn(Optional.empty(), Optional.of(prior));
        Wallet wallet = new Wallet(mock(User.class), 100, 0);
        when(walletRepository.findByUserIdForUpdate(USER_ID)).thenReturn(Optional.of(wallet));

        // When
        UserUpgradeResponse result =
                userUpgradeService.levelUp(USER_ID, request(UPGRADE_ID, CREATE_ID));

        // Then
        assertThat(result.getLevel()).isEqualTo(3);
        assertThat(wallet.getGold()).isEqualTo(100);
        verifyNoInteractions(upgradeService, userUpgradeRepository, userItemRepository);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    @DisplayName("같은 progressionId를 쓰는 스킬 중 가장 낮은 해금 레벨을 저장된 경험치로 검사한다")
    void shouldUseEarliestLinkedSkillUnlock(boolean unlocked) {
        // Given
        Upgrade upgrade = new Upgrade(UPGRADE_ID, 3, 1, null, 11, 7, null, null, null, null, null);
        when(upgradeService.findUpgrade(UPGRADE_ID)).thenReturn(upgrade);
        Skill early = mock(Skill.class);
        Skill late = mock(Skill.class);
        when(early.getUnlockLevel()).thenReturn(3);
        when(late.getUnlockLevel()).thenReturn(7);
        when(skillRepository.findByProgressionId(UPGRADE_ID)).thenReturn(List.of(late, early));
        when(userDataRepository.findByUserId(USER_ID))
                .thenReturn(Optional.of(new UserData(mock(User.class), 35)));
        when(playerLevelPolicy.hasReached(35, 3)).thenReturn(unlocked);
        Wallet wallet = new Wallet(mock(User.class), 100, 0);
        when(walletRepository.findByUserIdForUpdate(USER_ID)).thenReturn(Optional.of(wallet));

        // When / Then
        if (unlocked) {
            when(userUpgradeRepository.save(any())).thenAnswer(call -> call.getArgument(0));
            assertThat(
                            userUpgradeService
                                    .levelUp(USER_ID, request(UPGRADE_ID, CREATE_ID))
                                    .getLevel())
                    .isEqualTo(1);
            assertThat(wallet.getGold()).isEqualTo(89);
        } else {
            assertStatus(
                    () -> userUpgradeService.levelUp(USER_ID, request(UPGRADE_ID, CREATE_ID)),
                    HttpStatus.CONFLICT,
                    ErrorCode.UPGRADE_LOCKED,
                    UPGRADE_ID);
            assertThat(wallet.getGold()).isEqualTo(100);
        }
    }

    @Test
    @DisplayName("저장된 배열이 잘못돼도 선형식으로 대체하거나 골드를 차감하지 않는다")
    void shouldRejectMalformedStoredArrayWithoutFallback() {
        // Given
        Upgrade upgrade =
                new Upgrade(UPGRADE_ID, 3, 1, null, 11, 7, null, null, null, List.of(2, 5), null);
        givenNoExistingRequestOrProgress(upgrade);
        Wallet wallet = new Wallet(mock(User.class), 100, 0);
        when(walletRepository.findByUserIdForUpdate(USER_ID)).thenReturn(Optional.of(wallet));

        // When / Then
        assertThatThrownBy(
                        () -> userUpgradeService.levelUp(USER_ID, request(UPGRADE_ID, CREATE_ID)))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("비용 배열");
        assertThat(wallet.getGold()).isEqualTo(100);
        verify(userUpgradeRequestRepository, never()).saveAndFlush(any());
    }

    private void givenUnlocked() {
        when(userDataRepository.findByUserId(USER_ID))
                .thenReturn(Optional.of(new UserData(mock(User.class), 0)));
        when(playerLevelPolicy.hasReached(0, 1)).thenReturn(true);
    }

    private void givenNoExistingRequestOrProgress(Upgrade upgrade) {
        givenUnlocked();
        when(userUpgradeRequestRepository.findByUserIdAndUpgradeIdAndCreateId(
                        USER_ID, UPGRADE_ID, CREATE_ID))
                .thenReturn(Optional.empty());
        when(upgradeService.findUpgrade(UPGRADE_ID)).thenReturn(upgrade);
        when(userUpgradeRepository.findByUserIdAndUpgradeId(USER_ID, UPGRADE_ID))
                .thenReturn(Optional.empty());
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
