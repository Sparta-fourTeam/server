package com.novaserver.battle.progress;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.novaserver.global.error.ErrorCode;
import com.novaserver.monster.service.MonsterCatalog;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/** DB에 게시된 단일 전투 원본에서 스폰 정책을 만든다. */
@Component
public class BattlePolicySource {
    private final BattleSourceRepository repository;
    private volatile BattlePolicySource cached;
    private final String monstersJson;
    private final String stagesJson;
    private final String combatJson;
    private final Manifest manifest;
    private final Map<Integer, StageProgressPolicy> stages;
    private final java.util.Set<Long> monsterIds;

    /** 원본은 관리 API로 DB에 게시한다. 서버 파일이나 로컬 기본값으로 대체하지 않는다. */
    @Autowired
    public BattlePolicySource(BattleSourceRepository repository) {
        this.repository = repository;
        stagesJson = null;
        monstersJson = null;
        combatJson = null;
        manifest = null;
        stages = null;
        monsterIds = null;
    }

    BattlePolicySource(String stageJson, String monsterJson, String combatJson) throws IOException {
        repository = null;
        monstersJson = monsterJson;
        this.stagesJson = stageJson;
        this.combatJson = combatJson;
        var json = new ObjectMapper();
        JsonNode monsters = json.readTree(monsterJson);
        MonsterCatalog.validate(monsters);
        final var descendants = new TreeMap<Long, List<StageProgressPolicy.SpawnLimit>>();
        for (JsonNode monster : monsters) {
            var children = new ArrayList<StageProgressPolicy.SpawnLimit>();
            for (JsonNode passive : monster.path("Passives")) {
                if ("Spawn".equals(passive.path("Kind").asText())) {
                    String count =
                            "OnDeath".equals(passive.path("Trigger").asText())
                                    ? "Count"
                                    : "MaxTotal";
                    children.add(
                            new StageProgressPolicy.SpawnLimit(
                                    passive.get("MonsterId").longValue(),
                                    passive.get(count).intValue()));
                }
            }
            descendants.put(monster.get("Id").longValue(), List.copyOf(children));
        }
        JsonNode combat = json.readTree(combatJson);
        require(combat != null && combat.isObject(), "Combat은 객체여야 합니다");
        final BigDecimal maxScale =
                number(combat, "BaseTimeScale", false).multiply(BigDecimal.valueOf(2));
        JsonNode rows = json.readTree(stageJson);
        require(rows != null && rows.isArray() && !rows.isEmpty(), "Stages가 비었습니다");
        final var generated = new TreeMap<Integer, StageProgressPolicy>();
        for (JsonNode row : rows) {
            final int id = positive(row, "Id");
            JsonNode spawn = row.path("Spawn");
            BigDecimal minimum = number(spawn, "IntervalMin", false);
            require(
                    number(spawn, "IntervalMax", false).compareTo(minimum) >= 0,
                    "IntervalMax가 IntervalMin보다 작습니다");
            number(spawn, "Cooldown", true);
            JsonNode waves = row.path("Waves");
            require(waves.isArray() && waves.size() == 20, "스테이지는 20웨이브여야 합니다");
            var limits = new ArrayList<Map<Long, Integer>>();
            for (JsonNode wave : waves) {
                JsonNode spawns = wave.path("Spawns");
                require(spawns.isArray() && !spawns.isEmpty(), "웨이브 Spawns가 비었습니다");
                var counts = new TreeMap<Long, Integer>();
                for (JsonNode entry : spawns) {
                    long monster = positive(entry, "MonsterId");
                    require(descendants.containsKey(monster), "알 수 없는 스폰 몬스터입니다");
                    require(
                            counts.putIfAbsent(monster, positive(entry, "Count")) == null,
                            "스폰 몬스터가 중복됩니다");
                }
                limits.add(Map.copyOf(counts));
            }
            require(
                    generated.putIfAbsent(
                                    id,
                                    new StageProgressPolicy(
                                            maxScale,
                                            minimum,
                                            List.copyOf(limits),
                                            Map.copyOf(descendants),
                                            0))
                            == null,
                    "스테이지 ID가 중복됩니다");
        }
        stages = Map.copyOf(generated);
        monsterIds = java.util.Set.copyOf(descendants.keySet());
        var hashes = new LinkedHashMap<String, String>();
        hashes.put("Stages", sha256(stageJson));
        hashes.put("Monsters", sha256(monsterJson));
        hashes.put("Combat", sha256(combatJson));
        String revision =
                sha256(
                        "battle-policy-v1\n"
                                + hashes.get("Stages")
                                + "\n"
                                + hashes.get("Monsters")
                                + "\n"
                                + hashes.get("Combat")
                                + "\n");
        manifest =
                new Manifest(
                        1, revision, Map.copyOf(hashes), maxScale, List.copyOf(generated.keySet()));
    }

