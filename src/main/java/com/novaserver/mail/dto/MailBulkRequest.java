package com.novaserver.mail.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.Getter;

/** 여러 유저에게 같은 우편을 보내는 요청. */
@Getter
@Schema(description = "우편 대량 발송 요청")
public class MailBulkRequest {
    @Schema(description = "받는 유저 id 목록", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "userIds는 1개 이상이어야 합니다")
    private List<Long> userIds;

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

    @Schema(description = "함께 보낼 골드. 없으면 0", example = "100")
    @PositiveOrZero(message = "gold는 0 이상이어야 합니다")
    private int gold;

    @Schema(description = "첨부 아이템 목록. 없으면 빈 배열 또는 생략")
    @Valid
    private List<MailItemRequest> items;
}
