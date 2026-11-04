package com.bootcamp.powerhour.provider;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record EleringResponse (boolean success, Map<String, List<PricePoint>> data) {
    public record PricePoint (long timestamp, BigDecimal price){
    }
}
