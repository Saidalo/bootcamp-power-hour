package com.bootcamp.powerhour.dto;

import com.bootcamp.powerhour.entity.BiddingZone;

import java.math.BigDecimal;
import java.time.LocalTime;

public record ApplianceResponse(Long id,
                                Long userId,
                                BiddingZone biddingZone,
                                String name,
                                BigDecimal powerKw,
                                Integer durationMinutes,
                                LocalTime allowedFrom,
                                LocalTime allowedTo) {
}
