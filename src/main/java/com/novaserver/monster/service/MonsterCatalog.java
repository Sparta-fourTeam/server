package com.novaserver.monster.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.novaserver.condex.entity.MonsterGrade;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

/** 클라이언트 dev의 확정 Monsters 테이블을 그대로 조회하며 시작 전에 전체 참조를 검증한다. */
@Component
public class MonsterCatalog {
    private static final Set<String> MONSTER_FIELDS =
            Set.of(
                    "Id",
                    "Hp",
                    "Damage",
                    "Speed",
                    "AttackInterval",
                    "AttackRange",
                    "ProjectileSpeed",
                    "Grade",
                    "BurstCount",
                    "BurstInterval",
                    "Resists",
                    "CastResists",
                    "BlocksProjectile",
                    "Passives");
    private static final Set<String> ELEMENTS =
            Set.of("Neutral", "Fire", "Ice", "Wind", "Lightning", "Earth");
    private static final Set<String> CAST_TYPES =
            Set.of("Projectile", "Instant", "Area", "Beam", "Chain");
    private static final Set<String> STATUSES =
            Set.of(
                    "Stun",
                    "Burn",
                    "Paralysis",
                    "Freeze",
                    "Slow",
                    "Frostbite",
                    "Vulnerability",
                    "AllDebuffs");

    private final String json;

    /** 서버 배포물의 몬스터 원문을 읽고 불완전하면 시작을 거부한다. */
    public MonsterCatalog() throws IOException {
        var resource = new ClassPathResource("game-data/Monsters.json");
        json = new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        validate(new ObjectMapper().readTree(json));
    }

    /** 버전은 기존 /data/version의 Monsters 값과 X-Data-Version 헤더로 전달한다. */
    public String json() {
        return json;
    }

    /** 모든 수치·등급·패시브·소환 대상을 한 번에 확인해 잘못된 카탈로그로 서버가 시작하지 않게 한다. */
    public static void validate(JsonNode catalog) {
        require(catalog != null && catalog.isArray() && !catalog.isEmpty(), "Monsters 목록이 비었습니다");
        Map<Integer, JsonNode> byId = new HashMap<>();
        for (JsonNode row : catalog) {
            require(row.isObject(), "Monsters 행은 객체여야 합니다");
            fields(row, MONSTER_FIELDS);
            int id = positiveInt(row, "Id");
            require(byId.putIfAbsent(id, row) == null, "Monsters ID가 중복됩니다: " + id);
            require(positiveInt(row, "Hp") > 0, "Hp가 잘못되었습니다: " + id);
            require(integer(row, "Damage", 0) >= 0, "Damage가 잘못되었습니다: " + id);
            number(row, "Speed", 0, true);
            number(row, "AttackInterval", 0, false);
            number(row, "AttackRange", 0, true);
            number(row, "ProjectileSpeed", 0, true);
            String grade = string(row, "Grade");
            require(MonsterGrade.fromName(grade).isPresent(), "Grade가 잘못되었습니다: " + id);
            int burst = row.has("BurstCount") ? positiveInt(row, "BurstCount") : 1;
            double burstInterval =
                    row.has("BurstInterval") ? number(row, "BurstInterval", 0, true) : 0;
            require(
                    burst == 1
                            || (burstInterval > 0
                                    && number(row, "AttackInterval", 0, false)
                                            > burstInterval * (burst - 1)),
                    "BurstInterval이 잘못되었습니다: " + id);
            ratios(row.path("Resists"), ELEMENTS);
            ratios(row.path("CastResists"), CAST_TYPES);
            if (row.has("BlocksProjectile")) {
                require(
                        row.get("BlocksProjectile").isBoolean(),
                        "BlocksProjectile이 잘못되었습니다: " + id);
            }
            JsonNode passives = row.path("Passives");
            require(
                    passives.isMissingNode() || passives.isNull() || passives.isArray(),
                    "Passives가 잘못되었습니다: " + id);
            if (passives.isArray()) {
                for (JsonNode passive : passives) {
                    validatePassive(passive, id);
                }
            }
        }

        for (var entry : byId.entrySet()) {
            JsonNode passives = entry.getValue().path("Passives");
            if (!passives.isArray()) {
                continue;
            }
            for (JsonNode passive : passives) {
                if (!"Spawn".equals(passive.path("Kind").asText())) {
                    continue;
                }
                int targetId = passive.get("MonsterId").intValue();
                JsonNode target = byId.get(targetId);
                require(target != null, "소환 대상이 없습니다: " + entry.getKey() + " -> " + targetId);
                require(targetId != entry.getKey(), "자기 소환은 허용하지 않습니다: " + targetId);
                JsonNode targetPassives = target.path("Passives");
                if (targetPassives.isArray()) {
                    for (JsonNode next : targetPassives) {
                        require(
                                !"Spawn".equals(next.path("Kind").asText()),
                                "연쇄 소환은 허용하지 않습니다: " + entry.getKey() + " -> " + targetId);
                    }
                }
            }
        }
    }

