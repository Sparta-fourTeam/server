package com.novaserver.mail.dto;

/** 대량·전체 발송 결과로 실제 전송된 수를 돌려준다. */
public record MailSendCountResponse(int sentCount) {}
