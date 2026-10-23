package com.novaserver.monster.dto;

import com.novaserver.monster.entity.Element;
import com.novaserver.monster.entity.PassiveKind;
import com.novaserver.monster.entity.PassiveTrigger;
import com.novaserver.monster.entity.StatusImmunity;
import com.novaserver.monster.entity.Target;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import java.util.Set;
import lombok.Getter;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

/** 몬스터 패시브 요청. Kind마다 필요한 값이 달라 모든 값을 생략 가능하게 받고, Kind별 필수 검사는 MonsterPassiveFactory가 한다. */
@Getter
@JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
public class MonsterPassiveRequest {

    @NotNull private PassiveKind kind;

    private PassiveTrigger trigger;

    private Long monsterId;

    @Positive private Integer count;

    @Positive private Double interval;

    @Positive private Integer maxTotal;

    private Set<@NotNull StatusImmunity> statuses;

    private Target target;

    private Double multiplier;

    @Positive private Integer hits;

    @PositiveOrZero private Double duration;

    @DecimalMin(value = "0", inclusive = false)
    @DecimalMax("1")
    private Double chance;

    @DecimalMin(value = "0", inclusive = false)
    @DecimalMax(value = "1", inclusive = false)
    private Double hpRatio;

    @PositiveOrZero private Double healRatio;

    private Boolean invulnerable;

    @Positive private Double speedMultiplier;

    private Element element;
}
