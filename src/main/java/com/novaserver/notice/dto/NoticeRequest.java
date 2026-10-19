package com.novaserver.notice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;

/** 공지 등록 요청. */
@Getter
@Schema(description = "공지 등록 요청")
public class NoticeRequest {
    @Schema(
            description = "공지 제목",
            example = "정기 점검 안내",
            requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "제목은 필수입니다")
    @Size(max = 100, message = "제목은 100자를 넘을 수 없습니다")
    private String title;

    @Schema(
            description = "공지 내용",
            example = "오늘 03:00~05:00 서버 점검이 있습니다",
            requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "내용은 필수입니다")
    @Size(max = 1000, message = "내용은 1000자를 넘을 수 없습니다")
    private String content;
}
