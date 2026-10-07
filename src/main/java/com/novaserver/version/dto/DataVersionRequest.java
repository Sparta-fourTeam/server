package com.novaserver.version.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

/** 데이터 버전 증가 요청. */
@Getter
public class DataVersionRequest {
    @NotBlank(message = "테이블 이름은 필수입니다")
    private String table;
}