    /** 현재 원본 세트의 내용 주소. 숫자 테이블 버전과 달리 실제 원문 변화에 함께 바뀐다. */
    public String revision() {
        return snapshot().manifest.sourceRevision();
    }

    /** 같은 몬스터 원문에서 확인한 ID만 내부 정책 참조로 허용한다. */
    public boolean containsMonster(Long id) {
        return snapshot().monsterIds.contains(id);
    }

    /** 원문별 hash와 묶음 revision을 한 응답으로 제공한다. */
    public Manifest manifest() {
        return snapshot().manifest;
    }

    /** 정책을 생성한 Stages 원문이다. */
    public String stagesJson() {
        return snapshot().stagesJson;
    }

    /** 출시 배속을 계산한 Combat 원문이다. */
    public String combatJson() {
        return snapshot().combatJson;
    }

    /** 반복 버스트·분열·소환을 포함한 가능 범위다. 고정 웨이브 합계를 처치 최대치로 쓰지 않는다. */
    public StageProgressPolicy policy(Integer stageId) {
        var policy = snapshot().stages.get(stageId);
        if (policy == null) {
            throw ErrorCode.BATTLE_PROGRESS_UNAVAILABLE.exception();
        }
        return policy;
    }

    /** 동일 DB 행에서 읽은 불변 세트다. 여러 getter를 쓸 때에도 이 객체를 고정한다. */
    public BattlePolicySource snapshot() {
        if (repository == null) {
            return this;
        }
        var row =
                repository
                        .findById(1)
                        .orElseThrow(() -> ErrorCode.BATTLE_PROGRESS_UNAVAILABLE.exception());
        var current = cached;
        if (current != null && current.revision().equals(row.getSourceRevision())) {
            return current;
        }
        try {
            var parsed =
                    new BattlePolicySource(
                            row.getStagesJson(), row.getMonstersJson(), row.getCombatJson());
            if (!parsed.revision().equals(row.getSourceRevision())) {
                throw ErrorCode.BATTLE_PROGRESS_UNAVAILABLE.exception();
            }
            cached = parsed;
            return parsed;
        } catch (IOException | IllegalArgumentException invalid) {
            throw ErrorCode.BATTLE_PROGRESS_UNAVAILABLE.exception();
        }
    }

    /** 정책과 같은 DB 원본의 몬스터 데이터다. */
    public String monstersJson() {
        return snapshot().monstersJson;
    }

    private static int positive(JsonNode row, String name) {
        JsonNode value = row.path(name);
        require(
                value.isIntegralNumber() && value.canConvertToInt() && value.intValue() > 0,
                name + "은 양수 정수여야 합니다");
        return value.intValue();
    }

    private static BigDecimal number(JsonNode row, String name, boolean zero) {
        JsonNode value = row.path(name);
        require(value.isNumber(), name + "은 숫자여야 합니다");
        BigDecimal result = value.decimalValue();
        require(zero ? result.signum() >= 0 : result.signum() > 0, name + " 범위가 잘못되었습니다");
        return result;
    }

    private static String sha256(String value) {
        try {
            return HexFormat.of()
                    .formatHex(
                            MessageDigest.getInstance("SHA-256")
                                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException(impossible);
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new IllegalArgumentException(message);
        }
    }

    /** 공개 데이터 API에서 실제 원문 세트와 일치하는 fingerprint를 공급한다. */
    public record Manifest(
            int schemaVersion,
            String sourceRevision,
            Map<String, String> tables,
            BigDecimal maxTimeScale,
            List<Integer> stageIds) {}
}
