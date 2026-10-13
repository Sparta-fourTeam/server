package com.novaserver.auth.social;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.novaserver.global.error.ErrorCode;
import com.novaserver.user.Provider;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.web.server.ResponseStatusException;

class SocialTokenVerifierTest {

    private static final String GOOGLE_ISS = "https://accounts.google.com";
    private static final String CLIENT_ID = "our-app.apps.googleusercontent.com";
    private static final SocialProperties.Provider GOOGLE =
            new SocialProperties.Provider(
                    "unused", List.of(GOOGLE_ISS, "accounts.google.com"), List.of(CLIENT_ID));

    private static KeyPair googleKey;
    private static KeyPair otherKey;

    @BeforeAll
    static void generateKeys() throws NoSuchAlgorithmException {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        googleKey = generator.generateKeyPair();
        otherKey = generator.generateKeyPair();
    }

    @Test
    @DisplayName("서명·iss·aud·exp가 모두 맞으면 sub를 반환한다")
    void shouldReturnSub_whenTokenIsValid() throws JOSEException {
        String idToken = sign(googleKey, claims(GOOGLE_ISS, CLIENT_ID, future()));

        assertThat(verifier(GOOGLE).verify(Provider.GOOGLE, idToken)).isEqualTo("google-sub-1");
    }

    @Test
    @DisplayName("Google의 https 없는 발급자 형태(accounts.google.com)도 허용한다")
    void shouldAcceptAlternativeIssuer() throws JOSEException {
        String idToken = sign(googleKey, claims("accounts.google.com", CLIENT_ID, future()));

        assertThat(verifier(GOOGLE).verify(Provider.GOOGLE, idToken)).isEqualTo("google-sub-1");
    }

    @Test
    @DisplayName("aud 목록 중 하나라도 우리 client ID면 허용한다")
    void shouldAccept_whenOneOfAudiencesMatches() throws JOSEException {
        JWTClaimsSet claims =
                new JWTClaimsSet.Builder(claims(GOOGLE_ISS, CLIENT_ID, future()))
                        .audience(List.of("someone-else", CLIENT_ID))
                        .build();

        assertThat(verifier(GOOGLE).verify(Provider.GOOGLE, sign(googleKey, claims)))
                .isEqualTo("google-sub-1");
    }

    @Test
    @DisplayName("다른 앱용으로 발급된 토큰(aud 불일치)이면 401 INVALID_SOCIAL_TOKEN")
    void shouldReject_whenAudienceIsOtherApp() throws JOSEException {
        String idToken = sign(googleKey, claims(GOOGLE_ISS, "other-app", future()));

        assertError(
                () -> verifier(GOOGLE).verify(Provider.GOOGLE, idToken),
                ErrorCode.INVALID_SOCIAL_TOKEN);
    }

    @Test
    @DisplayName("발급자가 다르면 401 INVALID_SOCIAL_TOKEN")
    void shouldReject_whenIssuerIsWrong() throws JOSEException {
        String idToken = sign(googleKey, claims("https://evil.example.com", CLIENT_ID, future()));

        assertError(
                () -> verifier(GOOGLE).verify(Provider.GOOGLE, idToken),
                ErrorCode.INVALID_SOCIAL_TOKEN);
    }

    @Test
    @DisplayName("만료된 토큰이면 401 INVALID_SOCIAL_TOKEN")
    void shouldReject_whenExpired() throws JOSEException {
        Date expired = Date.from(Instant.now().minusSeconds(3600));
        String idToken = sign(googleKey, claims(GOOGLE_ISS, CLIENT_ID, expired));

        assertError(
                () -> verifier(GOOGLE).verify(Provider.GOOGLE, idToken),
                ErrorCode.INVALID_SOCIAL_TOKEN);
    }

    @Test
    @DisplayName("다른 키로 서명한 토큰이면 401 INVALID_SOCIAL_TOKEN")
    void shouldReject_whenSignedWithOtherKey() throws JOSEException {
        String idToken = sign(otherKey, claims(GOOGLE_ISS, CLIENT_ID, future()));

        assertError(
                () -> verifier(GOOGLE).verify(Provider.GOOGLE, idToken),
                ErrorCode.INVALID_SOCIAL_TOKEN);
    }

    @Test
    @DisplayName("형식이 잘못된 토큰이면 401 INVALID_SOCIAL_TOKEN")
    void shouldReject_whenMalformed() {
        assertError(
                () -> verifier(GOOGLE).verify(Provider.GOOGLE, "not-a-jwt"),
                ErrorCode.INVALID_SOCIAL_TOKEN);
    }

    @Test
    @DisplayName("client ID가 설정되지 않았으면 모든 토큰을 거절한다")
    void shouldRejectAll_whenNoClientIdConfigured() throws JOSEException {
        SocialProperties.Provider noClientIds =
                new SocialProperties.Provider("unused", List.of(GOOGLE_ISS), List.of());
        String idToken = sign(googleKey, claims(GOOGLE_ISS, CLIENT_ID, future()));

        assertError(
                () -> verifier(noClientIds).verify(Provider.GOOGLE, idToken),
                ErrorCode.INVALID_SOCIAL_TOKEN);
    }

    @Test
    @DisplayName("GUEST처럼 소셜이 아닌 provider면 400 UNSUPPORTED_PROVIDER")
    void shouldReject_whenProviderIsNotSocial() {
        assertError(
                () -> verifier(GOOGLE).verify(Provider.GUEST, "token"),
                ErrorCode.UNSUPPORTED_PROVIDER);
    }

    @Test
    @DisplayName("공개키를 받아오지 못하면 503 SOCIAL_PROVIDER_UNAVAILABLE")
    void shouldReturnUnavailable_whenJwksUnreachable() {
        JwtDecoder decoder = mock(JwtDecoder.class);
        when(decoder.decode(anyString()))
                .thenThrow(new JwtException("Couldn't retrieve remote JWK set"));
        SocialTokenVerifier verifier = new SocialTokenVerifier(Map.of(Provider.GOOGLE, decoder));

        assertError(
                () -> verifier.verify(Provider.GOOGLE, "token"),
                ErrorCode.SOCIAL_PROVIDER_UNAVAILABLE);
    }

    private static SocialTokenVerifier verifier(SocialProperties.Provider provider) {
        NimbusJwtDecoder decoder =
                NimbusJwtDecoder.withPublicKey((RSAPublicKey) googleKey.getPublic()).build();
        decoder.setJwtValidator(SocialTokenVerifier.validator(provider));
        return new SocialTokenVerifier(Map.of(Provider.GOOGLE, decoder));
    }

    private static JWTClaimsSet claims(String issuer, String audience, Date expiresAt) {
        return new JWTClaimsSet.Builder()
                .issuer(issuer)
                .audience(audience)
                .subject("google-sub-1")
                .issueTime(new Date())
                .expirationTime(expiresAt)
                .build();
    }

    private static Date future() {
        return Date.from(Instant.now().plusSeconds(3600));
    }

    private static String sign(KeyPair key, JWTClaimsSet claims) throws JOSEException {
        SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.RS256), claims);
        jwt.sign(new RSASSASigner(key.getPrivate()));
        return jwt.serialize();
    }

    private static void assertError(ThrowingCallable call, ErrorCode errorCode) {
        assertThatThrownBy(call)
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(
                        e -> {
                            ResponseStatusException ex = (ResponseStatusException) e;
                            assertThat(ex.getStatusCode()).isEqualTo(errorCode.getStatus());
                            assertThat(ex.getReason()).isEqualTo(errorCode.getMessage());
                        });
    }
}
