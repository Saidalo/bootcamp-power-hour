package com.bootcamp.powerhour.dto;

import com.bootcamp.powerhour.entity.PlannedRunStatus;

import java.math.BigDecimal;

public record RunView(long id, String applianceName, String when, BigDecimal cost, PlannedRunStatus status) {
}