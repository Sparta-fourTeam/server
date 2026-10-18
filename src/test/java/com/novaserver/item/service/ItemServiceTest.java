package com.novaserver.item.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.novaserver.global.error.ErrorCode;
import com.novaserver.item.dto.ItemCreateRequest;
import com.novaserver.item.dto.ItemRequest;
import com.novaserver.item.dto.ItemResponse;
import com.novaserver.item.entity.Item;
import com.novaserver.item.repository.ItemRepository;
import com.novaserver.skill.repository.SkillRepository;
import com.novaserver.stage.service.RewardRulesService;
import com.novaserver.upgrade.repository.UpgradeRepository;
import com.novaserver.user.repository.UserItemRepository;
import java.util.List;
import java.util.Optional;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class ItemServiceTest {

    @Mock private ItemRepository itemRepository;
    @Mock private UserItemRepository userItemRepository;
    @Mock private SkillRepository skillRepository;
    @Mock private UpgradeRepository upgradeRepository;
    @Mock private RewardRulesService rewardRules;

    @InjectMocks private ItemService itemService;

    private ItemCreateRequest createRequest(String itemId, String targetId) {
        ItemCreateRequest request = new ItemCreateRequest();
        ReflectionTestUtils.setField(request, "itemId", itemId);
        ReflectionTestUtils.setField(request, "targetId", targetId);
        return request;
    }

    private ItemRequest updateRequest(String targetId) {
        ItemRequest request = new ItemRequest();
        ReflectionTestUtils.setField(request, "targetId", targetId);
        return request;
    }

    @Test
    @DisplayName("아이템 목록을 itemId 기준으로 정렬해서 조회한다")
    void shouldReturnItemList_onGetItemList() {
        when(itemRepository.findAll(org.springframework.data.domain.Sort.by("itemId")))
                .thenReturn(List.of(new Item("book.hat", null)));

        List<ItemResponse> responses = itemService.getItemList();

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).itemId()).isEqualTo("book.hat");
    }

    @Test
    @DisplayName("존재하지 않는 아이템을 조회하면 404 ITEM_NOT_FOUND를 던진다")
    void shouldThrowNotFound_whenItemMissingOnGet() {
        when(itemRepository.findById("book.hat")).thenReturn(Optional.empty());

        assertStatus(
                () -> itemService.getItem("book.hat"),
                HttpStatus.NOT_FOUND,
                ErrorCode.ITEM_NOT_FOUND,
                "book.hat");
    }

    @Test
    @DisplayName("이미 존재하는 id로 등록하면 409 ITEM_DUPLICATED를 던지고 저장하지 않는다")
    void shouldThrowConflict_whenCreatingDuplicatedId() {
        when(itemRepository.existsById("book.hat")).thenReturn(true);

        assertStatus(
                () -> itemService.createItem(createRequest("book.hat", null)),
                HttpStatus.CONFLICT,
                ErrorCode.ITEM_DUPLICATED,
                "book.hat");
        verify(itemRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("대상 스킬·장비 강화가 없으면 400 ITEM_DATA_INVALID를 던진다")
    void shouldThrowBadRequest_whenTargetIdNotNumeric() {
        when(itemRepository.existsById("book.skill")).thenReturn(false);

        assertThatThrownBy(() -> itemService.createItem(createRequest("book.skill", "abc")))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("targetId에 해당하는 스킬 또는 장비 강화가 없습니다");
    }

    @Test
    @DisplayName("targetId에 해당하는 스킬이 없으면 400 ITEM_DATA_INVALID를 던진다")
    void shouldThrowBadRequest_whenTargetSkillMissing() {
        when(itemRepository.existsById("book.skill")).thenReturn(false);
        when(skillRepository.existsById(1L)).thenReturn(false);

        assertThatThrownBy(() -> itemService.createItem(createRequest("book.skill", "1")))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("targetId에 해당하는 스킬이 없습니다");
    }

    @Test
    @DisplayName("스킬 재료 아이템을 정상적으로 등록한다")
    void shouldCreateItemSuccessfully_withSkillTarget() {
        when(itemRepository.existsById("book.skill")).thenReturn(false);
        when(skillRepository.existsById(1L)).thenReturn(true);
        when(itemRepository.saveAndFlush(any())).thenReturn(new Item("book.skill", "1"));

        ItemResponse response = itemService.createItem(createRequest("book.skill", "1"));

        assertThat(response.itemId()).isEqualTo("book.skill");
        assertThat(response.targetId()).isEqualTo("1");
    }

    @Test
    void equipmentTargetCanBeRegistered() {
        when(upgradeRepository.existsById("fixture.equipment.target")).thenReturn(true);
        when(itemRepository.saveAndFlush(any()))
                .thenReturn(new Item("fixture.equipment", "fixture.equipment.target"));
        var result =
                itemService.createItem(
                        createRequest("fixture.equipment", "fixture.equipment.target"));
        assertThat(result.targetId()).isEqualTo("fixture.equipment.target");
    }

    @Test
    @DisplayName("저장 시점에 유니크 제약이 깨지면 409 ITEM_DUPLICATED를 던진다")
    void shouldThrowConflict_whenSaveViolatesUniqueConstraint() {
        when(itemRepository.existsById("mat.wood")).thenReturn(false);
        when(itemRepository.saveAndFlush(any())).thenThrow(new DataIntegrityViolationException(""));

        assertStatus(
                () -> itemService.createItem(createRequest("mat.wood", null)),
                HttpStatus.CONFLICT,
                ErrorCode.ITEM_DUPLICATED,
                "mat.wood");
    }

    @Test
    @DisplayName("존재하지 않는 아이템을 수정하면 404 ITEM_NOT_FOUND를 던진다")
    void shouldThrowNotFound_whenUpdatingMissingItem() {
        when(itemRepository.findById("book.hat")).thenReturn(Optional.empty());

        assertStatus(
                () -> itemService.updateItem("book.hat", updateRequest(null)),
                HttpStatus.NOT_FOUND,
                ErrorCode.ITEM_NOT_FOUND,
                "book.hat");
    }

    @Test
    @DisplayName("아이템을 정상적으로 수정한다")
    void shouldUpdateItemSuccessfully() {
        Item item = new Item("mat.wood", null);
        when(itemRepository.findById("mat.wood")).thenReturn(Optional.of(item));

        ItemResponse response = itemService.updateItem("mat.wood", updateRequest(null));

        assertThat(response.targetId()).isNull();
    }

    @Test
    @DisplayName("존재하지 않는 아이템을 삭제하면 404 ITEM_NOT_FOUND를 던진다")
    void shouldThrowNotFound_whenDeletingMissingItem() {
        when(itemRepository.findById("book.hat")).thenReturn(Optional.empty());

        assertStatus(
                () -> itemService.deleteItem("book.hat"),
                HttpStatus.NOT_FOUND,
                ErrorCode.ITEM_NOT_FOUND,
                "book.hat");
    }

    @Test
    @DisplayName("유저가 보유 중인 아이템을 삭제하면 409 ITEM_IN_USE를 던진다")
    void shouldThrowConflict_whenDeletingItemOwnedByUser() {
        Item item = new Item("book.hat", null);
        when(itemRepository.findById("book.hat")).thenReturn(Optional.of(item));
        when(userItemRepository.existsByItemId("book.hat")).thenReturn(true);

        assertStatus(
                () -> itemService.deleteItem("book.hat"),
                HttpStatus.CONFLICT,
                ErrorCode.ITEM_IN_USE,
                "book.hat");
    }

    @Test
    @DisplayName("강화 재료로 사용 중인 아이템을 삭제하면 409 ITEM_LINKED_TO_UPGRADE를 던진다")
    void shouldThrowConflict_whenDeletingItemLinkedToUpgrade() {
        Item item = new Item("mat.wood", null);
        when(itemRepository.findById("mat.wood")).thenReturn(Optional.of(item));
        when(userItemRepository.existsByItemId("mat.wood")).thenReturn(false);
        when(upgradeRepository.existsByMaterialItemId("mat.wood")).thenReturn(true);

        assertStatus(
                () -> itemService.deleteItem("mat.wood"),
                HttpStatus.CONFLICT,
                ErrorCode.ITEM_LINKED_TO_UPGRADE,
                "mat.wood");
    }

    @Test
    @DisplayName("보상 기준에 분류된 아이템 삭제를 차단한다")
    void shouldRejectDeletingRewardItem() {
        doThrow(ErrorCode.ITEM_LINKED_TO_REWARD_RULES.exception("test.material"))
                .when(rewardRules)
                .checkItemRemovable("test.material");
        assertStatus(
                () -> itemService.deleteItem("test.material"),
                HttpStatus.CONFLICT,
                ErrorCode.ITEM_LINKED_TO_REWARD_RULES,
                "test.material");
    }

    @Test
    @DisplayName("아이템을 정상적으로 삭제한다")
    void shouldDeleteItemSuccessfully() {
        Item item = new Item("mat.wood", null);
        when(itemRepository.findById("mat.wood")).thenReturn(Optional.of(item));
        when(userItemRepository.existsByItemId("mat.wood")).thenReturn(false);
        when(upgradeRepository.existsByMaterialItemId("mat.wood")).thenReturn(false);

        itemService.deleteItem("mat.wood");

        verify(itemRepository).delete(item);
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
