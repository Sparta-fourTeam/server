package com.novaserver.clientpolicy;

/** Android 정수·iOS 점 구분 build를 동일한 숫자 성분 규칙으로 비교한다. */
public final class BuildNumber {
    private BuildNumber() {}

    /** 1~10자리 숫자 성분을 최대 네 개 허용한다. 부호·공백·버전 접미사는 거절한다. */
    public static boolean valid(String value) {
        return value != null && value.matches("[0-9]{1,10}(\\.[0-9]{1,10}){0,3}");
    }

    /** 1과 1.0은 같고 1.10은 1.9보다 크다. 입력은 먼저 검증해야 한다. */
    public static int compare(String left, String right) {
        if (!valid(left) || !valid(right)) {
            throw new IllegalArgumentException("잘못된 build 번호");
        }
        String[] a = left.split("\\.");
        String[] b = right.split("\\.");
        for (int i = 0; i < Math.max(a.length, b.length); i++) {
            int comparison =
                    Long.compare(
                            i < a.length ? Long.parseLong(a[i]) : 0,
                            i < b.length ? Long.parseLong(b[i]) : 0);
            if (comparison != 0) {
                return comparison;
            }
        }
        return 0;
    }
}
