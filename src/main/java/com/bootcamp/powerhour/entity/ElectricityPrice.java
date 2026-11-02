package com.bootcamp.powerhour.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Entity
@Table(name = "electricity_prices")
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class ElectricityPrice {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private BiddingZone biddingZone;

    @Column(nullable = false)
    private OffsetDateTime startsAt;

    @Column(nullable = false)
    private BigDecimal priceEurKwh;
}
