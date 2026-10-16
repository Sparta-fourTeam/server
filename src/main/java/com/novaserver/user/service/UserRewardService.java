package com.novaserver.user.service;

import com.novaserver.global.error.ErrorCode;
import com.novaserver.item.entity.Item;
import com.novaserver.item.repository.ItemRepository;
import com.novaserver.user.entity.User;
import com.novaserver.user.entity.UserData;
import com.novaserver.user.entity.UserItem;
import com.novaserver.user.entity.Wallet;
import com.novaserver.user.repository.UserDataRepository;
import com.novaserver.user.repository.UserItemRepository;
import com.novaserver.user.repository.UserRepository;
import com.novaserver.user.repository.WalletRepository;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 유저에게 경험치·골드·아이템을 지급하는 공용 서비스. */
@Service
@RequiredArgsConstructor
public class UserRewardService {
    private final UserRepository userRepository;
    private final UserDataRepository userDataRepository;
    private final WalletRepository walletRepository;
    private final UserItemRepository userItemRepository;
    private final ItemRepository itemRepository;

    /**
     * 경험치·골드·아이템을 지급한다. 각 값이 0이면 해당 지급은 건너뛴다.
     *
     * @param userId 유저 id
     * @param exp 지급할 경험치
     * @param gold 지급할 골드
     * @param items 지급할 아이템별 수량
     */
    @Transactional
    public void grant(Long userId, int exp, int gold, Map<String, Integer> items) {
        if (exp > 0) {
            UserData userData =
                    userDataRepository
                            .findByUserId(userId)
                            .orElseThrow(() -> ErrorCode.USER_DATA_NOT_FOUND.exception(userId));
            userData.addExp(exp);
        }

        if (gold > 0) {
            Wallet wallet =
                    walletRepository
                            .findByUserIdForUpdate(userId)
                            .orElseThrow(() -> ErrorCode.WALLET_NOT_FOUND.exception(userId));
            wallet.addGold(gold);
        }

        items.forEach((itemId, amount) -> grantItem(userId, itemId, amount));
    }

    private void grantItem(Long userId, String itemId, int amount) {
        if (amount <= 0) {
            return;
        }

        UserItem userItem = userItemRepository.findByUserIdAndItemId(userId, itemId).orElse(null);
        if (userItem != null) {
            userItem.addQuantity(amount);
            return;
        }

        Item item =
                itemRepository
                        .findById(itemId)
                        .orElseThrow(() -> ErrorCode.ITEM_NOT_FOUND.exception(itemId));
        User user = userRepository.getReferenceById(userId);
        userItemRepository.save(new UserItem(user, item, amount));
    }
}
