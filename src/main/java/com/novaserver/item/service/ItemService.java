package com.novaserver.item.service;

import com.novaserver.global.error.ErrorCode;
import com.novaserver.item.dto.ItemCreateRequest;
import com.novaserver.item.dto.ItemRequest;
import com.novaserver.item.dto.ItemResponse;
import com.novaserver.item.entity.Item;
import com.novaserver.item.repository.ItemRepository;
import com.novaserver.skill.repository.SkillRepository;
import com.novaserver.upgrade.repository.UpgradeRepository;
import com.novaserver.user.repository.UserItemRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 아이템 데이터 관련 API. */
@Service
@RequiredArgsConstructor
public class ItemService {
    private final ItemRepository itemRepository;
    private final UserItemRepository userItemRepository;
    private final SkillRepository skillRepository;
    private final UpgradeRepository upgradeRepository;

    /** 아이템 데이터를 전체 조회한다. 유니티와 관리자 목록에서 함께 쓴다. */
    @Transactional(readOnly = true)
    public List<ItemResponse> getItemList() {
        return itemRepository.findAll(Sort.by("itemId")).stream().map(ItemResponse::from).toList();
    }

    /** 아이템 한 건을 조회한다. 없으면 404를 던진다. */
    @Transactional(readOnly = true)
    public ItemResponse getItem(String id) {
        return ItemResponse.from(findItem(id));
    }

    /** 아이템을 등록한다. 이미 있는 id면 409, 데이터가 올바르지 않으면 400을 던진다. */
    @Transactional
    public ItemResponse createItem(ItemCreateRequest request) {
        // id를 직접 지정하는 엔티티라 save()가 기존 행을 덮어쓰므로, 먼저 존재 여부를 확인한다.
        if (itemRepository.existsById(request.getItemId())) {
            throw ErrorCode.ITEM_DUPLICATED.exception(request.getItemId());
        }
        validate(request.getTargetId());

        try {
            Item saved =
                    itemRepository.saveAndFlush(
                            new Item(request.getItemId(), request.getTargetId()));
            return ItemResponse.from(saved);
        } catch (DataIntegrityViolationException e) {
            throw ErrorCode.ITEM_DUPLICATED.exception(request.getItemId());
        }
    }

    /** 아이템을 수정한다. 없는 id면 404를 던진다. */
    @Transactional
    public ItemResponse updateItem(String id, ItemRequest request) {
        Item item = findItem(id);
        validate(request.getTargetId());

        item.update(request.getTargetId());
        return ItemResponse.from(item);
    }

    /** 아이템을 삭제한다. 유저가 이미 보유 중이거나 강화 재료로 쓰이고 있으면 409를 던진다. */
    @Transactional
    public void deleteItem(String id) {
        Item item = findItem(id);
        if (userItemRepository.existsByItemId(id)) {
            throw ErrorCode.ITEM_IN_USE.exception(id);
        }
        if (upgradeRepository.existsByMaterialItemId(id)) {
            throw ErrorCode.ITEM_LINKED_TO_UPGRADE.exception(id);
        }
        itemRepository.delete(item);
    }

    /** 아이템을 id로 찾는다. 없으면 404를 던진다. */
    public Item findItem(String id) {
        return itemRepository
                .findById(id)
                .orElseThrow(() -> ErrorCode.ITEM_NOT_FOUND.exception(id));
    }

    /** 스킬 재료(targetId 있음)면 해당 스킬이 있는지 확인한다. */
    private void validate(String targetId) {
        if (targetId != null) {
            Long skillId;
            try {
                skillId = Long.valueOf(targetId);
            } catch (NumberFormatException e) {
                throw ErrorCode.ITEM_DATA_INVALID.exception(
                        "targetId는 숫자 형태의 스킬 id여야 합니다: " + targetId);
            }
            if (!skillRepository.existsById(skillId)) {
                throw ErrorCode.ITEM_DATA_INVALID.exception("targetId에 해당하는 스킬이 없습니다: " + targetId);
            }
        }
    }
}
