package com.novaserver.mail.dto;

import com.novaserver.mail.entity.MailItem;

/** 우편에 첨부된 아이템. */
public record MailItemResponse(String itemId, String targetId, int quantity) {
    /** 첨부 아이템 항목을 응답으로 변환한다. */
    public static MailItemResponse from(MailItem mailItem) {
        return new MailItemResponse(
                mailItem.getItem().getItemId(),
                mailItem.getItem().getTargetId(),
                mailItem.getQuantity());
    }
}
