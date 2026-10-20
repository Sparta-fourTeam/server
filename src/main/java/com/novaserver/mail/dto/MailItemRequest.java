package com.novaserver.mail.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;

/** 우편에 첨부할 아이템. */
@Getter
@Schema(description = "우편 첨부 아이템")
public class MailItemRequest {
    @Schema(
            description = "아이템 id",
            example = "ITEM_POTION",
            requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "itemId는 필수입니다")
    private String itemId;

    @Schema(description = "지급 수량", example = "5", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "quantity는 필수입니다")
    @Positive(message = "quantity는 1 이상이어야 합니다")
    private Integer quantity;
}
