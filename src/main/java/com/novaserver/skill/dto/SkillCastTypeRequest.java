package com.novaserver.skill.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;

/** 스킬 시전 방식 등록·수정 요청. */
@Getter
public class SkillCastTypeRequest {
    @NotBlank(message = "이름은 필수값입니다.")
    @Size(max = 30, message = "이름은 30자 이하입니다.")
    @Pattern(regexp = "^[A-Za-z][A-Za-z0-9_]*$", message = "이름은 영문, 숫자, 밑줄만 사용할 수 있습니다")
    private String name;

    @NotBlank(message = "표시 이름은 필수값입니다.")
    @Size(max = 255, message = "표시 이름은 255자 이하입니다.")
    private String label;
}