    private static void validatePassive(JsonNode p, int id) {
        require(p != null && p.isObject(), "Passives 행이 잘못되었습니다: " + id);
        String kind = string(p, "Kind");
        switch (kind) {
            case "Spawn" -> {
                fields(p, Set.of("Kind", "Trigger", "MonsterId", "Count", "Interval", "MaxTotal"));
                String trigger = string(p, "Trigger");
                require(
                        Set.of("OnDeath", "Interval").contains(trigger),
                        "Spawn Trigger가 잘못되었습니다: " + id);
                positiveInt(p, "MonsterId");
                positiveInt(p, "Count");
                if (trigger.equals("Interval")) {
                    number(p, "Interval", 0, false);
                    positiveInt(p, "MaxTotal");
                }
            }
            case "Immunity" -> {
                fields(p, Set.of("Kind", "Statuses"));
                statuses(p, true);
            }
            case "Modifier" -> {
                fields(p, Set.of("Kind", "Target", "Multiplier"));
                require(
                        Set.of("KnockbackDistance", "BurnDuration").contains(string(p, "Target")),
                        "Modifier Target이 잘못되었습니다: " + id);
                number(p, "Multiplier", 0, true);
            }
            case "Shield" -> {
                fields(p, Set.of("Kind", "Trigger", "Hits", "Duration", "Chance", "Statuses"));
                String trigger = string(p, "Trigger");
                require(
                        Set.of("Start", "OnHit").contains(trigger),
                        "Shield Trigger가 잘못되었습니다: " + id);
                positiveInt(p, "Hits");
                if (p.has("Duration")) {
                    number(p, "Duration", 0, true);
                }
                if (trigger.equals("OnHit")) {
                    chance(p);
                }
                statuses(p, false);
            }
            case "Evade" -> {
                fields(p, Set.of("Kind", "Chance"));
                chance(p);
            }
            case "LowHp" -> {
                fields(
                        p,
                        Set.of(
                                "Kind",
                                "HpRatio",
                                "Duration",
                                "HealRatio",
                                "Invulnerable",
                                "SpeedMultiplier",
                                "Statuses"));
                double hpRatio = number(p, "HpRatio", 0, false);
                require(hpRatio < 1, "LowHp HpRatio가 잘못되었습니다: " + id);
                number(p, "Duration", 0, false);
                double heal = p.has("HealRatio") ? number(p, "HealRatio", 0, true) : 0;
                double speed =
                        p.has("SpeedMultiplier") ? number(p, "SpeedMultiplier", 0, false) : 1;
                boolean invulnerable = p.path("Invulnerable").asBoolean(false);
                if (p.has("Invulnerable")) {
                    require(
                            p.get("Invulnerable").isBoolean(),
                            "LowHp Invulnerable이 잘못되었습니다: " + id);
                }
                statuses(p, false);
                require(
                        heal > 0 || speed != 1 || invulnerable || p.path("Statuses").size() > 0,
                        "LowHp 효과가 비었습니다: " + id);
            }
            case "HealOnHit" -> {
                fields(
                        p,
                        Set.of(
                                "Kind",
                                "HealRatio",
                                "SpeedMultiplier",
                                "Duration",
                                "Chance",
                                "Element"));
                double heal = p.has("HealRatio") ? number(p, "HealRatio", 0, true) : 0;
                double speed =
                        p.has("SpeedMultiplier") ? number(p, "SpeedMultiplier", 0, false) : 1;
                require(heal > 0 || speed != 1, "HealOnHit 효과가 비었습니다: " + id);
                if (speed != 1) {
                    number(p, "Duration", 0, false);
                }
                chance(p);
                if (p.has("Element")
                        && !p.get("Element").isNull()
                        && !p.get("Element").asText().isEmpty()) {
                    require(
                            ELEMENTS.contains(string(p, "Element")),
                            "HealOnHit Element가 잘못되었습니다: " + id);
                }
            }
            case "SpeedBoost" -> {
                fields(p, Set.of("Kind", "Interval", "Duration", "SpeedMultiplier"));
                number(p, "Interval", 0, false);
                number(p, "Duration", 0, false);
                number(p, "SpeedMultiplier", 0, false);
            }
            default -> throw new IllegalArgumentException("알 수 없는 패시브 Kind: " + kind);
        }
    }

