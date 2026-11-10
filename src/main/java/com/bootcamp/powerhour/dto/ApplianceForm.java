package com.bootcamp.powerhour.dto;

import com.bootcamp.powerhour.entity.BiddingZone;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalTime;

@Getter
@Setter
public class ApplianceForm {
    @NotNull(message = "Choose a bidding zone")
    private BiddingZone biddingZone;

    @NotBlank(message = "Enter a name")
    @Size(max = 100, message = "Name must be at most 100 characters")
    private String name;

    @NotNull(message = "Enter the power")
    @DecimalMin(value = "0.001", message = "Power must be more than 0")
    @Digits(integer = 5, fraction = 3, message = "Use at most 3 decimals")
    private BigDecimal powerKw;

    @NotNull(message = "Enter the duration")
    @Min(value = 1, message = "Duration must be at least 1 minute")
    @Max(value = 1440, message = "Duration can be at most 1440 minutes")
    private Integer durationMinutes;

    @NotNull(message = "Choose a start time")
    @DateTimeFormat(pattern = "HH:mm")
    private LocalTime allowedFrom;

    @NotNull(message = "Choose an end time")
    @DateTimeFormat(pattern = "HH:mm")
    private LocalTime allowedTo;
}