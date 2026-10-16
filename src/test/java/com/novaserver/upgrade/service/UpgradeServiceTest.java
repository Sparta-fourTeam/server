package com.novaserver.upgrade.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.novaserver.global.error.ErrorCode;
import com.novaserver.item.entity.Item;
import com.novaserver.item.repository.ItemRepository;
import com.novaserver.upgrade.dto.UpgradeCreateRequest;
import com.novaserver.upgrade.dto.UpgradeRequest;
import com.novaserver.upgrade.dto.UpgradeResponse;
import com.novaserver.upgrade.entity.Upgrade;
import com.novaserver.upgrade.entity.UpgradeEffect;
import com.novaserver.upgrade.repository.UpgradeRepository;
import com.novaserver.user.repository.UserUpgradeRepository;
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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class UpgradeServiceTest {

    @Mock private UpgradeRepository upgradeRepository;
    @Mock private UserUpgradeRepository userUpgradeRepository;
    @Mock private ItemRepository itemRepository;

    @InjectMocks private UpgradeService upgradeService;

    private UpgradeCreateRequest createRequest(
            String upgradeId, int maxLevel, String materialItemId, List<UpgradeEffect> effects) {
        UpgradeCreateRequest request = new UpgradeCreateRequest();
        ReflectionTestUtils.setField(request, "upgradeId", upgradeId);
        ReflectionTestUtils.setField(request, "maxLevel", maxLevel);
        ReflectionTestUtils.setField(request, "unlockLevel", 1);
        ReflectionTestUtils.setField(request, "materialItemId", materialItemId);
        ReflectionTestUtils.setField(request, "effects", effects);
        return request;
    }

    private UpgradeRequest updateRequest(
            int maxLevel, String materialItemId, List<UpgradeEffect> effects) {
        UpgradeRequest request = new UpgradeRequest();
        ReflectionTestUtils.setField(request, "maxLevel", maxLevel);
        ReflectionTestUtils.setField(request, "unlockLevel", 1);
        ReflectionTestUtils.setField(request, "materialItemId", materialItemId);
        ReflectionTestUtils.setField(request, "effects", effects);
        return request;
    }

    @Test
    @DisplayName("강화 목록을 upgradeId 기준으로 정렬해서 조회한다")
    void shouldReturnUpgradeList_onGetUpgradeList() {
        when(upgradeRepository.findAll(Sort.by("upgradeId")))
                .thenReturn(List.of(new Upgrade("atk", 10, 1, null, null, null, null, null, null)));

        List<UpgradeResponse> responses = upgradeService.getUpgradeList();

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).upgradeId()).isEqualTo("atk");
    }

    @Test
    @DisplayName("존재하지 않는 강화를 조회하면 404 UPGRADE_NOT_FOUND를 던진다")
    void shouldThrowNotFound_whenUpgradeMissingOnGet() {
        when(upgradeRepository.findById("atk")).thenReturn(Optional.empty());

        assertStatus(
                () -> upgradeService.getUpgrade("atk"),
                HttpStatus.NOT_FOUND,
                ErrorCode.UPGRADE_NOT_FOUND,
                "atk");
    }

    @Test
    @DisplayName("이미 존재하는 id로 등록하면 409 UPGRADE_DUPLICATED를 던지고 저장하지 않는다")
    void shouldThrowConflict_whenCreatingDuplicatedId() {
        when(upgradeRepository.existsById("atk")).thenReturn(true);

        assertStatus(
                () -> upgradeService.createUpgrade(createRequest("atk", 10, null, null)),
                HttpStatus.CONFLICT,
                ErrorCode.UPGRADE_DUPLICATED,
                "atk");
        verify(upgradeRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("effects.kind가 비어있으면 400 UPGRADE_DATA_INVALID를 던진다")
    void shouldThrowBadRequest_whenEffectKindBlank() {
        when(upgradeRepository.existsById("atk")).thenReturn(false);
        UpgradeEffect effect = new UpgradeEffect(" ", 1, null);

        assertThatThrownBy(
                        () ->
                                upgradeService.createUpgrade(
                                        createRequest("atk", 10, null, List.of(effect))))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("effects.kind는 필수값입니다");
    }

    @Test
    @DisplayName("effects에 valuePerLevel과 levelValues가 둘 다 있으면 400 UPGRADE_DATA_INVALID를 던진다")
    void shouldThrowBadRequest_whenEffectHasBothValueForms() {
        when(upgradeRepository.existsById("atk")).thenReturn(false);
        UpgradeEffect effect = new UpgradeEffect("ATK", 1, List.of(1, 2, 3));

        assertThatThrownBy(
                        () ->
                                upgradeService.createUpgrade(
                                        createRequest("atk", 3, null, List.of(effect))))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("effects는 valuePerLevel과 levelValues 중 하나만 있어야 합니다");
    }

    @Test
    @DisplayName("effects에 valuePerLevel과 levelValues가 둘 다 없으면 400 UPGRADE_DATA_INVALID를 던진다")
    void shouldThrowBadRequest_whenEffectHasNeitherValueForm() {
        when(upgradeRepository.existsById("atk")).thenReturn(false);
        UpgradeEffect effect = new UpgradeEffect("ATK", null, null);

        assertThatThrownBy(
                        () ->
                                upgradeService.createUpgrade(
                                        createRequest("atk", 3, null, List.of(effect))))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("effects는 valuePerLevel과 levelValues 중 하나만 있어야 합니다");
    }

    @Test
    @DisplayName("effects.levelValues 길이가 maxLevel과 다르면 400 UPGRADE_DATA_INVALID를 던진다")
    void shouldThrowBadRequest_whenLevelValuesLengthMismatch() {
        when(upgradeRepository.existsById("atk")).thenReturn(false);
        UpgradeEffect effect = new UpgradeEffect("ATK", null, List.of(1, 2));

        assertThatThrownBy(
                        () ->
                                upgradeService.createUpgrade(
                                        createRequest("atk", 3, null, List.of(effect))))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("effects.levelValues 길이는 maxLevel(3)과 같아야 합니다");
    }

    @Test
    @DisplayName("존재하지 않는 재료 아이템을 지정하면 404 ITEM_NOT_FOUND를 던진다")
    void shouldThrowNotFound_whenMaterialItemMissing() {
        when(upgradeRepository.existsById("atk")).thenReturn(false);
        when(itemRepository.findById("mat.wood")).thenReturn(Optional.empty());

        assertStatus(
                () -> upgradeService.createUpgrade(createRequest("atk", 10, "mat.wood", null)),
                HttpStatus.NOT_FOUND,
                ErrorCode.ITEM_NOT_FOUND,
                "mat.wood");
    }

    @Test
    @DisplayName("재료 없는 강화를 정상적으로 등록한다")
    void shouldCreateUpgradeSuccessfully_withoutMaterial() {
        when(upgradeRepository.existsById("atk")).thenReturn(false);
        when(upgradeRepository.saveAndFlush(any()))
                .thenReturn(new Upgrade("atk", 10, 1, null, 100, 10, null, null, null));

        UpgradeResponse response =
                upgradeService.createUpgrade(createRequest("atk", 10, null, null));

        assertThat(response.upgradeId()).isEqualTo("atk");
        assertThat(response.materialItemId()).isNull();
    }

    @Test
    @DisplayName("재료가 있는 강화를 정상적으로 등록한다")
    void shouldCreateUpgradeSuccessfully_withMaterial() {
        Item material = new Item("mat.wood", null);
        when(upgradeRepository.existsById("atk")).thenReturn(false);
        when(itemRepository.findById("mat.wood")).thenReturn(Optional.of(material));
        when(upgradeRepository.saveAndFlush(any()))
                .thenReturn(new Upgrade("atk", 10, 1, material, 100, 10, 2, 1, null));

        UpgradeResponse response =
                upgradeService.createUpgrade(createRequest("atk", 10, "mat.wood", null));

        assertThat(response.materialItemId()).isEqualTo("mat.wood");
    }

    @Test
    @DisplayName("unlockLevel이 없으면 1로 등록한다")
    void shouldDefaultUnlockLevelToOne_whenCreatingWithoutUnlockLevel() {
        when(upgradeRepository.existsById("atk")).thenReturn(false);
        when(upgradeRepository.saveAndFlush(any()))
                .thenReturn(new Upgrade("atk", 10, 1, null, null, null, null, null, null));
        UpgradeCreateRequest request = createRequest("atk", 10, null, null);
        ReflectionTestUtils.setField(request, "unlockLevel", null);

        upgradeService.createUpgrade(request);

        ArgumentCaptor<Upgrade> captor = ArgumentCaptor.forClass(Upgrade.class);
        verify(upgradeRepository).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getUnlockLevel()).isEqualTo(1);
    }

    @Test
    @DisplayName("저장 시점에 유니크 제약이 깨지면 409 UPGRADE_DUPLICATED를 던진다")
    void shouldThrowConflict_whenSaveViolatesUniqueConstraint() {
        when(upgradeRepository.existsById("atk")).thenReturn(false);
        when(upgradeRepository.saveAndFlush(any()))
                .thenThrow(new DataIntegrityViolationException(""));

        assertStatus(
                () -> upgradeService.createUpgrade(createRequest("atk", 10, null, null)),
                HttpStatus.CONFLICT,
                ErrorCode.UPGRADE_DUPLICATED,
                "atk");
    }

    @Test
    @DisplayName("존재하지 않는 강화를 수정하면 404 UPGRADE_NOT_FOUND를 던진다")
    void shouldThrowNotFound_whenUpdatingMissingUpgrade() {
        when(upgradeRepository.findById("atk")).thenReturn(Optional.empty());

        assertStatus(
                () -> upgradeService.updateUpgrade("atk", updateRequest(10, null, null)),
                HttpStatus.NOT_FOUND,
                ErrorCode.UPGRADE_NOT_FOUND,
                "atk");
    }

    @Test
    @DisplayName("강화를 정상적으로 수정하면 효과가 통째로 교체된다")
    void shouldUpdateUpgradeSuccessfully() {
        Upgrade upgrade = new Upgrade("atk", 10, 1, null, 100, 10, null, null, null);
        when(upgradeRepository.findById("atk")).thenReturn(Optional.of(upgrade));
        UpgradeEffect newEffect = new UpgradeEffect("ATK", 5, null);

        UpgradeResponse response =
                upgradeService.updateUpgrade("atk", updateRequest(10, null, List.of(newEffect)));

        assertThat(response.effects()).containsExactly(newEffect);
    }

    @Test
    @DisplayName("unlockLevel이 없으면 1로 수정한다")
    void shouldDefaultUnlockLevelToOne_whenUpdatingWithoutUnlockLevel() {
        Upgrade upgrade = new Upgrade("atk", 10, 5, null, 100, 10, null, null, null);
        when(upgradeRepository.findById("atk")).thenReturn(Optional.of(upgrade));
        UpgradeRequest request = updateRequest(10, null, null);
        ReflectionTestUtils.setField(request, "unlockLevel", null);

        UpgradeResponse response = upgradeService.updateUpgrade("atk", request);

        assertThat(response.unlockLevel()).isEqualTo(1);
    }

    @Test
    @DisplayName("존재하지 않는 강화를 삭제하면 404 UPGRADE_NOT_FOUND를 던진다")
    void shouldThrowNotFound_whenDeletingMissingUpgrade() {
        when(upgradeRepository.findById("atk")).thenReturn(Optional.empty());

        assertStatus(
                () -> upgradeService.deleteUpgrade("atk"),
                HttpStatus.NOT_FOUND,
                ErrorCode.UPGRADE_NOT_FOUND,
                "atk");
    }

    @Test
    @DisplayName("유저가 이미 사용 중인 강화를 삭제하면 409 UPGRADE_IN_USE를 던진다")
    void shouldThrowConflict_whenDeletingUpgradeInUse() {
        Upgrade upgrade = new Upgrade("atk", 10, 1, null, null, null, null, null, null);
        when(upgradeRepository.findById("atk")).thenReturn(Optional.of(upgrade));
        when(userUpgradeRepository.existsByUpgradeId("atk")).thenReturn(true);

        assertStatus(
                () -> upgradeService.deleteUpgrade("atk"),
                HttpStatus.CONFLICT,
                ErrorCode.UPGRADE_IN_USE,
                "atk");
    }

    @Test
    @DisplayName("강화를 정상적으로 삭제한다")
    void shouldDeleteUpgradeSuccessfully() {
        Upgrade upgrade = new Upgrade("atk", 10, 1, null, null, null, null, null, null);
        when(upgradeRepository.findById("atk")).thenReturn(Optional.of(upgrade));
        when(userUpgradeRepository.existsByUpgradeId("atk")).thenReturn(false);

        upgradeService.deleteUpgrade("atk");

        verify(upgradeRepository).delete(upgrade);
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
