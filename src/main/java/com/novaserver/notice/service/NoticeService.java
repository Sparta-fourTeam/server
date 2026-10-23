package com.novaserver.notice.service;

import com.novaserver.clientpolicy.ClientPolicySource;
import com.novaserver.notice.dto.NoticeRequest;
import com.novaserver.notice.dto.NoticeResponse;
import com.novaserver.notice.entity.Notice;
import com.novaserver.notice.repository.NoticeRepository;
import java.time.Clock;
import java.time.temporal.ChronoUnit;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 공지 등록·조회·정리.
 *
 * <p>SSE 방송은 여기서 하지 않는다. 이 트랜잭션이 커밋되기 전에 방송하면, 방송을 받은 클라이언트가 바로 목록을 다시 조회했을 때 아직 커밋 전이라 빠져있는 레이스가
 * 생긴다. 그래서 호출하는 쪽({@link com.novaserver.notice.controller.NoticeAdminController})이 이 메서드가 반환(=커밋)된
 * 뒤에 방송한다.
 */
@Service
@RequiredArgsConstructor
public class NoticeService {
    @Autowired(required = false)
    private ClientPolicySource clientPolicy;

    private static final int RECENT_NOTICE_LIMIT = 20;
    private static final int RETENTION_DAYS = 7;

    private final NoticeRepository noticeRepository;
    private final Clock clock;

    /** 공지를 저장한다. */
    @Transactional
    public NoticeResponse createNotice(NoticeRequest request) {
        Notice saved = noticeRepository.save(new Notice(request.getTitle(), request.getContent()));
        return NoticeResponse.from(saved);
    }

    /** 최근 공지를 최신순으로 반환한다. */
    public List<NoticeResponse> getRecentNotices() {
        return noticeRepository
                .findAllByOrderByCreatedAtDesc(PageRequest.of(0, RECENT_NOTICE_LIMIT))
                .stream()
                .map(NoticeResponse::from)
                .toList();
    }

    /** 등록된 지 7일 지난 공지를 매일 새벽에 지운다. */
    @Scheduled(cron = "0 0 4 * * *", zone = "UTC")
    @Transactional
    public void deleteOldNotices() {
        if (clientPolicy != null && clientPolicy.current().isMaintenance()) {
            return;
        }
        noticeRepository.deleteByCreatedAtBefore(
                clock.instant().minus(RETENTION_DAYS, ChronoUnit.DAYS));
    }
}
