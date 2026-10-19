package com.novaserver.notice.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.novaserver.notice.dto.NoticeRequest;
import com.novaserver.notice.dto.NoticeResponse;
import com.novaserver.notice.entity.Notice;
import com.novaserver.notice.repository.NoticeRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class NoticeServiceTest {

    @Mock private NoticeRepository noticeRepository;
    @Mock private Clock clock;

    @InjectMocks private NoticeService noticeService;

    @Test
    @DisplayName("공지를 등록하면 저장한다")
    void shouldSaveNotice_onCreateNotice() {
        // given
        NoticeRequest request = new NoticeRequest();
        ReflectionTestUtils.setField(request, "title", "정기 점검 안내");
        ReflectionTestUtils.setField(request, "content", "오늘 03:00~05:00 점검입니다");
        Notice saved = withId(new Notice("정기 점검 안내", "오늘 03:00~05:00 점검입니다"));
        when(noticeRepository.save(any(Notice.class))).thenReturn(saved);

        // when
        NoticeResponse response = noticeService.createNotice(request);

        // then
        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.title()).isEqualTo("정기 점검 안내");
        assertThat(response.content()).isEqualTo("오늘 03:00~05:00 점검입니다");
    }

    @Test
    @DisplayName("최근 공지를 최신순으로 반환한다")
    void shouldReturnRecentNotices_inNewestFirstOrder() {
        // given
        Notice newer = withId(new Notice("새 공지", "내용"));
        when(noticeRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(0, 20)))
                .thenReturn(List.of(newer));

        // when
        List<NoticeResponse> responses = noticeService.getRecentNotices();

        // then
        assertThat(responses).hasSize(1);
        assertThat(responses.getFirst().title()).isEqualTo("새 공지");
    }

    @Test
    @DisplayName("등록된 지 7일 지난 공지를 지운다")
    void shouldDeleteNoticesOlderThanSevenDays() {
        // given
        when(clock.instant()).thenReturn(Instant.parse("2026-10-19T04:00:00Z"));

        // when
        noticeService.deleteOldNotices();

        // then
        verify(noticeRepository).deleteByCreatedAtBefore(Instant.parse("2026-10-12T04:00:00Z"));
    }

    private static Notice withId(Notice notice) {
        ReflectionTestUtils.setField(notice, "id", 1L);
        ReflectionTestUtils.setField(notice, "createdAt", Instant.parse("2026-10-19T00:00:00Z"));
        return notice;
    }
}
