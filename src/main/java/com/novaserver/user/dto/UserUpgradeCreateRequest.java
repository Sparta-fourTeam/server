package com.novaserver.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;

/** 유저 강화 정보 저장. */
@Getter
public class UserUpgradeCreateRequest {
    @NotBlank(message = "강화 id는 필수값입니다.")
    @Size(max = 255, message = "강화 id는 255자 이하입니다.")
    private String upgradeId;

    @NotBlank(message = "생성 id는 필수값입니다.")
    @Size(max = 255, message = "생성 id는 255자 이하입니다.")
    private String createId;
}
