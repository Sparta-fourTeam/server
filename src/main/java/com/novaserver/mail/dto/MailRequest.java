package com.novaserver.mail.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.Getter;

/** 우편 발송 요청. */
@Getter
@Schema(description = "우편 발송 요청")
public class MailRequest {
    @Schema(description = "받는 유저 id", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "userId는 필수입니다")
    private Long userId;

    @Schema(
            description = "우편 제목",
            example = "출석 보상 안내",
            requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "제목은 필수입니다")
    @Size(max = 100, message = "제목은 100자를 넘을 수 없습니다")
    private String title;

    @Schema(
            description = "우편 내용",
            example = "꾸준한 접속에 감사드립니다",
            requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "내용은 필수입니다")
    @Size(max = 1000, message = "내용은 1000자를 넘을 수 없습니다")
    private String content;

    @Schema(description = "함께 보낼 경험치. 없으면 0", example = "0")
    @PositiveOrZero(message = "exp는 0 이상이어야 합니다")
    private int exp;

    @Schema(description = "함께 보낼 골드. 없으면 0", example = "100")
    @PositiveOrZero(message = "gold는 0 이상이어야 합니다")
    private int gold;

    @Schema(description = "함께 보낼 젬(유료 재화). 없으면 0", example = "0")
    @PositiveOrZero(message = "gem은 0 이상이어야 합니다")
    private int gem;

    @Schema(description = "함께 보낼 에너지. 없으면 0", example = "0")
    @PositiveOrZero(message = "energy는 0 이상이어야 합니다")
    private int energy;

    @Schema(description = "첨부 아이템 목록. 없으면 빈 배열 또는 생략")
    @Valid
    private List<MailItemRequest> items;
}
