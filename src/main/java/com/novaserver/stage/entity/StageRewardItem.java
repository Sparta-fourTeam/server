package com.novaserver.stage.entity;

/** 완료 보상 아이템. StageReward.items에 JSON으로 저장된다. */
public record StageRewardItem(String itemId, int finalAmount) {}
