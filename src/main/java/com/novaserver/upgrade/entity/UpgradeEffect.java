package com.novaserver.upgrade.entity;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

/** 강화 레벨별 효과. */
@JsonInclude
public record UpgradeEffect(String kind, Integer valuePerLevel, List<Integer> levelValues) {}
