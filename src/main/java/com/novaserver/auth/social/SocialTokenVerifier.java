package com.novaserver.auth.social;

import com.novaserver.global.error.ErrorCode;
import com.novaserver.user.Provider;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

/**
 * Google·Apple ID 토큰을 검증하고 사용자 식별자(sub)를 꺼낸다.
 *
 * <p>Provider별 decoder는 처음 쓸 때 JWKS 주소에서 공개키를 받아 캐싱하고, 모르는 kid의 토큰이 오면 공개키를 다시 받는다. 서명과 exp·nbf에 더해
 * iss(발급자)와 aud(우리 앱의 client ID)를 검증한다.
 */
@Component
@EnableConfigurationProperties(SocialProperties.class)
public class SocialTokenVerifier {

    private static final Duration JWKS_TIMEOUT = Duration.ofSeconds(3);

    private final Map<Provider, JwtDecoder> decoders;

    /**
     * 설정으로 Google·Apple decoder를 만든다.
     *
     * @param properties 소셜 로그인 설정
     */
    @Autowired
    public SocialTokenVerifier(SocialProperties properties) {
        RestTemplate restTemplate = jwksRestTemplate();
        this.decoders =
                Map.of(
                        Provider.GOOGLE, decoder(properties.google(), restTemplate),
                        Provider.APPLE, decoder(properties.apple(), restTemplate));
    }

    // 테스트용: 직접 만든 키로 서명한 토큰을 검증할 decoder를 주입한다
    SocialTokenVerifier(Map<Provider, JwtDecoder> decoders) {
        this.decoders = decoders;
    }

    /**
     * ID 토큰을 검증하고 sub를 반환한다.
     *
     * @param provider ID 토큰을 발급한 소셜 provider
     * @param idToken 클라이언트가 받은 ID 토큰 원문
     * @return 해당 provider 안에서 사용자를 구분하는 고유 ID
     * @throws org.springframework.web.server.ResponseStatusException 소셜 provider가 아니면
     *     UNSUPPORTED_PROVIDER, 서명·만료·발급자·client ID 검증에 실패하면 INVALID_SOCIAL_TOKEN, 공개키를 받아오지 못하면
     *     SOCIAL_PROVIDER_UNAVAILABLE
     */
    public String verify(Provider provider, String idToken) {
        JwtDecoder decoder = decoders.get(provider);
        if (decoder == null) {
            throw ErrorCode.UNSUPPORTED_PROVIDER.exception();
        }

        Jwt jwt;
        try {
            jwt = decoder.decode(idToken);
        } catch (BadJwtException e) {
            throw ErrorCode.INVALID_SOCIAL_TOKEN.exception();
        } catch (JwtException e) {
            throw ErrorCode.SOCIAL_PROVIDER_UNAVAILABLE.exception();
        }

        String sub = jwt.getSubject();
        if (sub == null || sub.isBlank()) {
            throw ErrorCode.INVALID_SOCIAL_TOKEN.exception();
        }
        return sub;
    }

    static OAuth2TokenValidator<Jwt> validator(SocialProperties.Provider provider) {
        List<String> issuers = provider.issuers();
        List<String> clientIds = provider.clientIds();
        return JwtValidators.createDefaultWithValidators(
                List.of(
                        new JwtClaimValidator<Object>(
                                JwtClaimNames.ISS,
                                iss -> iss != null && issuers.contains(iss.toString())),
                        new JwtClaimValidator<List<String>>(
                                JwtClaimNames.AUD,
                                aud -> aud != null && aud.stream().anyMatch(clientIds::contains))));
    }

    private static JwtDecoder decoder(
            SocialProperties.Provider provider, RestTemplate restTemplate) {
        NimbusJwtDecoder decoder =
                NimbusJwtDecoder.withJwkSetUri(provider.jwkSetUri())
                        .restOperations(restTemplate)
                        .build();
        decoder.setJwtValidator(validator(provider));
        return decoder;
    }

    // 공개키 서버가 느려도 로그인 요청이 오래 묶이지 않게 타임아웃을 건다
    private static RestTemplate jwksRestTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(JWKS_TIMEOUT);
        factory.setReadTimeout(JWKS_TIMEOUT);
        return new RestTemplate(factory);
    }
}
