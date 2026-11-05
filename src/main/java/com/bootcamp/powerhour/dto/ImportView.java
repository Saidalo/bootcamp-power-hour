package com.bootcamp.powerhour.dto;

import com.bootcamp.powerhour.entity.PriceImportStatus;

public record ImportView(String when, String zone, Integer rows, PriceImportStatus status, String errorMessage) {
}