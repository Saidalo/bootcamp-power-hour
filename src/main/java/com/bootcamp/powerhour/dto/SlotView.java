package com.bootcamp.powerhour.dto;

import java.math.BigDecimal;

// One bar in the price chart.
public record SlotView(String time, BigDecimal price, boolean allowed, boolean chosen,
                       int heightPercent, String hourLabel) {
}