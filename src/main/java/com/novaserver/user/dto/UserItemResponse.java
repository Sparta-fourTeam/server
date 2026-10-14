package com.novaserver.user.dto;

import com.novaserver.user.entity.UserItem;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
@Schema(description = "유저 아이템 응답")
public class UserItemResponse {
    @Schema(description = "아이템 id")
    private final Long itemId;

    @Schema(description = "보유 수량")
    private final int quantity;

    public static UserItemResponse from(UserItem userItem) {
        return new UserItemResponse(userItem.getItem().getId(), userItem.getQuantity());
    }
}
