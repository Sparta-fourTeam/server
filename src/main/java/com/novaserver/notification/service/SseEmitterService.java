package com.novaserver.notification.service;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * 유저별 SSE 연결을 관리하고 이벤트를 전송한다.
 *
 * <p>같은 유저가 여러 탭·기기로 접속할 수 있어 유저당 복수의 {@link SseEmitter}를 보관한다. 연결 유지는 주기적인 하트비트로 처리하며, 공지(전체
 * 브로드캐스트)·우편함(특정 유저 전송) 기능은 이후 {@link #sendTo}/{@link #broadcast}를 사용해 얹는다.
 */
@Slf4j
@Service
public class SseEmitterService {

    private static final long EMITTER_TIMEOUT = 60 * 60 * 1000L; // 1시간

    private final Map<Long, List<SseEmitter>> emitters = new ConcurrentHashMap<>();

    /** SSE 연결을 새로 등록하고, 연결 확인용 초기 이벤트를 보낸다. */
    public SseEmitter subscribe(Long userId) {
        SseEmitter emitter = new SseEmitter(EMITTER_TIMEOUT);
        emitters.computeIfAbsent(userId, key -> new CopyOnWriteArrayList<>()).add(emitter);

        emitter.onCompletion(() -> remove(userId, emitter));
        emitter.onTimeout(() -> remove(userId, emitter));
        emitter.onError(e -> remove(userId, emitter));

        send(emitter, "connect", "connected");
        return emitter;
    }

    /** 특정 유저의 모든 연결에 이벤트를 보낸다. */
    public void sendTo(Long userId, String eventName, Object data) {
        List<SseEmitter> userEmitters = emitters.get(userId);
        if (userEmitters == null) {
            return;
        }
        for (SseEmitter emitter : userEmitters) {
            send(emitter, eventName, data);
        }
    }

    /** 접속 중인 모든 유저에게 이벤트를 보낸다. */
    public void broadcast(String eventName, Object data) {
        for (Long userId : emitters.keySet()) {
            sendTo(userId, eventName, data);
        }
    }

    /** 프록시·로드밸런서의 idle timeout으로 연결이 끊기지 않도록 주기적으로 하트비트를 보낸다. */
    @Scheduled(fixedRate = 30_000L)
    public void heartbeat() {
        for (Long userId : emitters.keySet()) {
            sendTo(userId, "ping", "ping");
        }
    }

    private void send(SseEmitter emitter, String eventName, Object data) {
        try {
            emitter.send(SseEmitter.event().name(eventName).data(data));
        } catch (IOException | IllegalStateException e) {
            emitter.complete();
        }
    }

    private void remove(Long userId, SseEmitter emitter) {
        List<SseEmitter> userEmitters = emitters.get(userId);
        if (userEmitters == null) {
            return;
        }
        userEmitters.remove(emitter);
        emitters.computeIfPresent(userId, (key, list) -> list.isEmpty() ? null : list);
    }
}
