package com.novaserver.monster.dto;

import com.novaserver.monster.entity.CastType;
import com.novaserver.monster.entity.Element;
import com.novaserver.monster.entity.EnemyType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.annotation.JsonNaming;

/** 몬스터 등록 요청. 필드 이름은 클라이언트 Monsters JSON과 같은 PascalCase다. */
@Getter
@JsonNaming(PropertyNamingStrategies.UpperCamelCaseStrategy.class)
public class MonsterRequest {

    @NotNull private Long id;

    @NotNull
    @Min(1)
    private Integer hp;

    @NotNull
    @Min(0)
    private Integer damage;

    @NotNull @PositiveOrZero private Double speed;

    @NotNull @Positive private Double attackInterval;

    @NotNull @PositiveOrZero private Double attackRange;

    @PositiveOrZero private Double projectileSpeed;

    @NotNull(message = "EnemyType은 필수입니다.")
    private EnemyType grade;

    @Min(1)
    private Integer burstCount;

    @PositiveOrZero private Double burstInterval;

    private Map<Element, @NotNull @DecimalMin("-1") Double> resists;

    private Map<CastType, @NotNull @DecimalMin("-1") Double> castResists;

    private boolean blocksProjectile;

    private List<@Valid MonsterPassiveRequest> passives;

    /**
     * 연타 설정을 검사한다. 연타(BurstCount > 1)라면 타격 간격이 있어야 하고, 연타가 다음 공격 전에 끝나야 한다.
     *
     * @return 연타 설정이 올바르면 true. AttackInterval 누락은 @NotNull이 따로 보고한다.
     */
    @AssertTrue(message = "연속 공격 설정이 올바르지 않습니다")
    public boolean isBurstValid() {
        int count = burstCount != null ? burstCount : 1;
        double interval = burstInterval != null ? burstInterval : 0;

        if (count > 1 && interval <= 0) {
            return false;
        }
        if (attackInterval == null || count == 1) {
            return true;
        }
        return attackInterval > interval * (count - 1);
    }
}
