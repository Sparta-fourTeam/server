package com.novaserver.clientpolicy;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.Map;

/** 앱 시작 화면과 GAME 요청 차단이 함께 읽는 공개 정책. 비밀값을 넣지 않는다. */
public record ClientPolicy(
        int schemaVersion,
        String revision,
        boolean enforcementEnabled,
        String mode,
        String message,
        String estimatedEnd,
        int retryAfterSeconds,
        boolean allowLegacy,
        Map<String, PlatformPolicy> platforms) {

    /** 플랫폼별 설치 build 기준. iOS의 점 구분 build도 숫자 성분별로 비교한다. */
    public record PlatformPolicy(String minimumBuild, String recommendedBuild, String storeUrl) {}

    /** 파일을 지정하지 않은 최초 배포는 기존 이용자를 차단하지 않는다. */
    public static ClientPolicy disabled() {
        return new ClientPolicy(1, "initial-off", false, "NORMAL", "", null, 30, true, Map.of());
    }

    /** 명시적으로 지정한 정책 파일이 손상되면 정상이라고 응답하지 않는다. */
    public static ClientPolicy unavailable() {
        return new ClientPolicy(
                1,
                "policy-unavailable",
                true,
                "MAINTENANCE",
                "서버 상태를 확인하고 있습니다.",
                null,
                30,
                true,
                Map.of());
    }

    /** 현재 정책에서 GAME 쓰기와 신규 진입을 모두 막는지 반환한다. */
    @JsonIgnore
    public boolean isMaintenance() {
        return enforcementEnabled && "MAINTENANCE".equals(mode);
    }
}
