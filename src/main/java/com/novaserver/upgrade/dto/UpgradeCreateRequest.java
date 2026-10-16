package com.novaserver.upgrade.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;

/** 강화 등록 요청. */
@Getter
public class UpgradeCreateRequest extends UpgradeRequest {
    @NotBlank(message = "강화 id는 필수값입니다.")
    @Size(max = 255, message = "강화 id는 255자 이하입니다.")
    private String upgradeId;
}
