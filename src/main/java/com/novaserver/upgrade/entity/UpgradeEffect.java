package com.novaserver.upgrade.entity;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

@JsonInclude
public record UpgradeEffect(String kind, Integer valuePerLevel, List<Integer> levelValues) {}
