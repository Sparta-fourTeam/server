package com.novaserver.auth.social;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 소셜 로그인 설정. provider별 공개키(JWKS) 주소, 허용할 발급자와 client ID 목록을 담는다.
 *
 * @param google Google 설정
 * @param apple Apple 설정
 */
@ConfigurationProperties("app.social")
public record SocialProperties(Provider google, Provider apple) {

    /**
     * Provider 하나의 설정.
     *
     * @param jwkSetUri ID 토큰 서명 검증용 공개키 목록 주소
     * @param issuers ID 토큰의 iss로 허용할 발급자 목록
     * @param clientIds ID 토큰의 aud로 허용할 우리 앱의 client ID 목록
     */
    public record Provider(String jwkSetUri, List<String> issuers, List<String> clientIds) {}
}
