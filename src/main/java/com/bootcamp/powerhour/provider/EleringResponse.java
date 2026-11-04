package com.bootcamp.powerhour.provider;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record EleringResponse (boolean success, Map<String, List<EleringPrice>> data) {
    public record EleringPrice (long timestamp, BigDecimal price){
    }
}