    private static void statuses(JsonNode p, boolean required) {
        JsonNode values = p.path("Statuses");
        if (values.isMissingNode() || values.isNull()) {
            require(!required, "Statuses가 비었습니다");
            return;
        }
        require(values.isArray() && (!required || !values.isEmpty()), "Statuses가 잘못되었습니다");
        for (JsonNode status : values) {
            require(
                    status.isTextual() && STATUSES.contains(status.textValue()),
                    "알 수 없는 상태이상: " + status);
        }
    }

    private static void chance(JsonNode p) {
        if (p.has("Chance")) {
            require(number(p, "Chance", 0, false) <= 1, "Chance가 잘못되었습니다");
        }
    }

    private static void ratios(JsonNode ratios, Set<String> names) {
        if (ratios.isMissingNode() || ratios.isNull()) {
            return;
        }
        require(ratios.isObject(), "Resists/CastResists가 잘못되었습니다");
        ratios.fields()
                .forEachRemaining(
                        entry -> {
                            require(
                                    names.contains(entry.getKey()),
                                    "저항 키가 잘못되었습니다: " + entry.getKey());
                            require(
                                    entry.getValue().isNumber()
                                            && Double.isFinite(entry.getValue().doubleValue())
                                            && entry.getValue().doubleValue() >= -1,
                                    "저항 값이 잘못되었습니다: " + entry.getKey());
                        });
    }

    private static String string(JsonNode row, String key) {
        JsonNode value = row.get(key);
        require(
                value != null && value.isTextual() && !value.textValue().isBlank(),
                key + "가 잘못되었습니다");
        return value.textValue();
    }

    private static int positiveInt(JsonNode row, String key) {
        int value = integer(row, key, 1);
        require(value > 0, key + "는 양수여야 합니다");
        return value;
    }

    private static int integer(JsonNode row, String key, int minimum) {
        JsonNode value = row.get(key);
        require(
                value != null
                        && value.isIntegralNumber()
                        && value.canConvertToInt()
                        && value.intValue() >= minimum,
                key + "가 잘못되었습니다");
        return value.intValue();
    }

    private static double number(JsonNode row, String key, double minimum, boolean inclusive) {
        JsonNode value = row.get(key);
        require(
                value != null
                        && value.isNumber()
                        && Double.isFinite(value.doubleValue())
                        && (inclusive
                                ? value.doubleValue() >= minimum
                                : value.doubleValue() > minimum),
                key + "가 잘못되었습니다");
        return value.doubleValue();
    }

    private static void fields(JsonNode row, Set<String> names) {
        row.fieldNames()
                .forEachRemaining(name -> require(names.contains(name), "알 수 없는 필드: " + name));
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalArgumentException(message);
        }
    }
}
