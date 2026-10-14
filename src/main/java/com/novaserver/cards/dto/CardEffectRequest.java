package com.novaserver.cards.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.util.Map;
import lombok.Getter;

/** 카드 효과 등록·수정 요청. */
@Getter
public class CardEffectRequest {
    @Min(value = 0, message = "sortOrder는 0 이상입니다.")
    private int sortOrder;

    @NotBlank(message = "kind는 필수값입니다.")
    private String kind;

    private Double value;

    private String form;

    private String trigger;

    private Long skillId;

    @DecimalMin(value = "0.0", inclusive = false, message = "chance는 0보다 커야 합니다.")
    @DecimalMax(value = "1.0", message = "chance는 1 이하입니다.")
    private Double chance;

    @DecimalMin(value = "0.0", inclusive = false, message = "damageScale은 0보다 커야 합니다.")
    private Double damageScale;

    private Long target;

    @Min(value = 1, message = "count는 1 이상입니다.")
    private Integer count;

    private String onlyForm;

    private Boolean excludeHit;

    @DecimalMin(value = "0.0", inclusive = false, message = "interval은 0보다 커야 합니다.")
    private Double interval;

    /** 대상 스킬에 승계할 수치 비율. */
    private Map<String, Double> inherit;
}
