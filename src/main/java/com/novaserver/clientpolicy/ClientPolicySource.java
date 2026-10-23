package com.novaserver.clientpolicy;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/** 공유 정책 파일을 읽어 공개 상태와 GAME 차단에 같은 정책을 제공한다. */
@Component
public class ClientPolicySource {
    private final String file;
    private final ObjectMapper mapper = new ObjectMapper();

    public ClientPolicySource(@Value("${app.client-policy.file:}") String file) {
        this.file = file;
    }

    /** 미지정은 차단 OFF, 지정한 파일의 읽기·검증 실패는 점검 상태로 반환한다. */
    public ClientPolicy current() {
        if (file == null || file.isBlank()) {
            return ClientPolicy.disabled();
        }
        try {
            byte[] bytes = Files.readAllBytes(Path.of(file));
            if (bytes.length > 65536) {
                return ClientPolicy.unavailable();
            }
            var document = mapper.readTree(bytes);
            if (document == null
                    || !document.isObject()
                    || !document.path("enforcementEnabled").isBoolean()
                    || !document.path("allowLegacy").isBoolean()) {
                return ClientPolicy.unavailable();
            }
            ClientPolicy policy = mapper.treeToValue(document, ClientPolicy.class);
            return valid(policy) ? policy : ClientPolicy.unavailable();
        } catch (IOException | IllegalArgumentException e) {
            return ClientPolicy.unavailable();
        }
    }

    private boolean valid(ClientPolicy policy) {
        if (policy == null
                || policy.schemaVersion() != 1
                || policy.revision() == null
                || !policy.revision().matches("[A-Za-z0-9._:-]{1,128}")
                || policy.mode() == null
                || !Set.of("NORMAL", "DRAINING", "MAINTENANCE").contains(policy.mode())
                || policy.retryAfterSeconds() < 1
                || policy.retryAfterSeconds() > 3600
                || policy.platforms() == null
                || policy.message() == null) {
            return false;
        }
        return policy.platforms().entrySet().stream()
                .allMatch(
                        entry -> {
                            ClientPolicy.PlatformPolicy platform = entry.getValue();
                            return Set.of("android", "ios", "editor").contains(entry.getKey())
                                    && platform != null
                                    && BuildNumber.valid(platform.minimumBuild())
                                    && BuildNumber.valid(platform.recommendedBuild())
                                    && BuildNumber.compare(
                                                    platform.recommendedBuild(),
                                                    platform.minimumBuild())
                                            >= 0
                                    && validStoreUrl(entry.getKey(), platform.storeUrl());
                        });
    }

    private boolean validStoreUrl(String platform, String url) {
        if (url == null || url.isEmpty()) {
            return false;
        }
        try {
            URI uri = URI.create(url);
            return "https".equals(uri.getScheme())
                    && uri.getUserInfo() == null
                    && (("android".equals(platform) && "play.google.com".equals(uri.getHost()))
                            || ("ios".equals(platform) && "apps.apple.com".equals(uri.getHost()))
                            || ("editor".equals(platform)
                                    && "example.invalid".equals(uri.getHost())));
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
