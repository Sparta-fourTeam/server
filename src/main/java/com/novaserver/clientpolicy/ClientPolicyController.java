package com.novaserver.clientpolicy;

import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 로그인 전에 읽는 앱 진입 정책. 관리자 상태 변경 API는 제공하지 않는다. */
@RestController
@RequestMapping("/client/status")
@RequiredArgsConstructor
public class ClientPolicyController {
    private final ClientPolicySource source;

    @GetMapping
    public ResponseEntity<ClientPolicy> status() {
        ClientPolicy policy = source.current();
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .header("X-Client-Policy-Revision", policy.revision())
                .body(policy);
    }
}
