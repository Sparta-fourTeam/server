package com.novaserver.mail.service;

import com.novaserver.clientpolicy.ClientPolicySource;
import com.novaserver.global.error.ErrorCode;
import com.novaserver.item.entity.Item;
import com.novaserver.item.repository.ItemRepository;
import com.novaserver.mail.dto.MailBroadcastRequest;
import com.novaserver.mail.dto.MailBulkRequest;
import com.novaserver.mail.dto.MailItemRequest;
import com.novaserver.mail.dto.MailRequest;
import com.novaserver.mail.dto.MailResponse;
import com.novaserver.mail.entity.Mail;
import com.novaserver.mail.repository.MailRepository;
import com.novaserver.user.entity.User;
import com.novaserver.user.repository.UserRepository;
import com.novaserver.user.service.UserRewardService;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 우편 발송·조회·수령·정리.
 *
 * <p>SSE 전송은 여기서 하지 않는다. 이 트랜잭션이 커밋되기 전에 전송하면, 전송을 받은 클라이언트가 바로 목록을 다시 조회했을 때 아직 커밋 전이라 빠져있는 레이스가
 * 생긴다. 그래서 호출하는 쪽({@link com.novaserver.mail.controller.MailAdminController})이 이 메서드가 반환(=커밋)된 뒤에
 * 전송한다.
 */
@Service
@RequiredArgsConstructor
public class MailService {
    @Autowired(required = false)
    private ClientPolicySource clientPolicy;

    private static final int RETENTION_DAYS = 30;

    private final MailRepository mailRepository;
    private final UserRepository userRepository;
    private final ItemRepository itemRepository;
    private final UserRewardService userRewardService;
    private final Clock clock;

    /** 우편을 저장한다. 첨부 아이템은 같은 itemId면 수량을 합친다. */
    @Transactional
    public MailResponse sendMail(MailRequest request) {
        if (!userRepository.existsById(request.getUserId())) {
            throw ErrorCode.USER_NOT_FOUND.exception(request.getUserId());
        }
        User user = userRepository.getReferenceById(request.getUserId());

        Map<String, Integer> quantities = mergeQuantities(request.getItems());
        Map<String, Item> items = resolveItems(quantities);
        Mail mail =
                buildMail(
                        user,
                        request.getTitle(),
                        request.getContent(),
                        request.getExp(),
                        request.getGold(),
                        request.getGem(),
                        request.getEnergy(),
                        quantities,
                        items);

        Mail saved = mailRepository.save(mail);
        return MailResponse.from(saved);
    }

    /**
     * 같은 우편을 여러 유저에게 보낸다. 존재하지 않는 userId는 조용히 건너뛰고, 실제로 발송된 우편만 반환한다.
     *
     * @return 실제로 발송된 우편 목록. 크기가 곧 발송 성공 건수다
     */
    @Transactional
    public List<MailResponse> sendBulkMail(MailBulkRequest request) {
        List<User> recipients = userRepository.findAllById(request.getUserIds());
        return sendToAll(
                recipients,
                request.getTitle(),
                request.getContent(),
                request.getGold(),
                request.getItems());
    }

    /**
     * 같은 우편을 전체 유저에게 보낸다.
     *
     * @return 발송된 우편 목록. 크기가 곧 전체 유저 수다
     */
    @Transactional
    public List<MailResponse> sendBroadcastMail(MailBroadcastRequest request) {
        List<User> recipients = userRepository.findAll();
        return sendToAll(
                recipients,
                request.getTitle(),
                request.getContent(),
                request.getGold(),
                request.getItems());
    }

    private List<MailResponse> sendToAll(
            List<User> recipients,
            String title,
            String content,
            int gold,
            List<MailItemRequest> itemRequests) {
        Map<String, Integer> quantities = mergeQuantities(itemRequests);
        Map<String, Item> items = resolveItems(quantities);
        List<Mail> mails =
                recipients.stream()
                        .map(
                                user ->
                                        buildMail(
                                                user,
                                                title,
                                                content,
                                                0,
                                                gold,
                                                0,
                                                0,
                                                quantities,
                                                items))
                        .toList();
        return mailRepository.saveAll(mails).stream().map(MailResponse::from).toList();
    }

    private Mail buildMail(
            User user,
            String title,
            String content,
            int exp,
            int gold,
            int gem,
            int energy,
            Map<String, Integer> quantities,
            Map<String, Item> items) {
        Mail mail = new Mail(user, title, content, exp, gold, gem, energy);
        quantities.forEach((itemId, quantity) -> mail.addItem(items.get(itemId), quantity));
        return mail;
    }

    private Map<String, Item> resolveItems(Map<String, Integer> quantities) {
        if (quantities.isEmpty()) {
            return Map.of();
        }
        Map<String, Item> items =
                itemRepository.findAllById(quantities.keySet()).stream()
                        .collect(Collectors.toMap(Item::getItemId, item -> item));
        for (String itemId : quantities.keySet()) {
            if (!items.containsKey(itemId)) {
                throw ErrorCode.ITEM_NOT_FOUND.exception(itemId);
            }
        }
        return items;
    }

    /** 유저의 우편함을 최신순으로 반환한다. */
    public List<MailResponse> getMails(Long userId) {
        return mailRepository.findAllByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(MailResponse::from)
                .toList();
    }

    /** 우편을 수령한다. 경험치·골드·젬·에너지나 첨부 아이템이 있으면 지급한다. */
    @Transactional
    public MailResponse claimMail(Long userId, Long mailId) {
        Mail mail =
                mailRepository
                        .findByIdAndUserIdForUpdate(mailId, userId)
                        .orElseThrow(() -> ErrorCode.MAIL_NOT_FOUND.exception(mailId));
        if (mail.isClaimed()) {
            throw ErrorCode.MAIL_ALREADY_CLAIMED.exception(mailId);
        }

        Map<String, Integer> items = new LinkedHashMap<>();
        mail.getItems()
                .forEach(
                        mailItem ->
                                items.put(mailItem.getItem().getItemId(), mailItem.getQuantity()));
        userRewardService.grant(
                userId, mail.getExp(), mail.getGold(), mail.getGem(), mail.getEnergy(), items);

        mail.claim();
        return MailResponse.from(mail);
    }

    /** 등록된 지 30일 지난 우편을 매일 새벽에 지운다. 수령 여부와 상관없이 지운다. */
    @Scheduled(cron = "0 0 4 * * *", zone = "UTC")
    @Transactional
    public void deleteOldMails() {
        if (clientPolicy != null && clientPolicy.current().isMaintenance()) {
            return;
        }
        Instant cutoff = clock.instant().minus(RETENTION_DAYS, ChronoUnit.DAYS);
        // 첨부 아이템은 cascade·orphanRemoval로만 같이 지워지므로, 벌크 delete가 아니라 엔티티 단위로
        // 지운다(MailRepository#findAllByCreatedAtBefore 참고).
        mailRepository.deleteAll(mailRepository.findAllByCreatedAtBefore(cutoff));
    }

    private Map<String, Integer> mergeQuantities(List<MailItemRequest> items) {
        Map<String, Integer> merged = new LinkedHashMap<>();
        if (items == null) {
            return merged;
        }
        for (MailItemRequest item : items) {
            merged.merge(item.getItemId(), item.getQuantity(), Integer::sum);
        }
        return merged;
    }
}
